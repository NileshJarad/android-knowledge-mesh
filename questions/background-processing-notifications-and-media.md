# Background Processing, Notifications and Media

> Answers written from the perspective of a **Staff Android Engineer**. Each answer is structured as:
> *What the interviewer is really asking* → *Key concepts* → *Approach* → *Trade-offs* → *Code* → *Talking points*.

---

### 1. A file upload must continue reliably when the app moves to the background. How would you design it?

**What the interviewer is really asking**
Can you pick the right **background execution mechanism** for the platform version, and design the upload to be **resumable and process-death-safe** rather than a fire-and-forget coroutine?

**Key concepts**
- `WorkManager` (deferred, guaranteed, constraint-aware) vs **foreground service** (immediate, user-visible, long-running) vs a plain coroutine (dies with the process)
- Foreground service types (Android 14 requires a type + matching permission): `dataSync`, `mediaPlayback`, `mediaProcessing`, `camera`, `location`
- `FOREGROUND_SERVICE_DATA_SYNC` permission and the **`dataSync` 6-hour/24-hour cap** on Android 15 (API 35)
- `WorkManager` expedited work (`setExpedited`) — quota-limited to ~10 minutes of foreground work per day on API 31+
- Android 12+ background start restrictions: you cannot start a foreground service from the background
- `startForegroundService()` must call `startForeground()` within ~5 seconds or `ForegroundServiceDidNotStartInTimeException`
- Resumable/chunked upload (Section 1, Q9)
- Notification requirements for a foreground service (Android 13+ `POST_NOTIFICATIONS` permission)
- `WorkManager` `setForeground()` — a Worker can promote itself to a foreground service

**Approach**

1. **Choose the mechanism based on the guarantee you need.**
   - **Must complete, can be delayed** → `WorkManager`. Guaranteed to eventually run, survives process death and reboot, respects constraints, and is Doze-aware.
   - **Must run now, for a long time, and the user should see it** → **foreground service** with a `dataSync` type. Immediate and long-running, but constrained by the platform's background-start rules and (on API 35) by the daily `dataSync` time cap.
   - **Nice-to-have, tied to a screen** → a `viewModelScope`/`lifecycleScope` coroutine. Dies with the screen — acceptable only if the operation is not essential.

   For a **user-initiated upload that they are watching**, the best of both: a `WorkManager` job that calls `setForeground()` with a notification. This gives durability *and* the foreground-service privileges, and works with the expedited quota.

2. **Persist the upload state so it survives process death.** A `Room` row per upload: `(uploadId, fileUri, bytesUploaded, chunkIndex, idempotencyKey, state)`. The worker reads it on start and resumes. This is the part most implementations get wrong — the worker is a *stateless executor*; it must be able to reconstruct its state.

3. **Chunk the upload.** Send the file in chunks so that (a) each chunk is small enough to retry cheaply, (b) the token can be refreshed between chunks (Section 1, Q9), and (c) the progress can be checkpointed precisely. Use the server's resumable-upload protocol.

4. **Set the right constraints.**
   ```kotlin
   val request = OneTimeWorkRequestBuilder<UploadWorker>()
       .setConstraints(
           Constraints.Builder()
               .setRequiredNetworkType(NetworkType.CONNECTED)   // or UNMETERED for large files
               .setRequiresStorageNotLow(true)
               .build()
       )
       .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
       .addTag(TAG_UPLOAD)
       .build()
   ```
   Use `UNMETERED` for large media when the user has not consented to cellular data.

5. **Promote the worker to a foreground service with a progress notification.**
   ```kotlin
   override suspend fun doWork(): Result {
       setForeground(createForegroundInfo(bytesDone, totalBytes))
       // ... upload loop, calling setForeground(...) periodically to update the notification
   }
   ```
   `setForeground` must be called promptly (the OS expects it within ~10s of the worker starting, and the notification must be updated as progress changes). On Android 13+ the `POST_NOTIFICATIONS` permission is required for the notification to be visible; the FGS still runs without it, but the user sees nothing in the shade, which is a UX and policy problem.

6. **Handle the platform's background-start restrictions.** You generally **cannot** start a foreground service while the app is in the background on Android 12+. Therefore:
   - If the user is in the app when they tap "Upload" → starting the FGS is allowed.
   - If the app is backgrounded → the upload must be scheduled as `WorkManager` work (which the system starts when constraints permit), not as an FGS you start from the background.
   - This is a hard platform constraint and the design must not assume otherwise.

7. **Design for the Doze/app-standby buckets.** A `WorkManager` job in a rare-app bucket may be deferred significantly. If the user expects the upload to complete promptly, use **expedited** work (`setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)`) — the fallback policy ensures the job still runs even when the expedited quota is exhausted, just not immediately.

8. **Be honest about the platform limits.** On Android 15 (API 35), `dataSync` foreground services are capped at **6 hours in any 24-hour period** per app. A huge upload may exceed it. The mitigation is to make the upload resumable so it can continue in a later window, and to tell the user.

9. **Handle user cancellation and failures distinctly.** A user-initiated cancel should delete the checkpoint (or offer "pause" vs "cancel"). A network failure should retry with backoff. An unrecoverable error (file missing, 403) should fail permanently and notify the user.

10. **Notify on completion and on permanent failure**, with a deep link back to the upload.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| `WorkManager` only | Guaranteed, survives death/reboot, constraint-aware | May be delayed by Doze and bucket restrictions; not "now" |
| `WorkManager` + `setForeground` | Durable *and* visible with a progress notification | Consumes expedited quota; `POST_NOTIFICATIONS` needed for visibility |
| Foreground service directly | Immediate, full control, no quota accounting until the caps | Cannot start from the background on Android 12+; `dataSync` time cap on API 35; more lifecycle code |
| Plain coroutine | Simplest | Dies with the process — unacceptable for "must complete" |
| Chunked upload | Resumable, cheap retries, token-friendly | More server work; more round trips |
| Single monolithic PUT | Simple | Not resumable; a failure re-transfers everything |
| `UNMETERED` constraint | Respects the user's data | Delays the upload until Wi-Fi |

**Key detail:** The platform's background-start restriction (Android 12+) is the constraint that shapes the whole design: **you cannot escalate to a foreground service from the background**. The only robust pattern is "start the work as `WorkManager` work, and let the worker promote itself" — because `WorkManager`'s expedited path *is* the sanctioned way to run foreground work started from the background.

**Code example**

```kotlin
class UploadWorker(
    context: Context,
    params: WorkerParameters,
    private val dao: UploadDao,
    private val uploader: ResumableUploader,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val job = dao.nextPending() ?: return Result.success()
        setForeground(progressNotification(job.uploadedBytes, job.totalBytes))

        return try {
            uploader.upload(
                fileUri = job.fileUri,
                uploadId = job.uploadId,
                resumeFrom = job.uploadedBytes,
                onProgress = { sent ->
                    dao.updateProgress(job.uploadId, sent)
                    setForeground(progressNotification(sent, job.totalBytes))  // update the notification
                },
            )
            dao.markDone(job.uploadId)
            Result.success()
        } catch (e: CancellationException) {
            dao.markPaused(job.uploadId)   // keep the checkpoint for a later resume
            throw e
        } catch (e: UnknownHostException) {
            Result.retry()                  // transient → WorkManager applies backoff
        } catch (e: HttpException) {
            if (e.code() >= 500) Result.retry() else Result.failure()
        }
    }

    private fun progressNotification(done: Long, total: Long): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Uploading…")
            .setProgress(100, ((done * 100) / total).toInt(), false)
            .setSmallIcon(R.drawable.ic_upload)
            .setOngoing(true)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }
}
```

**Talking points / follow-ups**
- *"When would you choose a foreground service over WorkManager?"* → When the work must start immediately and continue for a long time, the user is actively waiting on it (e.g. a live recording, an active call), or the work needs continuous foreground CPU/network that Doze would otherwise restrict.
- *"What is the Android 15 dataSync limit?"* → Roughly 6 hours per 24-hour window per app for `dataSync` foreground services. Plan for it: make the job resumable and schedule the remainder.
- *"Does the notification permission block the upload?"* → No, but the user will not see the progress; and Google Play policy requires FGS notifications to be meaningful.
- *"How do you handle the user force-stopping the app?"* → `WorkManager` work is cancelled and not rescheduled until the user launches the app again. There is no way around this; design the UX to tell the user.
- *"How do you know the upload succeeded?"* → Verify server-side and reconcile; do not rely on the local notification.

---

### 2. A WorkManager job fails repeatedly because the network is unstable. How would you configure retries and constraints?

**What the interviewer is really asking**
Do you understand that `WorkManager` retries are **bounded, use exponential backoff, and require the work to be idempotent** — and that repeated failure usually means a **design problem**, not a retry-configuration problem?

**Key concepts**
- `Result.retry()` vs `Result.failure()` vs `Result.success()`
- `BackoffPolicy.EXPONENTIAL` (default) / `LINEAR`; `setBackoffCriteria` — minimum 10 seconds, maximum 5 hours
- `Constraints` — `NetworkType.CONNECTED`, `UNMETERED`, `NOT_ROAMING`, `requiresCharging`, `requiresDeviceIdle`, `requiresStorageNotLow`, `requiresBatteryNotLow`
- `setRequiredNetworkType` is a *precondition*, not a guarantee of connectivity during execution
- Idempotency: `WorkManager` guarantees **at-least-once** execution
- `runAttemptCount` — use it to stop retrying
- Network validation: `NetworkCapabilities.NET_CAPABILITY_VALIDATED`
- `WorkManager` "unconstrained" vs "constrained" workers
- `Result.retry()` resets the attempt counter only on success

**Approach**

1. **Distinguish the failure modes first.** A job that "fails repeatedly because the network is unstable" is one of:
   - **Connection lost mid-work** → transient; retry.
   - **The server is down / 5xx** → transient; retry with backoff.
   - **The device thinks it has a network but it does not actually work** (captive portal, a broken VPN, a network that passes the `CONNECTED` check but cannot reach the server) → the constraint is satisfied but the work still fails. This is the real cause in most field reports.

2. **Set the right constraint.** `NetworkType.CONNECTED` means "a network is available", not "the internet is reachable". On API 23+, `CONNECTED` implies validated connectivity for most cases, but a captive portal or a misconfigured enterprise Wi-Fi can still break it. Mitigations:
   - Use `NetworkType.CONNECTED` and handle the failure with a bounded retry.
   - Optionally check `NET_CAPABILITY_VALIDATED` yourself before starting the actual work and return `Result.retry()` if it is absent.

3. **Use exponential backoff with a sensible floor.**
   ```kotlin
   .setBackoffCriteria(
       BackoffPolicy.EXPONENTIAL,
       30, TimeUnit.SECONDS,       // WorkManager's minimum is 10s
   )
   ```
   Exponential is correct for network faults: it avoids hammering a flaky link and gives the network time to stabilise. Linear is better for a job with a predictable, short retry budget.

4. **Bound the retries.** `WorkManager` will retry indefinitely as long as you return `Result.retry()`. Use `runAttemptCount` to give up gracefully:
   ```kotlin
   override suspend fun doWork(): Result {
       if (runAttemptCount >= MAX_ATTEMPTS) {
           stateDao.markFailed(jobId, "network unavailable after $MAX_ATTEMPTS attempts")
           return Result.failure()      // stop; surface to the user
       }
       ...
   }
   ```
   Alternatively use `Result.failure(workDataOf(...))` after a bounded number of attempts and notify the user. Never let a job retry forever — it drains the battery and hammers the backend.

5. **Check `ConnectivityManager` inside the worker** before doing real work, to avoid a pointless attempt:
   ```kotlin
   val caps = cm.getNetworkCapabilities(cm.activeNetwork)
   val usable = caps != null &&
       caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
       caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
   if (!usable) return Result.retry()
   ```

6. **Make the work idempotent — this is non-negotiable.** `WorkManager` provides **at-least-once** execution: a worker may be run again after a process death, a retry, or a constraint re-evaluation. Therefore:
   - Do not assume `doWork` runs exactly once.
   - Use the same **idempotency key** / **server-side upsert** on every attempt (Section 1, Q1).
   - Track progress in the database, not only in memory.
   - Make partial progress resumable (a chunked upload, a cursor-based sync).

7. **Split the work so a failure is cheap.** A single worker that syncs 10,000 records and fails at 9,900 re-does everything. Chunk the work so each attempt makes durable progress, and record the cursor.

8. **Use `Result.retry()` only for transient failures.** A `401` (token expired) should trigger a token refresh and then a retry; a `400` (malformed request) is permanent → `Result.failure()`. Misclassifying permanent failures as retryable causes the "job fails 10 times" noise.

9. **Consider a different scheduling shape for very flaky networks.**
   - Use `NetworkType.UNMETERED` if the payload is large and Wi-Fi is a reasonable expectation.
   - Use **periodic** work for opportunistic sync rather than a retry loop for a large payload.
   - Consider a **foreground service** for a user-initiated transfer that must complete now.

10. **Observe it.** Track the success rate and the attempt-count distribution per job type. A job whose p95 attempt count is 3 is unhealthy and indicates the constraint or the endpoint is wrong, not that the backoff needs tuning.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| `EXPONENTIAL` backoff | Standard, gentle on the network and the server | The first retries are slow; a short blip may be over-waited |
| `LINEAR` backoff | Predictable, faster recovery for quick failures | Can synchronise retries across a fleet → thundering herd |
| `Result.retry()` forever | Never gives up | Battery drain, backend hammering, silent failure |
| Bounded attempts via `runAttemptCount` | Predictable, surfaces failure to the user | Gives up on a long outage that would have resolved |
| `CONNECTED` constraint | Simple | A "connected" network may not reach the server |
| `UNMETERED` constraint | Respects data usage | The upload waits for Wi-Fi — possibly a long time |
| Retry the whole job | Simple | Expensive for large payloads |
| Chunked, resumable work | Cheap retries, makes progress | More state to manage |

**Key detail:** `WorkManager` gives **at-least-once** execution, not exactly-once. That single fact drives the whole design: **every worker must be idempotent**, because it will eventually run twice. Most "the sync duplicated rows" bugs come from violating this.

**Code example**

```kotlin
class SyncWorker(
    context: Context,
    params: WorkerParameters,
    private val repo: SyncRepository,
    private val cm: ConnectivityManager,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // 1. Give up gracefully rather than retrying forever.
        if (runAttemptCount >= MAX_ATTEMPTS) {
            repo.markSyncFailed("gave up after $MAX_ATTEMPTS attempts")
            return Result.failure(workDataOf("reason" to "max_attempts"))
        }

        // 2. Verify the network is actually usable, not merely present.
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        val usable = caps != null &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        if (!usable) return Result.retry()

        return try {
            // 3. Idempotent + resumable: the same cursor is safe to re-apply.
            repo.syncFromLastCursor()
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Result.retry()                       // transient
        } catch (e: HttpException) {
            when (e.code()) {
                401 -> { repo.refreshSession(); Result.retry() }
                408, 429, in 500..599 -> Result.retry()
                else -> Result.failure()        // permanent — do not retry
            }
        }
    }

    companion object { const val MAX_ATTEMPTS = 5 }
}

// Enqueue with exponential backoff.
OneTimeWorkRequestBuilder<SyncWorker>()
    .setConstraints(
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresStorageNotLow(true)
            .build()
    )
    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
    .build()
```

**Talking points / follow-ups**
- *"What is the maximum backoff?"* → 5 hours; the minimum is 10 seconds. WorkManager computes the delay from the attempt count.
- *"Does `Result.retry()` reset the backoff?"* → The backoff grows with each attempt; a successful run resets the counter.
- *"Why did my job run 10 times?"* → Almost always a non-idempotent worker or a permanent error classified as transient. Check `runAttemptCount` and the exception type in the logs.
- *"How do you test this?"* → Use `TestListenableWorkerBuilder` / `TestWorkerBuilder` with a `TestDriver` to advance the WorkManager scheduler and assert the attempt count and the backoff schedule.
- *"Should I use `WorkManager` or a foreground service for a chat sync that must be instant?"* → If it must be instant and user-visible, a foreground service or a persistent FCM connection; `WorkManager` is for deferred work.

---

### 3. A background synchronization job is scheduled twice. How would you avoid duplicate work?

**What the interviewer is really asking**
Do you know the difference between **`enqueueUniqueWork`** (one logical job) and `ExistingWorkPolicy`, and can you explain why *even a unique work request* can still result in duplicated **effects** without idempotency?

**Key concepts**
- `WorkManager.enqueueUniqueWork(name, policy, request)` — uniqueness by name
- `ExistingWorkPolicy`: `KEEP` (keep the existing, ignore the new), `REPLACE` (cancel and replace), `APPEND` (chain after), `APPEND_OR_REPLACE`
- `enqueueUniquePeriodicWork` for periodic work
- `ExistingPeriodicWorkPolicy`: `KEEP`, `UPDATE`, `CANCEL_AND_REENQUEUE`
- Work **tags** (`addTag`) — for bulk cancellation/queries, not for uniqueness
- `WorkInfo` / `getWorkInfosForUniqueWork`
- At-least-once execution — uniqueness ≠ exactly-once effects
- Idempotency at the data layer
- `PeriodicWorkRequest` minimum period 15 minutes; flex

**Approach**

1. **Understand why "scheduled twice" happens.** Common causes:
   - Two different code paths both schedule the sync (e.g. `onResume` and a `WorkManager` periodic job).
   - The app process is killed and restarted, and the scheduling code runs again on every launch.
   - A `PeriodicWorkRequest` and a `OneTimeWorkRequest` both exist for the same purpose.
   - A user action triggers a sync while a scheduled sync is pending.
   The fix depends on the cause: unify the scheduling code, and use uniqueness where appropriate.

2. **Use `enqueueUniqueWork` with a stable name.**
   ```kotlin
   WorkManager.getInstance(context).enqueueUniqueWork(
       SYNC_WORK_NAME,                 // a stable, shared name
       ExistingWorkPolicy.KEEP,        // do not start a second one
       OneTimeWorkRequestBuilder<SyncWorker>().build(),
   )
   ```
   - `KEEP` — if a sync is already pending/running, the new request is ignored. Correct for "sync now" where an in-flight sync will pick up the new data anyway.
   - `REPLACE` — cancel the existing and start fresh. Correct when the new request carries different parameters (e.g. a different sync scope). Note it causes churn.
   - `APPEND` — chain: run the new work *after* the existing one finishes. Correct for ordered, incremental work.

3. **Choose the policy by intent.** "User tapped Sync" while a sync is running → `KEEP` (the running sync will pick up the changes). "A sync with a new filter/period was requested" → `REPLACE`. "Another delta to apply" → `APPEND`.

4. **Unify periodic and one-off work.** Use `enqueueUniquePeriodicWork` for the recurring sync and make the manual trigger a `OneTimeWorkRequest` under the same unique name — but note a periodic and a one-time request have different unique names in practice; use `enqueueUniqueWork` for the manual trigger and `enqueueUniquePeriodicWork` with `ExistingPeriodicWorkPolicy.KEEP` for the recurring one. Then make the worker itself tolerant of being invoked while another sync is running.

5. **Add an in-worker guard.** Even with unique work, guard the effect:
   ```kotlin
   class SyncWorker(...) : CoroutineWorker(...) {
       override suspend fun doWork(): Result {
           // A mutex/DB flag prevents concurrent sync runs across processes.
           if (!syncLock.tryAcquire()) return Result.retry()   // or success(), nothing to do
           try { repo.syncFromLastCursor(); return Result.success() }
           finally { syncLock.release() }
       }
   }
   ```
   A `Mutex` in a process-scoped singleton plus a database flag (for cross-process safety) prevents overlap.

6. **Make the sync effect idempotent — the real guarantee.** Uniqueness prevents *scheduling* duplicates; it does not prevent *effect* duplicates, because `WorkManager` may re-run a worker after a process death. The sync must apply a **cursor** and use an **idempotent upsert**, so re-running it produces the same database state. A unique work request is a scheduling optimisation; idempotency is the correctness property.

7. **Make the cursor advance atomically with the data.**
   ```kotlin
   db.withTransaction {
       delta.changes.forEach { dao.upsert(it) }      // idempotent upsert
       metaDao.setCursor(delta.nextCursor)
   }
   ```
   If the process dies mid-transaction, the transaction rolls back and the next run re-applies the same delta — safe because of the upsert.

8. **Observe.** Track "sync runs per hour" and "concurrent sync detected" as metrics. A duplicate-work bug is invisible without them.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| `enqueueUniqueWork` + `KEEP` | Simple; no duplicate scheduling | Ignores new parameters if the request differs |
| `enqueueUniqueWork` + `REPLACE` | The new parameters always win | Cancels in-flight work; churn if scheduled often |
| `APPEND` | Ordered execution | Builds a queue; can lag behind |
| Uniqueness by name only | Works across app restarts | Requires a globally stable name |
| Tags | Bulk cancel/query | **Not** a uniqueness mechanism — a common misconception |
| In-worker mutex | Prevents overlap within a process | Not across processes; needs a DB flag for that |
| Idempotent upsert + cursor | The actual correctness guarantee | Requires the data model to support it |

**Key detail:** **`addTag` is not uniqueness.** Tags are for bulk operations (`cancelAllWorkByTag`, `getWorkInfosByTag`). Uniqueness is only via `enqueueUniqueWork` / `enqueueUniquePeriodicWork` with a **stable name**. Many teams assume tagging gives deduplication and are surprised.

**Code example**

```kotlin
object SyncScheduler {
    const val UNIQUE_SYNC = "app.sync.full"

    fun requestSync(context: Context, scope: String? = null) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(workDataOf("scope" to (scope ?: "all")))
            .addTag(TAG_SYNC)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_SYNC,
            // If a sync is already running it will pick up the latest data anyway.
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun schedulePeriodic(context: Context) {
        val periodic = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_SYNC,
            ExistingPeriodicWorkPolicy.KEEP,   // never stack periodic work
            periodic,
        )
    }
}

// The worker must be idempotent regardless — WorkManager may re-run it.
class SyncWorker(...) : CoroutineWorker(...) {
    override suspend fun doWork(): Result {
        val scope = inputData.getString("scope") ?: "all"
        return try {
            db.withTransaction {                                 // atomic: data + cursor
                val delta = api.pull(since = db.metaDao().cursor(scope))
                delta.changes.forEach { dao.upsert(it) }          // idempotent
                db.metaDao().setCursor(scope, delta.nextCursor)
            }
            Result.success()
        } catch (e: IOException) {
            Result.retry()
        }
    }
}
```

**Talking points / follow-ups**
- *"What is the difference between `KEEP` and `REPLACE`?"* → `KEEP` ignores the new request if one exists; `REPLACE` cancels the running one and enqueues the new one. `REPLACE` is disruptive — a user tapping Sync repeatedly can starve the work.
- *"Does unique work guarantee the job runs once?"* → No — it guarantees at-most-one *enqueued/running* instance under a name. The worker may still be executed multiple times (at-least-once semantics).
- *"What if the app enqueues from two processes?"* → `WorkManager` uniqueness is process-wide by name, so it holds; but the *effect* still needs to be idempotent and, if truly concurrent across processes, guarded by a DB-level lock.
- *"How do you test it?"* → Enqueue twice; assert `getWorkInfosForUniqueWork` returns one entry. Use `TestDriver` to control execution.
- *"What about a periodic job and a manual job both running?"* → Give them the same unique name, and additionally guard with the DB cursor so overlapping runs are harmless.

---

### 4. A push notification arrives while the user is already viewing the relevant screen. How would you avoid duplicate navigation or redundant data loading?

**What the interviewer is really asking**
Can you design a **notification-driven update path** that is idempotent and state-aware — so that a push either updates the current screen in place, or navigates, but never does both, and never loads data the screen already has?

**Key concepts**
- FCM data vs notification messages; `onMessageReceived` vs `onNewToken`
- `FirebaseMessagingService` (a background service with a 20s budget) vs `FirebaseMessaging`
- `PendingIntent` with `FLAG_IMMUTABLE`, `FLAG_UPDATE_CURRENT`, and **unique request codes** (a `PendingIntent` with the same request code and intent is *reused*, not replaced — this causes the classic "tapping the second notification opens the first screen" bug)
- Deep links and `NavController.handleDeepLink`
- `SingleTop` launch mode / `FLAG_ACTIVITY_SINGLE_TOP` / `FLAG_ACTIVITY_CLEAR_TOP`
- Foreground vs background when the push arrives
- "Currently on screen" awareness — a shared `CurrentDestination`/`ActiveScreenTracker`
- Idempotent refresh (ETag, `If-None-Match`, cursor-based delta sync)
- `WorkManager` for deferring the fetch to avoid doing work for a backgrounded app

**Approach**

1. **Distinguish the two cases at the top.** When a push arrives:
   - **The app is in the foreground and the target screen is visible** → do **not** navigate; instead trigger an **in-place refresh** of the current screen, or show a subtle inline banner ("New activity available — Refresh").
   - **The app is in the foreground but on a different screen** → decide per product rules; often update the underlying data silently and let the user navigate.
   - **The app is in the background** → show a notification; do not do the work now; the `PendingIntent` handles the tap.
   - **The app is terminated** → the notification's `PendingIntent` cold-starts the app into the right destination.

2. **Handle the message in one place.** Use `FirebaseMessagingService` (or the newer `FirebaseMessaging` KTX) and route the payload to a `PushMessageHandler` that decides what to do:
   ```kotlin
   class AppMessagingService : FirebaseMessagingService() {
       override fun onMessageReceived(message: RemoteMessage) {
           val payload = runCatching { message.toPushPayload() }.getOrNull() ?: return
           PushRouter.route(payload)     // decides based on app state
       }
   }
   ```
   Note that when the app is **backgrounded**, `onMessageReceived` is not called for *notification* messages — the system shows the notification instead. For **data** messages it is called (with the 20-second budget) in both states. This is an important platform detail: to control behaviour in the background, send **data messages** and render the notification yourself, or accept the system's behaviour.

3. **Know the current screen.** Maintain a process-scoped tracker of the active destination:
   ```kotlin
   @Singleton
   class CurrentDestination @Inject constructor() {
       private val _route = MutableStateFlow<String?>(null)
       val route: StateFlow<String?> = _route.asStateFlow()
       fun set(route: String?) { _route.value = route }
   }
   ```
   Update it from a `NavHost` `currentBackStackEntryAsState` observer, or from a `LifecycleEventObserver` on the Activity. Then:
   ```kotlin
   fun route(payload: PushPayload) {
       val current = currentDestination.route
       when {
           current == payload.targetRoute -> refreshManager.requestRefresh(payload.targetRoute)  // in-place
           appIsForeground -> { /* update data, no navigation */ }
           else -> showNotification(payload)
       }
   }
   ```

4. **Make the in-place refresh idempotent and cheap.**
   - Use a **delta/cursor** fetch (`GET /sync?since=cursor`) rather than a full reload — a push usually just means "something changed".
   - Use `ETag`/`If-None-Match` so an unchanged resource costs a `304`.
   - If the screen's data comes from **Room** (offline-first), the push only needs to trigger the sync; the Room `Flow` updates the UI automatically. The screen does not need to "do" anything.
   - Debounce/dedupe refresh requests so five pushes for the same entity cause one refresh:
     ```kotlin
     private val refreshRequests = MutableSharedFlow<String>(extraBufferCapacity = 8)

     fun requestRefresh(scope: String) {
         refreshRequests.tryEmit(scope)     // conflating: only the latest matters
     }
     ```

5. **Get the `PendingIntent` right.** This is where the "duplicate navigation" bug usually lives:
   ```kotlin
   val intent = Intent(this, MainActivity::class.java).apply {
       action = "OPEN_${payload.targetRoute}"       // unique per destination
       putExtra(EXTRA_ROUTE, payload.targetRoute)
       putExtra(EXTRA_ENTITY_ID, payload.entityId)
       flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
   }
   val pendingIntent = PendingIntent.getActivity(
       this,
       requestCode,                              // unique per payload → do NOT always use 0
       intent,
       PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
   )
   ```
   - **A unique `requestCode` per notification** is essential. `PendingIntent.getActivity` with the same `requestCode` and an "equal" intent returns the *existing* `PendingIntent` (with `FLAG_UPDATE_CURRENT` only updating the extras) — so the second notification can carry the first notification's extras. Use a unique request code (e.g. derived from the notification id) or a distinct `action`.
   - `FLAG_IMMUTABLE` is required on Android 12+ for anything you do not need to mutate.
   - `FLAG_ACTIVITY_CLEAR_TOP | SINGLE_TOP` ensures the existing Activity is brought forward rather than a new one stacked on top.

6. **Handle the tap idempotently.** When the Activity is launched with the deep link, `NavController.handleDeepLink` navigates. If the destination is already on the back stack, `handleDeepLink` pops to it — usually correct. For the "already viewing" case, the `CurrentDestination` check lets you choose to refresh instead of navigating.

7. **Deduplicate at the data layer too.** Even if navigation happens twice, the data layer should be idempotent — the same entity applied twice yields the same state. Use `upsert` keyed on the server id.

8. **Do not do heavy work when backgrounded.** If the app is in the background, schedule a `WorkManager` sync with a network constraint rather than fetching immediately — the notification tap will find fresh data.

9. **Throttle.** If pushes arrive in a burst (e.g. 50 notifications), coalesce them: one refresh, and a notification group (`NotificationGroup`) so the user sees a summary.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| In-place refresh when the screen is visible | No jarring navigation; the user keeps context | The user might miss that new data arrived — add a subtle indicator |
| Always navigate | Simple; the user sees the new content | Disruptive; loses the user's context; causes the "navigated away" complaint |
| Delta fetch (`since=cursor`) | Cheap; scales | Requires cursor support on the server |
| Full refetch on push | Simple | Expensive; wasteful; can cause jank |
| Data messages + own notification rendering | Full control in both states | More code; must handle the 20s service budget; the notification looks non-standard unless you style it |
| Notification messages (system-rendered) | Standard; works when terminated | Less control; `onMessageReceived` not called in the background |
| Unique `requestCode` per notification | Correct routing | More `PendingIntent`s (bounded, but note the 1000-pending-intent limit in some launchers) |

**Key detail:** The `PendingIntent` "equal intent" behaviour is the root of most "notification opens the wrong screen" bugs — two `PendingIntent`s with the same `requestCode` and equal intents **share** the same `PendingIntent`; `FLAG_UPDATE_CURRENT` updates the extras of the shared instance, so the *older* notification can end up carrying the *newer* payload. Use a unique request code per notification, or `FLAG_CANCEL_CURRENT` (which cancels the previous one — also acceptable for "latest wins").

**Code example**

```kotlin
@Singleton
class CurrentDestination @Inject constructor() {
    private val _route = MutableStateFlow<String?>(null)
    val route: StateFlow<String?> = _route.asStateFlow()
    fun update(route: String?) { _route.value = route }
}

@Singleton
class PushRouter @Inject constructor(
    private val destinations: CurrentDestination,
    private val refreshes: RefreshCoordinator,
    private val appState: AppStateObserver,
) {
    fun route(payload: PushPayload) {
        val current = destinations.route
        when {
            // Already on the target screen → refresh in place, never navigate.
            current == payload.targetRoute -> refreshes.requestRefresh(payload.targetRoute)
            // App visible elsewhere → update data silently; no navigation.
            appState.isForeground -> refreshes.requestRefresh(payload.targetRoute)
            // Backgrounded → the notification handles it on tap.
            else -> Unit
        }
    }
}

class RefreshCoordinator @Inject constructor(private val scope: CoroutineScope) {
    // Conflating shared flow: a burst of pushes results in one refresh.
    private val requests = MutableSharedFlow<String>(
        replay = 0, extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    fun requestRefresh(scope: String) { requests.tryEmit(scope) }

    fun start(scope: CoroutineScope) {
        scope.launch {
            requests.collect { target -> performDeltaRefresh(target) }
        }
    }

    private suspend fun performDeltaRefresh(target: String) {
        // Delta fetch with a cursor + ETag → cheap and idempotent.
        val delta = api.delta(since = cursorStore.get(target))
        if (delta.notModified) return
        db.withTransaction {
            delta.changes.forEach { dao.upsert(it) }   // idempotent
            cursorStore.set(target, delta.nextCursor)
        }
    }
}

// Building the PendingIntent — unique request code per notification.
fun buildPendingIntent(context: Context, payload: PushPayload, notificationId: Int): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        action = "OPEN_${payload.targetRoute}"          // unique per destination
        putExtra(EXTRA_ROUTE, payload.targetRoute)
        putExtra(EXTRA_ENTITY_ID, payload.entityId)
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    return PendingIntent.getActivity(
        context,
        notificationId,                                // unique request code
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
```

**Talking points / follow-ups**
- *"What is the difference between a data message and a notification message?"* → A notification message is displayed by the system when the app is backgrounded and `onMessageReceived` is not called; a data message always calls `onMessageReceived` (with a ~20s budget when backgrounded) and you render the notification yourself.
- *"How do you know which screen the user is on?"* → A process-scoped `CurrentDestination` updated from the nav graph or from `LifecycleEventObserver`. It is not persisted across process death, which is fine.
- *"What if the push arrives while the app is in the background but the user is on the target screen?"* → The system shows the notification; on tap, `CurrentDestination` is empty (app backgrounded) so you navigate — but since the destination is already on the back stack, `handleDeepLink` pops to it rather than stacking a duplicate.
- *"How do you avoid a reload storm from a burst of pushes?"* → Conflate the refresh requests (`DROP_OLDEST` on a shared flow) and use a delta fetch.
- *"What about duplicate pushes from the server?"* → Deduplicate by the message id on the server, and make the client effect idempotent.

---

### 5. A video must resume from the exact position where the user stopped, even after process death. How would you implement it?

**What the interviewer is really asking**
Do you know the difference between **player position state** (must survive process death → `SavedStateHandle`/persistent storage) and **player instance state** (must survive configuration change → the player/ViewModel), and can you keep them consistent?

**Key concepts**
- `ExoPlayer` / `Media3` (`androidx.media3`) — the current player library
- `Player.Listener` (`onPlaybackStateChanged`, `onIsPlayingChanged`, `onPositionDiscontinuity`)
- Persisting position: `SavedStateHandle` (survives process death) vs `Room`/DataStore (durable, survives app data clear)
- `rememberSaveable` for a `Player` is not possible — the player is not `Saveable`
- ViewModel-held `ExoPlayer` survives configuration change but **not** process death
- Progress reporting cadence (e.g. every 5 seconds, and on pause/stop)
- `MediaSessionService` / `MediaLibraryService` for a media app
- `MediaItem` with `ClippingConfiguration` / `setStartPosition`
- `seekTo(positionMs)`
- Correctness: resuming at an exact frame vs. resuming at a chapter boundary
- Multi-window / picture-in-picture position

**Approach**

1. **Separate the two lifetimes.**
   - **Player instance** (the `ExoPlayer` object, its buffers, its listeners) → held in a **ViewModel** (or a `MediaSessionService`). It survives configuration changes and is released in `onCleared()`.
   - **Playback position** (a `Long` + the media id + maybe the subtitle track) → held in **`SavedStateHandle`** so it survives process death, and mirrored to a durable store if it must survive app data clear or be shared across devices.

2. **Persist the position continuously and on key events.** Use `Player.Listener`:
   ```kotlin
   player.addListener(object : Player.Listener {
       override fun onPlaybackStateChanged(state: Int) {
           if (state == Player.STATE_ENDED) {
               savedState["position_$mediaId"] = 0L        // reset for a finished item
           } else {
               savePosition()
           }
       }
       override fun onPositionDiscontinuity(
           old: Player.PositionInfo, new: Player.PositionInfo, reason: Int,
       ) {
           if (reason == Player.DISCONTINUITY_REASON_SEEK) {
               savePosition()                              // the user seeked — persist immediately
           }
       }
       override fun onIsPlayingChanged(isPlaying: Boolean) {
           if (!isPlaying) savePosition()                 // paused → persist now
       }
   })
   ```
   Plus a periodic save while playing (every ~5 seconds) so a process kill loses at most a few seconds.

3. **Restore on creation.**
   ```kotlin
   class PlayerViewModel(
       private val savedState: SavedStateHandle,
       private val repo: MediaRepository,
   ) : ViewModel() {
       private val player: ExoPlayer = ExoPlayer.Builder(app).build().apply {
           setMediaItem(repo.mediaItem(mediaId))
           val saved = savedState.get<Long>("position_$mediaId") ?: 0L
           if (saved > 0) seekTo(saved)
           prepare()
       }

       fun savePosition() {
           savedState["position_$mediaId"] = player.currentPosition
       }

       override fun onCleared() {
           savePosition()
           player.release()      // release the player; never leak it
       }
   }
   ```
   `SavedStateHandle` survives process death because the framework persists the ViewModel's saved state into the Activity's saved instance state `Bundle`. This is the correct mechanism for "resume even after process death" — **and the reason you must keep the value small** (a `Long` and a `String` id are fine).

4. **Persist durably when needed.** If the position must survive app data clear, or be visible on another device, write it to Room or DataStore as well:
   ```kotlin
   // Room: the resume position becomes part of the media item's server state
   @Entity(tableName = "playback_progress")
   data class PlaybackProgress(
       @PrimaryKey val mediaId: String,
       val positionMs: Long,
       val updatedAt: Long,
       val deviceId: String,
   )
   ```
   For a media app with cross-device resume, the position should be synced to the backend.

5. **Release the player correctly.** A ViewModel-held `ExoPlayer` must be released in `onCleared()` — otherwise it leaks a codec and an audio track. This is also where you should stop a `MediaSession` update.

6. **Handle "resume from exactly" correctly.** `seekTo(positionMs)` restores exactly, but the first frame after a seek may take a moment and the buffered range may not include the position (if it was evicted). Handle `onPlaybackStateChanged(STATE_BUFFERING)` and the `PlayerException` case; consider resuming at a keyframe-aligned position for smooth playback (HLS segments are ~2–6s, so a few seconds of offset is normal and users accept it).

7. **For a media app, use `MediaSessionService`.** A `MediaService` in the foreground keeps playback alive in the background, integrates with the notification and lock screen, and supports **Android 13+ system media controls** and Android 14's live activity. The `MediaSession` holds the playback state so external controllers (Bluetooth, Assistant, the system UI) see the correct position.

8. **Handle multi-window / PiP.** If the user enters PiP, the activity is not destroyed but the ViewModel is retained; position handling is the same.

9. **Save on every "leaving" event**: pause, activity `onStop`, the app going to the background, and the process being killed. Combine `LifecycleEventObserver` with the player listener.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| `SavedStateHandle` | Survives process death; simple; no I/O | Tied to the ViewModel's saved state; small; lost if the Activity's state was never saved |
| Room / DataStore | Durable; survives app data clear; syncable | Extra I/O; more code; needs a write policy |
| Player in the ViewModel | Survives rotation; easy to control | Not a foreground service — playback stops when the app is killed; must be released |
| Player in a `MediaSessionService` | Background playback, system controls, PiP | More complex; a foreground service; notification requirements |
| Save every 5s + on events | Low overhead | Can lose up to 5s on a hard kill |
| Save on every position change | Exact | High I/O; wear on storage |
| Exact position vs. keyframe-aligned | Exact is what the user expects | A few seconds of drift in HLS; re-buffering on resume |

**Key detail:** The precise requirement "**even after process death**" points at `SavedStateHandle`, not `remember` (lost on rotation) and not a plain ViewModel field (lost on process death). `SavedStateHandle` is the one that is saved into the Activity's `Bundle` — and it also carries the size constraint (keep it small).

**Code example**

```kotlin
class VideoPlayerViewModel(
    application: Application,
    private val savedState: SavedStateHandle,
    private val repo: MediaRepository,
) : AndroidViewModel(application) {

    private val mediaId: String = checkNotNull(savedState["mediaId"])

    val player: ExoPlayer = ExoPlayer.Builder(application).build().apply {
        setMediaItem(repo.mediaItem(mediaId))
        // Restore the exact position (survives process death via SavedStateHandle).
        savedState.get<Long>(KEY_POSITION)?.takeIf { it > 0 }?.let { seekTo(it) }
        prepare()

        addListener(object : Player.Listener {
            override fun onPositionDiscontinuity(
                old: Player.PositionInfo, new: Player.PositionInfo, reason: Int,
            ) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK) persist()
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    savedState[KEY_POSITION] = 0L        // finished → start over next time
                } else {
                    persist()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) persist()
            }
        })
    }

    private val positionTicker = viewModelScope.launch {
        while (isActive) {
            delay(5_000)
            if (player.isPlaying) persist()
        }
    }

    private fun persist() {
        savedState[KEY_POSITION] = player.currentPosition
    }

    override fun onCleared() {
        persist()
        positionTicker.cancel()
        player.release()      // never leak the player/codec
        super.onCleared()
    }

    companion object { private const val KEY_POSITION = "position" }
}
```

**Talking points / follow-ups**
- *"Does `SavedStateHandle` really survive process death?"* → Yes, when the framework saved the instance state before the process was killed (i.e. the Activity went to the background normally). It does not survive a crash or a force-stop.
- *"Why not just keep the position in the ViewModel field?"* → A ViewModel field dies with the process.
- *"What about a `MediaSessionService`?"* → The right architecture for a media app: playback continues in the background, the system UI and Bluetooth controls work, and `MediaSession` exposes the position. The position still needs persisting for the process-death case.
- *"How precise must the resume be?"* → Exact within the seek granularity of the format. For HLS/DASH, resuming mid-segment causes a small re-buffer; resuming at the segment boundary is smoother but less exact. Let the user choose if it matters.
- *"How do you test it?"* → Play, pause at 30s, kill the process (`adb shell am force-stop`), relaunch, assert the player resumes at ~30s.

---

### 6. Audio playback is interrupted by a phone call or another audio application. How would you handle audio focus and lifecycle changes?

**What the interviewer is really asking**
Do you understand the **audio focus protocol** (request → gain/loss → duck/pause → abandon), and can you implement it correctly with `AudioManager`/`AudioFocusRequest`, including the **Android 12+ "transient" vs "loss"** distinction and the **becoming-noisy** broadcast?

**Key concepts**
- `AudioManager.requestAudioFocus(AudioFocusRequest)` (API 26+) vs the deprecated `requestAudioFocus(listener, streamType, durationHint)`
- `AudioFocusRequest` with `AudioAttributes` (`CONTENT_TYPE_MUSIC`, `CONTENT_TYPE_SONIFICATION`, `USAGE_MEDIA`, `USAGE_ASSISTANCE_ACCESSIBILITY`)
- `AUDIOFOCUS_GAIN`, `AUDIOFOCUS_GAIN_TRANSIENT`, `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`, `AUDIOFOCUS_LOSS`, `AUDIOFOCUS_LOSS_TRANSIENT`, `AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK`
- **Becoming noisy**: `ACTION_AUDIO_BECOMING_NOISY` — headphones unplugged → pause
- `AudioManager.registerAudioPlaybackCallback` (API 26+) — the modern way to observe other apps taking focus
- `MediaSessionService` + the system handling focus for you
- `onAudioFocusChange` handling on the correct thread (the callback is on the main thread)
- `pause()` vs `stop()` vs lowering the volume (duck)

**Approach**

1. **Request audio focus before playing, and abandon it when done.** The request must carry correct `AudioAttributes` — the attributes determine whether your app is eligible to duck or be ducked, and what the system does with the stream.

2. **Use the modern API (API 26+):**
   ```kotlin
   private val audioManager = context.getSystemService<AudioManager>()!!

   private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
       .setAudioAttributes(
           AudioAttributes.Builder()
               .setUsage(AudioAttributes.USAGE_MEDIA)
               .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
               .build()
       )
       .setOnAudioFocusChangeListener(focusChangeListener, mainHandler)
       .build()

   private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
       when (change) {
           AudioManager.AUDIOFOCUS_LOSS -> pausePlayback()          // permanent: another app took over
           AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pausePlayback() // temporary (a call): pause, remember position
           AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> duckVolume(true)
           AudioManager.AUDIOFOCUS_GAIN -> { duckVolume(false); resumeIfShould() }
       }
   }

   fun play() {
       val result = audioManager.requestAudioFocus(focusRequest)
       if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
           player.play()
       } else {
           // Denied → do not play; maybe show a message
       }
   }

   fun stop() {
       audioManager.abandonAudioFocusRequest(focusRequest)
   }
   ```

3. **Handle each focus change correctly — this is where most implementations are wrong.**
   - **`AUDIOFOCUS_LOSS`** — you will not get focus back. **Stop** playback, release resources, and update the notification. Do not auto-resume.
   - **`AUDIOFOCUS_LOSS_TRANSIENT`** — you *may* get it back (a phone call). **Pause** and remember the position; resume on `AUDIOFOCUS_GAIN`. Do not release the player.
   - **`AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK`** — lower the volume (typically to ~0.2–0.3) and **keep playing**. Notifications and navigation prompts use this.
   - **`AUDIOFOCUS_GAIN`** — restore the volume and resume **only if you were paused by a transient loss** (not if the user paused it). Track a `pausedByFocusLoss` flag so you do not resume when the user did not want it.

4. **Pause on "becoming noisy".** Register for `ACTION_AUDIO_BECOMING_NOISY` — headphones unplugged → pause immediately (otherwise the phone plays audio out of the speaker, which users consider a bug). With `MediaSessionService`, the platform handles this for you via the session's callbacks.

5. **Use `MediaSessionService` for a media app.** It handles focus, the notification, the lock-screen controls, Bluetooth/Assistant integration, and Android 13+ system media controls, and it keeps playback alive in the background. Doing all of this by hand is a lot of subtle work.

6. **Observe other apps taking focus.** On API 26+, `registerAudioPlaybackCallback` tells you when another app starts playing — useful for pausing your own recording or preview, and for a "playing elsewhere" indicator.

7. **Handle the lifecycle.** A plain `Activity`-scoped player stops when the Activity is destroyed. For background audio, the player lives in a **`MediaSessionService`** (a foreground service). If playback must stop when the screen is off, keep it in the Activity/ViewModel and pause in `onStop`.

8. **Use the correct `AudioAttributes` for the content type.** Navigation prompts should use `USAGE_ASSISTANCE_NAVIGATION_GUIDANCE`; accessibility announcements use `USAGE_ASSISTANCE_ACCESSIBILITY`; alarms use `USAGE_ALARM`. The wrong usage can cause your app to be ducked by navigation apps, or to fail to play over other apps.

9. **Register the noisy broadcast correctly.** Use `ContextCompat.registerReceiver` with `RECEIVER_NOT_EXPORTED` on API 34+ (see Section 7, Q7), and unregister in `onDestroy`.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| `MediaSessionService` | Handles focus, notifications, system controls, background playback | More moving parts; a foreground service |
| Manual audio focus | Full control, no service needed | You must implement focus, noisy, ducking, and notifications yourself |
| `AUDIOFOCUS_GAIN_TRANSIENT` (pause) | Simple; predictable | A navigation prompt stops your music entirely |
| `GAIN_TRANSIENT_MAY_DUCK` + ducking | Music keeps playing quietly | Requires volume handling; some content is unintelligible when ducked |
| Ignoring focus | Simplest | Rude; the system may force your app to stop anyway; poor user experience |
| Manual `registerAudioPlaybackCallback` | Knows about other players | Extra work; usually unnecessary with `MediaSessionService` |

**Key detail:** The most commonly missed behaviours are: (a) **not resuming after a transient loss if the user had already paused** — hence the `pausedByFocusLoss` flag; and (b) **not handling `AUDIOFOCUS_LOSS` as permanent**, which leads to a player that is resumed into a silent state.

**Code example**

```kotlin
class AudioController(private val context: Context) {
    private val audioManager = context.getSystemService<AudioManager>()!!
    private val player: ExoPlayer = ExoPlayer.Builder(context).build()
    private var pausedByFocusLoss = false

    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        .setOnAudioFocusChangeListener(::onFocusChange, Handler(Looper.getMainLooper()))
        .build()

    private fun onFocusChange(change: Int) {
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Permanent — another app owns the audio now. Do not auto-resume.
                pausedByFocusLoss = false
                player.pause()
                releasePlayerIfIdle()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // A phone call — pause and remember that *we* were interrupted.
                pausedByFocusLoss = player.isPlaying
                player.pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                player.volume = 0.25f        // keep playing, quietly
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                player.volume = 1f
                if (pausedByFocusLoss) {     // resume only if the focus loss paused us
                    pausedByFocusLoss = false
                    player.play()
                }
            }
        }
    }

    fun play() {
        val granted = audioManager.requestAudioFocus(focusRequest) ==
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        if (granted) player.play()
    }

    fun pause() {
        pausedByFocusLoss = false        // a user pause is not a focus-loss pause
        player.pause()
    }

    fun release() {
        audioManager.abandonAudioFocusRequest(focusRequest)
        player.release()
    }

    // Headphones unplugged → pause.
    fun registerNoisyReceiver(receiver: BroadcastReceiver) {
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }
}
```

**Talking points / follow-ups**
- *"What is the difference between transient loss and permanent loss?"* → Transient: you should pause and expect to resume (a call, an alarm). Permanent: another app has taken the audio; stop and do not resume.
- *"What is ducking?"* → Lowering your volume so another app (navigation, a notification) can be heard. `AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK` requests it.
- *"How do you handle the user pausing while a call is happening?"* → The `pausedByFocusLoss` flag prevents an unwanted resume on `AUDIOFOCUS_GAIN`.
- *"Do I need to handle the noisy broadcast myself?"* → With `MediaSessionService`, no. Otherwise yes — and it is one of the most requested UX behaviours.
- *"What about audio recording (a call recorder)?"* → Recording uses `MediaRecorder`/`AudioRecord` and has separate rules; focus handling is different (you should not duck a recording; you may need `MODIFY_AUDIO_SETTINGS` and, on API 29+, the foreground service type `microphone`).

---

### 7. A large file download must support resuming after a network disconnect or device restart. How would you design it?

**What the interviewer is really asking**
Can you implement **HTTP range-based resumption** with **integrity verification**, on top of the right platform mechanism (`DownloadManager` vs a `WorkManager` job)?

**Key concepts**
- HTTP `Range: bytes=N-` and the `206 Partial Content` response; `Accept-Ranges`
- `ETag` / `If-Range` — resume only if the file has not changed
- `Content-Range`, `Content-Length`
- `DownloadManager` — the platform service; survives the app process; resumes; shows a notification; but limited control
- `WorkManager` — for a resumable download you implement yourself; survives process death; you control chunking and integrity
- Partial files (`.part`) and atomic rename on completion
- SHA-256 checksum verification
- `RandomAccessFile` / `FileOutputStream(file, append = true)`
- Foreground service for a user-visible long download
- Disk space checks
- `OkHttp` streaming with `ResponseBody.byteStream()`

**Approach**

1. **Choose the mechanism.**
   - **`DownloadManager`** — the platform's own download service. It survives the app being killed, shows a system notification, handles network changes, and supports **HTTP resume** natively for servers that support `Range`. Constraints: limited control over headers (you can add some), no custom checksum verification hooks, no per-chunk callbacks, and it can be flaky behind some proxies/auth flows. Good for public, simple downloads.
   - **`WorkManager` + your own HTTP client** — full control: `Range` requests, checksum verification, progress, destination control (including `MediaStore` for public files), pause/resume UI. The worker is re-invoked after a process death and resumes from the checkpoint.
   - **Foreground service** — for a long download that must run now and be visible.

   For a large file with integrity requirements, **`WorkManager` + your own client** is the answer.

2. **Implement range-based resume.**
   ```kotlin
   val existing = File(dir, "$name.part")
   val offset = if (existing.exists()) existing.length() else 0L

   val request = Request.Builder()
       .url(url)
       .header("Range", "bytes=$offset-")
       .apply { etag?.let { header("If-Range", it) } }
       .build()

   val response = client.newCall(request).execute()
   when (response.code) {
       206 -> { /* append to the partial file */ }
       200 -> { /* server ignored Range — restart from zero, truncate the file */ }
       else -> { /* handle error */ }
   }
   ```
   - **If the server responds `200` instead of `206`**, it ignored the range request — you must **truncate and restart**, otherwise you will append to a partial file and produce a corrupt result. This is a bug many implementations have.
   - **`If-Range` with the stored `ETag`** ensures you do not append bytes from a *different* version of the file. If the server's ETag changed, it returns `200` with the full content — again, restart.
   - If the server does not support ranges (`Accept-Ranges: none`), you cannot resume — you must restart.

3. **Verify integrity.** Compute a SHA-256 (or MD5, or whatever the server provides) over the complete file and compare with the server-provided checksum. Because a resumed file is assembled from two parts, **per-chunk checksums plus a whole-file checksum** is the robust combination: verify each chunk as it arrives (so a corrupt chunk is retried), and verify the whole file at the end (so a bad resume is caught).

4. **Persist the download state** so a device restart resumes: `(url, destPath, bytesDownloaded, etag, totalBytes, checksum, state)` in Room. The worker reads it on start.

5. **Write to a `.part` file and rename atomically on completion.** Never expose a partially downloaded file under its final name — a user or another app must not see a corrupt file.

6. **Handle disk space.** Check `StatFs` before starting and periodically during the download; fail gracefully with a clear message rather than a crash.

7. **Use a foreground worker for a user-visible download:**
   ```kotlin
   override suspend fun doWork(): Result {
       setForeground(downloadNotification(downloaded, total))
       // ...
   }
   ```

8. **Respect the user's data preference.** If the file is large, use `NetworkType.UNMETERED` unless the user explicitly allowed cellular.

9. **Handle the auth header across the resume.** If the token expired between the original request and the resume, refresh it (Section 1, Q9). If the download is from a signed URL, the URL may have expired — you must re-request a fresh URL. Store the "URL request" separately from the "URL use" so a fresh URL can be obtained.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| `DownloadManager` | Platform-managed; survives process death; system notification; native resume | Limited headers; no custom checksum hook; no progress callbacks; can fail behind some proxies |
| `WorkManager` + own client | Full control: ranges, checksums, destination, progress | You implement resume, retry, and progress yourself |
| Foreground service | Immediate, visible, long-running | Background-start restrictions; API 35 `dataSync` caps |
| Plain coroutine | Simplest | Dies with the process — no resume after a kill |
| Range requests | True resume; efficient | Needs server support; must handle the `200`-instead-of-`206` case |
| Whole-file checksum only | Simple | A corrupt chunk is only caught at the end — the whole file is re-downloaded |
| Per-chunk + whole-file checksum | Detects corruption early; only the bad chunk is retried | More metadata; the server must provide chunk checksums |
| `.part` file + atomic rename | Never exposes a corrupt final file | Requires cleanup of abandoned `.part` files |

**Key detail:** The two responses you must handle explicitly are **`206 Partial Content`** (append) and **`200 OK`** (the server ignored the range → **truncate and restart**). Getting the second case wrong silently produces a corrupt file that passes a length check but fails a checksum.

**Code example**

```kotlin
class DownloadWorker(
    context: Context,
    params: WorkerParameters,
    private val dao: DownloadDao,
    private val client: OkHttpClient,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getString(KEY_ID) ?: return Result.failure()
        val record = dao.get(id) ?: return Result.failure()

        val part = File(record.dir, "${record.name}.part")
        val target = File(record.dir, record.name)
        val etag = dao.getEtag(id)

        // Resume from the checkpoint.
        val offset = if (part.exists()) part.length() else 0L
        val request = Request.Builder()
            .url(record.url)
            .header("Range", "bytes=$offset-")
            .apply { etag?.let { header("If-Range", it) } }
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                when (response.code) {
                    206 -> {
                        val total = response.header("Content-Range")?.substringAfter('/')?.toLongOrNull()
                        part.outputStream().let { out ->
                            response.body!!.byteStream().use { input ->
                                var copied = 0L
                                val buffer = ByteArray(64 * 1024)
                                while (true) {
                                    val read = input.read(buffer)
                                    if (read < 0) break
                                    out.write(buffer, 0, read)
                                    copied += read
                                    dao.updateProgress(id, offset + copied, total)
                                    setForeground(notification(offset + copied, total))
                                }
                            }
                        }
                    }
                    200 -> {
                        // The server ignored the Range → restart from scratch.
                        part.delete()
                        response.body!!.byteStream().use { input ->
                            part.outputStream().use { out -> input.copyTo(out) }
                        }
                    }
                    else -> return Result.retry()
                }
            }

            // Integrity check before publishing the file.
            val digest = sha256(part.inputStream())
            if (record.sha256 != null && digest != record.sha256) {
                part.delete()
                return Result.retry()      // corrupt → retry from scratch
            }
            check(part.renameTo(target))     // atomic publish
            dao.markDone(id)
            Result.success()
        } catch (e: CancellationException) {
            throw e                         // keep the .part file for a later resume
        } catch (e: IOException) {
            Result.retry()
        }
    }
}
```

**Talking points / follow-ups**
- *"How does `DownloadManager` resume?"* → It issues `Range` requests itself for servers advertising `Accept-Ranges`. It is a black box, which is both its strength and its limitation.
- *"What if the server does not support ranges?"* → No resume is possible; you must restart. Detect via `Accept-Ranges` / the absence of `206`.
- *"How do you prevent a corrupt file from being installed?"* → Checksum verification plus writing to a `.part` file and renaming only after verification.
- *"How do you handle an expiring signed URL?"* → Separate the URL *request* from the URL *use*: store the object id, re-request a fresh signed URL on each attempt.
- *"How do you know the total size?"* → `Content-Length` for a `200`, or the `Content-Range` header's total for a `206`. Show progress only when the total is known.
- *"Where do you store the file?"* → App-private storage for private content; `MediaStore` for user-visible media (with `IS_PENDING` until complete).

---

### 8. A background task must run at a particular time, but Android may delay execution due to Doze or battery restrictions. How would you approach scheduling?

**What the interviewer is really asking**
Do you understand the **exactness vs. fairness trade-off** in Android scheduling, and can you design for "must happen eventually" versus "must happen at 9:00 AM"?

**Key concepts**
- **Doze**: the device idle, screen off, unplugged → `AlarmManager` is deferred, network access is suspended, `JobScheduler` jobs are batched
- **App Standby buckets** (`active`, `working_set`, `frequent`, `rare`, `restricted`) — Google Play auto-bucketing (Android 9+) and the manual bucket request API
- `WorkManager` — deferred, constraint-aware, **not exact**
- `AlarmManager.setExactAndAllowWhileIdle` / `setAlarmClock` — exact but **restricted**
- Android 12+ (`API 31`) background start restrictions: exact alarms require the `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` permission; **`USE_EXACT_ALARM` is auto-granted but only for alarm-clock-type apps** and is subject to Play policy
- `setExactAndAllowWhileIdle` — inexact per the quotas (9 minutes / 1 hour per app in Doze)
- `ForegroundService` start restrictions (Android 12+)
- `WorkManager` expedited quota (~10 min/day of foreground work, API 31+)
- **WorkManager still honours `setInitialDelay` and constraints but does not guarantee wall-clock execution**
- `Calendar`/`AlarmManager` with a `BroadcastReceiver`
- The **"exact alarms" policy**: Play requires justification for `SCHEDULE_EXACT_ALARM`; from Android 14 the permission is not pre-granted for most apps
- `JobScheduler` (`JobService`) with `setExact`/`setMinimumLatency`/`setRequiresDeviceIdle`

**Approach**

1. **Classify the requirement — this is the core of the answer.**
   - **"The task must happen at a specific wall-clock time"** (a medication reminder, an alarm, a daily briefing at 08:00) → **the user-facing promise is exact**, but the platform cannot guarantee it. You must use `AlarmManager` with `setExactAndAllowWhileIdle` (or `setAlarmClock`, which is exempt from Doze and shows a user-visible alarm icon) and **never rely on `WorkManager` for exactness**.
   - **"The task should happen soon, when conditions allow"** (sync, upload, prefetch) → **`WorkManager`**. It is designed exactly for this and is Doze-aware.
   - **"The task must happen now, in the foreground"** → a foreground service (subject to background-start restrictions).

2. **Use `AlarmManager` for the exact case.**
   ```kotlin
   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
       // Requires SCHEDULE_EXACT_ALARM (user-grantable) or USE_EXACT_ALARM (policy-restricted).
       if (!alarmManager.canScheduleExactAlarms()) {
           // Fall back to an inexact alarm + prompt the user to grant the permission.
           alarmManager.setWindow(RTC_WAKEUP, triggerAt, 15 * 60_000L, pendingIntent)
           requestExactAlarmPermission()
           return
       }
   }
   alarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, triggerAt, pendingIntent)
   ```
   - `setAlarmClock` is the strongest: it is delivered even in Doze and shows a "next alarm" indicator in the status bar — appropriate for a user-visible alarm and it is exempt from the exact-alarm permission restrictions.
   - On Android 13+, a `USE_EXACT_ALARM` app can request the exemption directly; otherwise `SCHEDULE_EXACT_ALARM` requires the user to grant it in settings.

3. **Pair the alarm with `WorkManager` for the real work.** The alarm's `BroadcastReceiver` should only *schedule* the work, not do it:
   ```kotlin
   class AlarmReceiver : BroadcastReceiver() {
       override fun onReceive(context: Context, intent: Intent) {
           // Schedule durable work rather than doing it in the receiver (10s limit).
           WorkManager.getInstance(context).enqueueUniqueWork(
               "reminder_${intent.getStringExtra(EXTRA_ID)}",
               ExistingWorkPolicy.REPLACE,
               OneTimeWorkRequestBuilder<ReminderWorker>()
                   .setInitialDelay(delayUntil(intent.getLongExtra(EXTRA_AT, 0)))
                   .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                   .build(),
           )
       }
   }
   ```
   This gives you the exactness of the alarm and the durability of `WorkManager` (the work survives process death).

4. **Use `WorkManager` for "eventually" work.** It is Doze-aware, survives reboots (with `setInitialDelay` recalculated), respects constraints, and applies backoff. But be honest: **`WorkManager` does not guarantee wall-clock execution.** A job scheduled for 08:00 may run at 09:30 on a device in the `rare` standby bucket. Communicate this honestly in the UI — "We'll remind you around 08:00" rather than "at 08:00".

5. **Understand and mitigate Doze.**
   - Doze applies when the device is idle, unplugged, and the screen is off for a while.
   - In Doze: network access is suspended (unless the app holds a `PARTIAL_WAKE_LOCK` or is in a whitelist), `AlarmManager` alarms are deferred (to a ~9-minute window in Doze, ~1 hour in deep Doze), and `JobScheduler` batches jobs.
   - `setExactAndAllowWhileIdle` and `setAlarmClock` are the escape hatches — they fire during Doze.
   - `WorkManager` uses `JobScheduler` under the hood and its jobs are **maintained** (not dropped) during Doze.

6. **Consider App Standby buckets and request a better bucket.** Android 9+ auto-buckets apps by usage; an app in the `rare` bucket is severely restricted. `WorkManager` provides `requestWorkSpecDelay` hints and the platform provides:
   ```kotlin
   // Ask the platform to move the app to a less restricted bucket (API 30+).
   val pm = context.getSystemService<PowerManager>()!!
   pm.setTemporaryPowerWhitelistDuration(packageName, 60 * 60 * 1000L)
   ```
   Also, engaging the user with the app (a notification, a widget, a foreground service) raises the bucket.

7. **Do not fight the platform.** The correct staff-level stance: **design the product around the guarantee you can actually provide.** If the product promise is "a reminder at 08:00", use an exact alarm *plus* a `setAlarmClock` fallback *plus* an honest UI, and validate on real devices — emulator Doze testing (`adb shell dumpsys deviceidle force-idle`) is essential.

8. **Test on real devices.** Emulators are not representative for scheduling behaviour; test on a low-end device with battery optimisation aggressive (Xiaomi/Samsung "background activity" restrictions add another layer — many OEM ROMs kill background work even when the platform allows it). Handle OEM-specific settings (e.g. ask the user to allow auto-start on Xiaomi).

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| `WorkManager` | Doze-aware, durable, constraint-aware, battery-friendly | **No wall-clock guarantee**; deferred in rare buckets |
| `AlarmManager.setExactAndAllowWhileIdle` | Fires during Doze | Needs the exact-alarm permission on Android 12+ (policy-restricted); not exempt from battery optimisation |
| `AlarmManager.setAlarmClock` | Strongest guarantee; exempt from Doze and from most exact-alarm restrictions | Shows a persistent alarm icon in the status bar — user-visible |
| `setWindow` / inexact alarms | No permission needed | Can be delayed by hours |
| `JobScheduler` directly | System-managed | Not exact; more work than `WorkManager` |
| Foreground service | Runs now | Background-start restrictions; not for scheduled work |
| Rely on the server to trigger the action (push) | The server decides the moment | Requires the user to allow notifications; FCM is not guaranteed to be instant |

**Key detail:** **`WorkManager` is not a scheduler.** It is a deferred, constraint-aware executor. If the requirement is truly time-based, `AlarmManager` is the tool; if it is condition-based, `WorkManager` is the tool. Most real designs use `AlarmManager` to *trigger* `WorkManager` to do the work.

**Code example**

```kotlin
object ReminderScheduler {
    private const val CHANNEL = "reminders"

    fun schedule(context: Context, id: String, triggerAtMillis: Long) {
        val am = context.getSystemService<AlarmManager>()!!
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.example.REMINDER_$id"      // unique action per reminder
            putExtra("id", id)
            putExtra("at", triggerAtMillis)
        }
        val pi = PendingIntent.getBroadcast(
            context, id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            am.canScheduleExactAlarms()

        when {
            canExact -> am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            else -> {
                // No exact-alarm permission: fall back to an inexact window and ask the user.
                am.setWindow(AlarmManager.RTC_WAKEUP, triggerAtMillis, 15 * 60_000L, pi)
                ExactAlarmPermission.request(context)
            }
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("id") ?: return
        val at = intent.getLongExtra("at", 0L)
        // The receiver only *schedules*; WorkManager does the work durably.
        WorkManager.getInstance(context).enqueueUniqueWork(
            "reminder_$id",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay((at - System.currentTimeMillis()).coerceAtLeast(0))
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build(),
        )
    }
}
```

**Talking points / follow-ups**
- *"How do you guarantee an 8 AM alarm?"* → `setAlarmClock` (with a `PendingIntent` for the next alarm) is the strongest; show an in-app "exact alarm" permission prompt on Android 12+.
- *"Does `WorkManager` run at the scheduled time?"* → No. It runs "as soon as possible after the delay, subject to constraints and Doze/standby restrictions".
- *"What is Doze exactly?"* → The device is idle, screen off, unplugged. Network is suspended, alarms are deferred, jobs are batched. `setExactAndAllowWhileIdle` and `setAlarmClock` are the exceptions.
- *"How do OEM battery optimisations affect this?"* → Aggressive OEM ROMs (Xiaomi, Huawei, Vivo, Samsung) can kill background work and block alarms even when the platform permits them. You must detect and prompt the user to whitelist the app — a real-world requirement in many markets.
- *"How do you test this?"* → `adb shell dumpsys deviceidle force-idle` and `unforce`; `adb shell cmd jobscheduler run -f <jobId>`; and real devices in the `rare` bucket.

---

### 9. How would you choose between WorkManager, a foreground service, and a regular coroutine for long-running work?

**What the interviewer is really asking**
Can you lay out a **decision framework** grounded in the platform's actual guarantees, and state the trade-offs (guarantee, immediacy, user visibility, resource cost) rather than defaulting to one mechanism?

**Key concepts**
- The three mechanisms and their guarantees
- Foreground service types (Android 14 requires a type + permission) and the API 35 `dataSync` 6h/24h cap
- Background start restrictions (Android 12+): you cannot start an FGS from the background
- `WorkManager` guarantees: deferred, persistent (survives reboot), constraint-aware, at-least-once
- `WorkManager` expedited quota (~10 min/day of foreground work on API 31+)
- Foreground service → the user sees a notification; Play policy requires a visible, meaningful notification
- `ProcessLifecycleOwner` / `lifecycleScope` for UI-scoped work
- `CoroutineScope` lifetime rules
- Play Store policy on background work and the "notification must be user-visible" requirement
- Android 14 FGS type requirements: `dataSync`, `mediaPlayback`, `mediaProcessing`, `shortService`, `specialUse`
- `BOOT_COMPLETED` receivers for restarting FGS after a reboot

**Approach**

1. **Start from the guarantee you need, then pick the mechanism.** The decision is driven by three questions:
   - **Must it survive process death / reboot?** → `WorkManager` or a foreground service. A plain coroutine cannot.
   - **Must it start *now*, with no delay?** → A foreground service (and only if the app is in the foreground, or via an exemption). `WorkManager` may defer it.
   - **Must the user see it?** → A foreground service with a notification.

2. **The framework:**

   | Mechanism | Use when | Guarantees | Limits |
   |---|---|---|---|
   | **Coroutine** (`viewModelScope` / `lifecycleScope` / app scope) | Work tied to a screen or to the app session; short, cheap, cancellable | Runs while the scope lives | Dies with the process; no constraints; no persistence |
   | **`WorkManager`** | Deferred, deferrable, condition-based work: sync, upload, prefetch, periodic maintenance | Survives process death and reboot; Doze-aware; constraints; backoff; at-least-once | **Not wall-clock exact**; may be delayed by Doze/standby buckets; expedited quota; needs idempotency |
   | **Foreground service** | Immediate, user-visible, long-running work that must not be deferred: playback, an active navigation session, an ongoing upload the user is watching | Runs immediately; the user sees a notification; the system is less likely to kill it | Cannot be started from the background (Android 12+); requires a type + permission (Android 14+); API 35 `dataSync` cap; Play policy scrutiny |

3. **Detailed guidance per mechanism.**

   **Coroutine** — the default for anything under a few seconds or tied to a UI:
   - `viewModelScope` — survives rotation, cancelled on `onCleared()`. Use for screen-scoped work.
   - `lifecycleScope` — cancelled when the Lifecycle is destroyed; use for UI-observing work.
   - An **application-scoped** `CoroutineScope` — for work that must outlive any screen (e.g. a global sync trigger). Requires careful lifecycle management and a `SupervisorJob`.
   - **Never** `GlobalScope` — it has no lifecycle, no cancellation, and leaks.

   **`WorkManager`** — the default for "must happen, but not necessarily now":
   - `OneTimeWorkRequest` for one-off deferrable work.
   - `PeriodicWorkRequest` for recurring work (minimum 15 minutes; the OS decides the actual cadence).
   - `setExpedited` for work that must run soon (quota-limited; the fallback policy ensures it still runs).
   - Always idempotent (at-least-once semantics).

   **Foreground service** — the default for "must run right now and the user is watching":
   - Declare the correct `foregroundServiceType` and the matching `FOREGROUND_SERVICE_*` permission.
   - Call `startForeground()` within ~5 seconds of `startForegroundService()` (or you get `ForegroundServiceDidNotStartInTimeException`).
   - Post a meaningful, ongoing notification (Play policy: the notification must be user-visible and dismissible only when the work ends).
   - Handle `START_STICKY`/`START_NOT_STICKY` deliberately.
   - Android 12+: you can only start it from the foreground or with an exemption (e.g. from a `BroadcastReceiver` for a high-priority FCM, from an exact alarm, or when the user interacts with a notification).

4. **The hybrid pattern most real apps need.** `WorkManager` for durability + `setForeground()` for immediacy and visibility:
   ```kotlin
   override suspend fun doWork(): Result {
       setForeground(createForegroundInfo())   // promoted to an FGS while it runs
       // ... do the work
   }
   ```
   This is the recommended pattern for user-initiated long work (uploads, downloads) — it gets the durability of `WorkManager` and the foreground privileges of an FGS.

5. **Consider a fourth option: the server.** For work that is fundamentally server-side (sending a scheduled message, processing a document), push the intent to the backend and let the server do it, using FCM to notify the client. This removes the platform's scheduling constraints entirely — but it requires the user to allow notifications and a network connection.

6. **Do not overuse foreground services.** They cost battery and memory, show the user a notification, and are subject to Play policy review. Each FGS must justify itself. In 2024–2025 Google tightened the policy significantly (no FGS for location sharing, no `dataSync` for unknown destinations, etc.).

7. **Instrument and measure.** Track per mechanism: job success rates, FGS lifetimes, battery impact. A foreground service running for hours is a finding.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Coroutine | Simplest; cheap; cancellable | No survival of process death; no constraints; no scheduling |
| `WorkManager` | Durable, deferrable, Doze-aware, constrained | Not exact; delayed; quota-limited when expedited; idempotency required |
| Foreground service | Immediate; visible; hard to kill | Background-start restrictions; FGS type + permission (API 34+); API 35 caps; Play policy; battery |
| `WorkManager` + `setForeground` | Durable *and* immediate/visible | Still subject to the expedited quota and the `dataSync` cap; more complex |
| Server-side execution + push | No platform scheduling limits | Requires notifications + network; the user can disable it; latency |
| `AlarmManager` + `WorkManager` | Exact triggering + durable execution | Exact-alarm permission on API 31+; Doze caveats |

**Key detail:** The decision is not really "which one" but "**what is the guarantee the product needs**". A staff-level answer gives the framework and then says: *"for a user-initiated upload, the answer is `WorkManager` with `setForeground` — durability from one, immediacy and visibility from the other."*

**Code example**

```kotlin
// The hybrid: WorkManager for durability + foreground service for immediacy.
class UploadWorker(
    context: Context,
    params: WorkerParameters,
    private val uploader: ResumableUploader,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Promote to a foreground service so the work is not deferred and is visible.
        setForeground(
            ForegroundInfo(
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        )
        return try {
            uploader.upload { sent -> setForeground(buildNotification(sent)) }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Result.retry()
        }
    }
}

// A screen-scoped coroutine for work that is cheap and tied to the UI.
class DetailViewModel(repo: DetailRepository) : ViewModel() {
    fun load(id: String) = viewModelScope.launch {   // cancelled on onCleared()
        _state.value = UiState.Loading
        _state.value = repo.load(id).toUiState()
    }
}

// An application-scoped coroutine for work that must outlive every screen.
@Singleton
class AppScope @Inject constructor() {
    // Explicitly scoped, never GlobalScope.
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    // The Application must cancel it on termination (rare in practice, but correct).
}
```

**Talking points / follow-ups**
- *"When is a foreground service justified?"* → When the user is actively waiting on the work, or the work is user-visible and continuous (playback, navigation, an active transfer). Play policy requires the notification to be meaningful.
- *"What are the Android 14 foreground service type requirements?"* → `startForeground()` must specify a type, and the app must hold the matching `FOREGROUND_SERVICE_<TYPE>` permission. Types include `dataSync`, `mediaPlayback`, `mediaProcessing`, `camera`, `location`, `phoneCall`, `shortService`, `specialUse`.
- *"What is the expedited quota?"* → On API 31+, expedited work can consume roughly 10 minutes of foreground execution per day per app; beyond that, `RUN_AS_NON_EXPEDITED_WORK_REQUEST` applies.
- *"How do you handle work that must complete but the app is killed?"* → `WorkManager` — it is persisted by the system and re-scheduled after process death and reboot.
- *"How do you avoid a foreground service being killed?"* → You cannot guarantee it, but an FGS is far less likely to be killed than a background service. Use `START_STICKY` and persist state so a restart can resume. Android 12+ restricts restarting an FGS from the background, so a `BOOT_COMPLETED` receiver plus a `WorkManager` job is the robust path.
- *"How do you measure the cost?"* → `adb shell dumpsys activity services <pkg>`, battery stats (`adb shell dumpsys batterystats`), and per-mechanism telemetry.

---

## Section Summary — Staff-Level Themes

| Theme | Questions |
|---|---|
| **Matching the mechanism to the guarantee** | 1, 2, 3, 7, 9 — durability, immediacy, visibility, and the platform's actual limits |
| **Idempotency as a precondition** | 2, 3 — `WorkManager` is at-least-once; every worker must be safe to repeat |
| **Resumability and checkpointing** | 1, 5, 7 — state in the database, not in memory; ranges, checksums, `.part` files |
| **Platform constraints as design inputs** | 1, 4, 8 — background-start restrictions, Doze, standby buckets, exact-alarm policy, API 35 caps |
| **Correct lifecycle ownership** | 4, 5, 6 — player vs position, focus protocol, notification tap routing |

**The one sentence to memorise for this section:**
> *"Pick the mechanism from the guarantee the product actually promises — a coroutine for work tied to a screen, `WorkManager` for work that must eventually happen, a foreground service for work that must happen now and be seen — and then make every one of them idempotent, checkpointed, and safe to kill at any moment."*
