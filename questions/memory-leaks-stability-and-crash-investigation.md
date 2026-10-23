# Memory Leaks, Stability and Crash Investigation

> Answers written from the perspective of a **Staff Android Engineer**. Each answer is structured as:
> *What the interviewer is really asking* → *Key concepts* → *Approach* → *Trade-offs* → *Code* → *Talking points*.

---

### 1. An Activity leaks memory after the user repeatedly opens and closes a screen. How would you identify and fix the leak?

**What the interviewer is really asking**
Can you run a disciplined **leak investigation** — reproduce, observe, get a heap dump, find the retaining path, fix it, and *verify* the fix — rather than guessing at common causes?

**Key concepts**
- `Activity` (and `Context`) leaking → the whole view hierarchy + Activity retained
- Retaining path analysis in a heap dump (MAT / Android Studio Memory Profiler)
- `LeakCanary` with `HeapDump` and `LeakTrace`
- `Application` context vs `Activity` context
- Non-static inner classes capturing `this`
- `Handler`/`Runnable` posted with a delay; `Timer`/`TimerTask`; `CountDownTimer`
- Unregistered listeners/callbacks; `BroadcastReceiver` not unregistered
- `static` fields holding references
- `Single`/`Object` singletons capturing an Activity

**Approach**

1. **Reproduce deterministically.** The classic reproduction is: rotate the Activity 5–10 times, then navigate away, and watch the heap. In production, the signal is: after N visits, `Activity` instances in the heap keep growing.

2. **Enable LeakCanary in debug builds.** It is essentially free and it will tell you the leak *without* you having to know where to look:
   ```kotlin
   // debugImplementation only — LeakCanary is a no-op in release
   debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
   ```
   LeakCanary auto-installs a `ServiceWatcher` and dumps the heap when an Activity is destroyed while still reachable. Its `LeakTrace` shows the exact retaining path: `GC root → ... → MyActivity`.

3. **Read the retaining path, not the symptom.** The output tells you which reference is the problem:
   - `GC root → MyActivity` — the Activity itself is the leak (e.g. a static field holds it).
   - `GC root → ContextImpl → MyActivity` — something held the Activity via its `Context`.
   - `GC root → static field of MyClass → MyActivity` — a static/singleton reference.
   - `GC root → Handler → MyActivity` — a delayed message.
   - `GC root → Thread → ...` — a still-running coroutine or thread capturing `this`.

4. **Confirm with a heap dump** in Android Studio Memory Profiler (or `hprof` in MAT): `Debug > Dump Heap`, then use the "Find Instances" / "Leak Suspects" view. Look for the number of `Activity` instances over time.

5. **Common root causes and their fixes:**

   | Root cause | Fix |
   |---|---|
   | Non-static inner class / anonymous object holding the Activity | Make it `static`, or pass the Activity weakly, or move the callback into the ViewModel |
   | `Handler().postDelayed(runnable)` with a 5-minute delay | Store the handler, remove callbacks in `onDestroy`; or use `lifecycleScope` + `delay` (auto-cancelled) |
   | Singleton / `object` holding the Activity | Inject the Application context; never store an Activity in a singleton — see Q2 |
   | `BroadcastReceiver` registered but never unregistered | `context.registerReceiver(...)` in `onStart`, `unregisterReceiver` in `onStop`; or use `ContextCompat.registerReceiver` with the correct flags on API 33+ |
   | `ContentObserver` / `TextWatcher` / `OnClickListener` never removed | Remove in `onDestroy`; prefer `lifecycleScope`/`repeatOnLifecycle` |
   | Static `Application` field holding a View | Never cache Views in statics |
   | Long-running coroutine capturing the Activity | `viewModelScope` instead of `lifecycleScope` for work that outlives the screen; `lifecycleScope` is cancelled at DESTROY so it is safe |
   | `CountDownTimer` / `Timer` still ticking | `cancel()` in `onDestroy` |
   | Third-party SDK retaining the Activity | Use the SDK's application-context API; report to the vendor |

6. **Verify the fix.** Re-run the reproduction and confirm the `Activity` instance count returns to a stable baseline (1, not N). Add a **leak regression test** in CI:
   ```kotlin
   @Test fun activityIsGarbageCollectedAfterNavigation() {
       ActivityScenario.launch(MainActivity::class.java).use { scenario ->
           scenario.moveToState(Lifecycle.State.DESTROYED)
       }
       LeakCanary.assertNoLeaks()   // or check the retained count via LeakCanary's API
   }
   ```

7. **Prevent rather than fix.** Lint rule `StaticFieldLeak`, and code review rules: "never store a Context in a static field" and "always pair register/unregister". A `Context` parameter in a non-Activity class should be typed `Context` and receive `applicationContext`.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| LeakCanary in debug | Finds leaks automatically with a precise retaining path | Debug-only; adds a dependency; no value in release (heap dumps in production are impractical) |
| Manual heap dump analysis | Full control, works in release | Requires expertise; slow; the "dump the heap" workflow is not part of the normal dev loop |
| Static/`object` singletons | Convenient, easy access | The #1 leak source; couples the singleton's lifetime to the UI's |
| Application context in singletons | Safe, no leak | Application context is a long-lived leak of *other* things (e.g. a static Activity leaks everything) — but it does not pin the Activity |
| Fixing leaks in production with a hotfix | Immediate relief | A `Context` leak is usually small; better to fix properly and ship in the next release |
| `WeakReference` to the Activity | "Fixes" the leak | Merely moves the crash — the WeakReference can be collected mid-use. **Not a real fix**; a great thing to call out |

**Key detail:** `lifecycleScope` is cancelled when the Lifecycle reaches `DESTROYED`, so a coroutine launched there cannot leak the Activity. `viewModelScope` is cancelled on `onCleared()`. But a coroutine launched on `GlobalScope`/`applicationScope` **can** leak — and that is where most leaks hide.

**Code example**

```kotlin
// BAD: inner class captures the Activity; the Handler keeps it for 5 minutes.
class DetailActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            handler.postDelayed(this, 5_000)
            updateTimer()                 // captures DetailActivity
        }
    }
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        handler.post(ticker)
    }
    // no onDestroy cleanup → Activity retained for 5 minutes
}

// GOOD: lifecycle-scoped, self-cancelling.
class DetailActivity : AppCompatActivity() {
    private val scope = lifecycleScope

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        scope.launch {
            while (isActive) {
                updateTimer()
                delay(5_000)              // cancelled automatically on DESTROY
            }
        }
    }
}
```

**Talking points / follow-ups**
- *"How do you tell a memory leak from normal memory growth?"* → A leak shows a **monotonic increase correlated with a user action** (each navigation adds one instance); normal growth is bounded and eventually plateaus. LeakCanary's repeated-instance detection is the tool.
- *"What's the difference between a leak and a cache?"* → A cache is a bounded, intentional retention with an eviction policy; a leak is unbounded, unintentional retention of a destroyed object.
- *"Do you always need `WeakReference`?"* → No. It hides the design problem. Use it only for genuinely optional callbacks (e.g. a long-lived observer that may or may not be alive).
- *"Why does the leak only appear after N visits?"* → Because the system has memory pressure at some point and only then does the GC have to collect; also heap growth must hit a threshold before the object graph is walked. Also, the OS may not kill the process so the Activity stays reachable.
- *"What is `android:noHistory` and why is it not a leak fix?"* → It finishes the Activity when the user navigates away, avoiding the retention but changing the back-stack behaviour; it is a workaround, not a fix.

---

### 2. A singleton retains a reference to an Activity. Why is this dangerous, and how would you redesign the dependency?

**What the interviewer is really asking**
Can you articulate the **lifetime mismatch** problem — a process-lifetime object holding a UI-lifetime object — and design a dependency inversion that fixes it structurally rather than with a `WeakReference` patch?

**Key concepts**
- Lifetime mismatch (application scope vs Activity scope)
- Dependency inversion: depend on abstractions, not on the `Context`
- `Application` context vs `Activity` context
- Constructor injection with `@Inject`
- Scoping in Hilt/Dagger (`@Singleton`, `@ActivityRetainedScoped`, `@ActivityScoped`, `ViewModelScoped`)
- `ContextWrapper` and `applicationContext`
- Observing patterns: `Flow`/`LiveData` decoupled from the observer's lifetime
- Leaking the whole view hierarchy (Activity holds `mWindow`, `mDecor`, `mContext` → the whole tree)

**Approach**

1. **Explain the danger precisely.**
   - A `singleton` (or `object`, or `static` field) lives for the **process lifetime** (potentially hours/days).
   - An `Activity` lives for **seconds to minutes**.
   - When the singleton holds the Activity, the Activity **cannot be garbage collected** — even after it is destroyed and the user has navigated away.
   - The blast radius is large: an Activity holds `mWindow` → `WindowManager$DecorView` → the **entire view hierarchy** → every View, every ContextWrapper, every Bitmap that view holds. So leaking one Activity can leak tens of megabytes.
   - Over repeated navigations, the process's heap grows until the system kills the app or the app throws `OutOfMemoryError`. This is the classic "app dies after navigating to Settings 10 times" bug.

2. **Show the anti-pattern:**
   ```kotlin
   object AnalyticsTracker {
       var activity: Activity? = null     // never nulled → permanent leak
       fun track(event: String) { activity?.let { log(it, event) } }
   }
   class MainActivity : AppCompatActivity() {
       override fun onCreate(s: Bundle?) {
           super.onCreate(s)
           AnalyticsTracker.activity = this     // BOOM
       }
   }
   ```

3. **Redesign — three layers, best first:**

   **(a) Invert the dependency: the singleton depends on an abstraction, not on the Activity.**
   ```kotlin
   interface EventSink {
       fun log(event: String, attrs: Map<String, Any?> = emptyMap())
   }

   class FirebaseEventSink @Inject constructor() : EventSink {
       override fun log(event: String, attrs: Map<String, Any?>) { /* no Context needed */ }
   }
   ```
   The sink needs no `Context` at all, or only the `Application` context — which is safe to hold forever.

   **(b) Scope the object to the Activity's lifetime instead of the process.** With Hilt/Dagger:
   ```kotlin
   @Singleton
   class UserSession @Inject constructor() { /* process-scoped state */ }

   @ActivityRetainedScoped   // survives config change, dies with the Activity
   class ScreenTracker @Inject constructor(private val sink: EventSink) {
       var screenName: String? = null
       fun onScreenVisible(name: String) { screenName = name; sink.log("screen_view") }
   }
   ```
   `@ActivityRetainedScoped` is the right scope for "must live as long as the screen" state — it survives rotation but is released with the Activity.

   **(c) Observe instead of own.** For the "notify the tracker when the Activity changes" case, use a `Flow`/`LiveData` that the Activity *subscribes* to and *unsubscribes* on destroy — the observer pattern inverts the ownership:
   ```kotlin
   class SessionEvents @Inject constructor() {
       private val _events = MutableSharedFlow<String>(extraBufferCapacity = 16)
       val events: SharedFlow<String> = _events.asSharedFlow()
   }
   ```
   The Activity collects with `repeatOnLifecycle`; when it is destroyed, the collection stops automatically.

4. **If the singleton genuinely needs a `Context`, inject the `Application` context only:**
   ```kotlin
   @Singleton
   class ImageLoader @Inject constructor(@ApplicationContext private val context: Context) {
       // context here is the Application → safe to hold forever
   }
   ```
   `@ApplicationContext` in Hilt, or `context.applicationContext` manually.

5. **If a callback must reference the Activity temporarily, use a weak reference *plus* correct lifecycle handling** — but note the trade-off: `WeakReference` can be cleared at any time, so it is only appropriate for optional callbacks, never for a callback you must invoke.

6. **Enforce it structurally:** a lint rule / detekt rule that flags `static`/`object` fields of type `Activity`, `View`, `Dialog`, or `Context` (other than `Application`). Detekt's `ObjectPropertyNaming`/`ForbiddenComment` set and Lint's `StaticFieldLeak` help; code review enforces the rest.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Inject `Application` context in the singleton | Simple, safe | Application context is itself a long-lived object — never put a `Dialog`, `View`, or Activity-specific resource in it |
| Scope to `@ActivityRetainedScoped` | Correct lifetime; survives rotation | Requires DI framework support; object recreated per Activity |
| Flow/observer pattern | No ownership of the observer; auto-unsubscribes | More indirection; events emitted while nobody observes are lost unless replayed |
| `WeakReference` to the Activity | Quick patch | **Not a fix** — the callback may never fire; hides the design flaw |
| `static` reference + `onDestroy { ref = null }` | Works if `onDestroy` always runs | `onDestroy` is not guaranteed on process death; easy to forget; still a smell |
| Application-scoped `object` with no Context at all | Zero leak surface | Requires the logic to not need a Context (usually true for analytics, prefs via DataStore) |

**Key detail:** The most important architectural point: **a process-lifetime object should never hold a shorter-lived object.** The fix is usually to *reduce what the singleton needs* rather than to *shorten the singleton's life*.

**Code example**

```kotlin
// BEFORE — the classic leak
object SessionManager {
    var currentActivity: Activity? = null
    fun onResume(activity: Activity) { currentActivity = activity }
}

// AFTER — inverted dependency, correct scoping
interface Analytics {
    fun log(event: String)
}

@Singleton
class RemoteAnalytics @Inject constructor() : Analytics {
    override fun log(event: String) { FirebaseAnalytics.getInstance().logEvent(event) }
}

@ActivityRetainedScoped
class ScreenTracker @Inject constructor(private val analytics: Analytics) {
    fun onScreenVisible(screen: String) {
        analytics.log("screen_view")
    }
}

class MainActivity @Inject constructor(
    private val tracker: ScreenTracker,
) : AppCompatActivity() {
    private val scope = lifecycleScope

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        scope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { observeSession() } }
    }

    private suspend fun observeSession() {
        sessionEvents.events.collect { tracker.onScreenVisible(it) }
    }
}
```

**Talking points / follow-ups**
- *"What is the difference between `applicationContext` and `Activity` as a field?"* → The Application context is process-lifetime and lightweight; the Activity context carries theme, window, and the view hierarchy and is short-lived.
- *"Can a singleton legitimately hold a `Context`?"* → Yes, the Application context. Never an Activity, View, or Dialog.
- *"How do you prevent this in a codebase with hundreds of modules?"* → Lint/detekt rules, a DI scoping convention, and an architecture decision record. Enforcement matters more than one-off fixes.
- *"What about a `Fragment` reference in a singleton?"* → Same rule; Fragment is shorter-lived than Activity.
- *"How do you confirm the fix?"* → LeakCanary shows no retained instances after N navigation cycles; and the retained count returns to a stable baseline.

---

### 3. A crash occurs only on Android 14 and later. How would you investigate platform-specific behavior?

**What the interviewer is really asking**
Can you run a **systematic version-difference investigation** — enumerate what actually changed in API 34, reproduce on the affected OS versions, use conditional code paths, and roll out defensively without waiting for a fix?

**Key concepts**
- API 34 (Android 14) behaviour changes: implicit broadcasts, `registerReceiver` export flags, foreground service types, `Runtime`/`READ_MEDIA_*` permissions, `WorkManager` expedited quota, `startForegroundService` restrictions, `PendingIntent` mutability, package visibility, `JobScheduler` quotas
- `Build.VERSION.SDK_INT` checks and `@RequiresApi`
- `Build.VERSION_CODES.UPSIDE_DOWN_CAKE`
- Gradle `compileSdk`/`targetSdk` vs `minSdk`
- Play Console **device catalog** and version-specific crash clusters
- `adb shell` targeting a specific API level image (emulator/Genymotion/Sauce Labs)
- `BuildConfig` per-variant flags
- Feature detection vs version checks
- Remote config kill switch

**Approach**

1. **Quantify the blast radius before debugging.** In Crashlytics/Bugsnag, filter by `OS version == 14` / `API level == 34` and look at:
   - crash-free users and sessions by OS version,
   - the exact exception type and stack,
   - the device model and OEM ROM (many "Android 14" bugs are actually a specific OEM's skin),
   - the app version — was it introduced by the most recent release?

2. **Get the exact signature.** "Crashes only on Android 14" is not actionable; `SecurityException: Permission Denial: ... requires android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK` is. Pull the top stack traces and group by signature.

3. **Read the actual behaviour-change documentation** for API 34 and diff it against your code's usage. In Android 14 the changes most likely to bite are:
   - **Implicit broadcast restrictions** — apps can no longer register for most *implicit* broadcasts; only a fixed list is allowed. Custom implicit broadcasts now require a `RECEIVER_EXPORTED`/`RECEIVER_NOT_EXPORTED` flag and are blocked for non-system apps.
   - **`registerReceiver` export flag required** on API 34 for context-registered receivers.
   - **Foreground service types required** — `startForeground` must declare a type and the matching `FOREGROUND_SERVICE_*` permission; starting a service with no type throws.
   - **Foreground service start restrictions** — a foreground service cannot be started from the background in most cases.
   - **Partial screen-share / media projection opt-in** — `MediaProjection` now requires the user to consent to single-app capture.
   - **Runtime read media permissions** — `READ_MEDIA_VISUAL_USER_SELECTED` grants access to only the selected items; code that assumes full access breaks.
   - **Package visibility** — `queryIntentActivities` returns fewer results unless you declare a `<queries>` element.
   - **`JobScheduler` and `WorkManager` expedited quota** — `RUN_AS_NON_EXPEDITED_WORK_REQUEST` is used when the app is in the foreground; quota is consumed faster.
   - **Non-SDK interface restrictions tightened.**
   - **`Context.getSystemService` credential-encrypted storage behaviour on Android 14** — some credential-encrypted storage files are moved to device-protected storage on direct boot, which can surprise file-based storage code.

4. **Reproduce on the exact OS.** Run the app on an API 34 emulator (or a physical device on Android 14, plus the OEM ROM from the crash cluster). API 34 emulator images from Android Studio. Reproduce the *user flow* from the crash report, not just the code path.

5. **Guard at the right level.**
   - **Feature detection first**: check the actual API exists (`context.getSystemService(Foo::class.java)`) rather than the version number, when the feature is not version-gated in a simple way.
   - **Version check where the change is purely behavioural**:
     ```kotlin
     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
         ContextCompat.registerReceiver(
             context, receiver, filter,
             ContextCompat.RECEIVER_NOT_EXPORTED,
         )
     } else {
         @Suppress("DEPRECATION")
         context.registerReceiver(receiver, filter)
     }
     ```
     (In practice `ContextCompat.registerReceiver` handles the flag for you — that is the correct, lint-clean answer; use the version check only where the behaviour itself differs.)
   - **Guard the FGS type at runtime** with the constant that matches the API level:
     ```kotlin
     val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
         ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
     } else 0
     ```
   - **Target the correct SDK**: `targetSdk` gates most of these changes. Check `targetSdkVersion` in the merged manifest and be aware that Play enforces new target levels each year — a crash on Android 14 often appears only after you bump `targetSdk`.

6. **Fail soft where you can.** If a feature is unavailable on the affected OS, degrade rather than crash: feature-detect, disable, and log. Wrap risky platform calls in a `runCatching` **only** when a fallback exists — do not use `runCatching` as a blanket crash-suppressor for security exceptions.

7. **Roll out defensively.** If you cannot fix it before the next release:
   - Use **remote config / feature flags** to disable the affected feature for API 34+ specifically.
   - Use **Play Console staged rollout** (see Section 11, Q9) to limit exposure.
   - Use **App Bundles with version-specific splits** if the issue is a library bug fixed in a newer version — bump the dependency and let the bundle deliver it.
   - Set a **minimum API gate**: if a third-party SDK is the culprit and you cannot upgrade it, set `minSdk` appropriately or gate the feature.

8. **Add a regression test.** Emulator API 34 in CI (or Firebase Test Lab) with a test covering the affected flow. At minimum, add the API 34 emulator to the manual QA matrix.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Version check (`SDK_INT`) | Explicit, compiles to a constant branch | Brittle: OEM ROMs may backport behaviour; you maintain a matrix |
| Feature detection | Robust to backports and future versions | Not possible when behaviour changed without an API change (Android 14 has many such cases) |
| Raise `targetSdk` deliberately | Unlocks modern behaviour; required by Play | Triggers all behaviour changes at once — the classic source of "crash after targetSdk bump" |
| Keep `targetSdk` low | Avoids behaviour changes | Play Store will reject the release; you accumulate technical debt |
| Remote config kill switch | Instant mitigation without a release | Requires the flag plumbing and a remote-config dependency; a crutch if used permanently |
| Upgrade the offending library | Real fix | May require API changes; check the library's compatibility matrix |

**Key detail:** The first question to ask is *"which OS versions exactly, and which OEMs?"* — a bug that appears "only on Android 14" is often "only on Samsung's Android 14 skin" or "only on API 34 devices with a specific Google Play Services version". The crash cluster's device breakdown answers this before any code reading.

**Code example**

```kotlin
// Feature detection where possible, version check where the behaviour is behavioural.
object ReceiverRegistrar {
    fun register(context: Context, receiver: BroadcastReceiver, filter: IntentFilter) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(
                context, receiver, filter,
                ContextCompat.RECEIVER_NOT_EXPORTED,   // don't accept broadcasts from other apps
            )
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
    }
}

object PlaybackService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
        startForeground(NOTIFICATION_ID, buildNotification(), type)
        return START_STICKY
    }
}
```

**Talking points / follow-ups**
- *"Why did it start crashing right after a release that changed nothing?"* → The release bumped `targetSdk` (Play enforces this annually), which enabled the Android 14 behaviour changes. This is the most common root cause.
- *"How do you test across OS versions?"* → Firebase Test Lab (device matrix), Emulator API levels, and at least one physical device on the lowest supported API and the newest.
- *"Do you have a device lab?"* → A small lab of representative devices (low-end Android Go, mid-range, flagship) with the top-5 OEM ROMs. Budget for it; it is cheaper than production crashes.
- *"What if the bug only occurs on a specific OEM's modified framework?"* → You cannot fix the OEM; you detect the affected device (Build.MANUFACTURER/MODEL) and gate the feature, and report upstream to the SDK vendor.
- *"How do you validate the fix for 2% of users?"* → Stage the rollout, watch the crash-free metric for the affected segment specifically, then ramp.

---

### 4. The app crashes intermittently with IllegalStateException in production, but you cannot reproduce it locally. What is your investigation strategy?

**What the interviewer is really asking**
Do you have a **systematic debug process** for non-reproducible production crashes: gather signal (fatals, breadcrumbs, session traces), narrow the population, form a hypothesis, and validate it with a targeted reproduction or a hotfix — rather than guessing?

**Key concepts**
- Crash reporting: Crashlytics, Bugsnag, Sentry; **fatal vs non-fatal**, custom keys, breadcrumbs
- `IllegalStateException` subtypes: `Fragment not attached`, `Activity already finished`, `Cannot perform this action after onSaveInstanceState`, `View not attached to window`, `IllegalStateException: View already attached`
- `IllegalStateException` as a *generic* signal — you must read the **message and stack**, not the type
- Device/API/OEM distribution of the crash
- Session replay / event timeline
- Debug builds vs release builds (obfuscation, R8, `minifyEnabled`) — stack traces get mangled
- `BuildConfig.DEBUG` guards around logging
- Reproducibility aids: debug symbols upload (`mapping.txt`), `crashlytics` symbol files
- Feature flags to bisect a suspect change
- Thread dump / ANR traces

**Approach**

1. **Read the message, not just the type.** `IllegalStateException` is thrown from dozens of places. Bucket the crashes by **message signature** in the crash reporter — this immediately splits one "intermittent" problem into 2–3 distinct bugs. One of them will usually be dominant.

2. **Slice the population.** For each signature, break down by:
   - app version (is it new? did it start at this release?)
   - OS version / API level / OEM model
   - screen or feature (from breadcrumbs)
   - network state, user account type, locale, A/B variant
   - whether the user was on a slow network, low battery (Doze), or low storage
   A crash that correlates with a device cohort (e.g. only on 4GB RAM devices, only during Doze) tells you where to look.

3. **Add the missing breadcrumbs.** Most "cannot reproduce" crashes are un-reproducible because the reporter lacks context. Instrument the flow with `breadcrumb()` calls at each state transition: screen entered, request started, response received, retry count, user action. Then the crash report shows the last 20 events before the crash. Also set **custom keys**: current screen, user id hash, feature flags active, app version, network type, foreground/background, screen-on/off.

4. **Turn the fatal into a non-fatal where safe.** If a crash is in a non-critical path (e.g. an analytics SDK throwing), catch it at that boundary and report it as a non-fatal, keeping the app alive. This converts a crash-out into a metric you can watch — and it stops the "app dies, user can't continue" problem.

5. **Add a "diagnostic mode".** For the top suspect flows, add verbose logging behind `BuildConfig.DEBUG` **plus** a remote-config-enabled verbose flag for a small internal cohort. Do not ship unbounded logging to all users.

6. **Bisect the change.** If the crash started at release N:
   - Compare the crash rate for N vs N-1 by signature.
   - Use **remote config to disable** the suspect feature for a fraction of users (a feature flag kill-switch) — this both mitigates and acts as a **bisection experiment**. If the crash drops to zero when the flag is off, the feature is the cause.
   - Check whether it correlates with the `targetSdk` bump (Android 14 changes — see Q3).

7. **Reduce to a deterministic reproduction.** Once you know the cohort, try to reproduce it:
   - Throttle the network (`adb shell tc` / Network Link Conditioner) to trigger timeouts.
   - Background the app mid-request, rotate, kill and restore, deny permissions, revoke notifications.
   - Test on the exact device/OS from the cluster, with a release build (not debug) — release builds differ (R8, no `debuggable` timing differences).
   - Use `adb shell am set-debug-app -w` on the affected device, and grab a tombstone/`logcat` buffer.

8. **Fix defensively where the root cause is a race.** Many `IllegalStateException`s are "act on a destroyed/unsaved component" races. Fixes:
   - Guard UI work with `lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)` or check `isFinishing`.
   - Use `repeatOnLifecycle` instead of raw `lifecycleScope.launch` for observers.
   - Never touch a `Fragment`/`Context` after `onDestroyView`; check `viewLifecycleOwner`.
   - Defer fragment transactions to `commitAllowingStateLoss` only as a last resort — prefer `commit()` after `onStateRestored`.
   - Null out listeners in `onDestroyView`.

9. **Ship a hotfix and verify.** Then watch the affected signature's crash-free rate over the next release cycle. Add a regression test reproducing the deterministic version.

10. **Close the loop.** If the crash is in a third-party SDK, escalate with a minimal reproduction and the cohort data; if it cannot be fixed, add a defensive wrapper and consider excluding affected devices.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Broad breadcrumbs everywhere | Maximum context | Log volume, PII risk, noise |
| Targeted breadcrumbs on suspect flows | Focused signal | Requires knowing where to look initially |
| Catching and downgrading to non-fatal | Users stay in the app; becomes a watchable metric | Can mask real problems; a swallowed crash may leave inconsistent state |
| Remote-config kill switch | Instant mitigation + bisection | Flag infrastructure cost; users lose the feature |
| Shipping a release-level fix | Permanent | Slower; requires a release cycle and store review |
| Hotfix via Play Console | Fast | Only for crashes on the same version; limited to 2 weeks/rollout |

**Key detail:** Most crash reporters let you filter by **custom keys and breadcrumbs**. The single highest-leverage change is to add a `current_screen` key and a `breadcrumb()` on every navigation and every request. "Cannot reproduce" is usually "we did not record enough".

**Code example**

```kotlin
// Instrument the flow so the next occurrence is diagnosable.
class CheckoutViewModel(
    private val repo: CheckoutRepository,
    private val tracker: CrashTracker,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<Receipt>>(UiState.Loading)
    val state: StateFlow<UiState<Receipt>> = _state.asStateFlow()

    fun load(orderId: String) = viewModelScope.launch {
        tracker.setKey("current_screen", "Checkout")
        tracker.breadcrumb("load started orderId=$orderId")
        when (val r = repo.getReceipt(orderId)) {
            is AppResult.Success -> {
                tracker.breadcrumb("load succeeded")
                _state.value = UiState.Success(r.data)
            }
            is AppResult.Failure -> {
                tracker.breadcrumb("load failed error=${r.error::class.simpleName}")
                tracker.recordNonFatal(r.error)          // non-fatal, keeps the app alive
                _state.value = UiState.Error(r.error)
            }
        }
    }
}

// Defensive fix for a classic IllegalStateException source:
// a fragment transaction after onSaveInstanceState.
class SummaryFragment : Fragment() {
    override fun onViewCreated(v: View, s: Bundle?) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Safe: we are STARTED, so the fragment is attached and state is saved.
                    render(state)
                }
            }
        }
    }
}
```

**Talking points / follow-ups**
- *"What are the most common `IllegalStateException`s in Android apps?"* → `Fragment not attached to a context`, `Activity already finishing`, `Can not perform this action after onSaveInstanceState`, `View not attached to window manager`, `IllegalStateException: FragmentManager is already executing transactions`, `RecyclerView` adapter state issues, `lifecycleScope` misuse.
- *"Why can you not reproduce it locally?"* → Usually one of: it depends on data/state that only production has, it depends on timing/load, it depends on a device/OS cohort, or it is a release-only build difference (R8/proguard, missing `debuggable`).
- *"Do you always fix by adding try/catch?"* → No. A blanket catch hides the bug and can leave state inconsistent. Fix the race or the lifecycle misuse; catch only where a graceful degradation exists.
- *"What do you do when the crash is in a third-party SDK?"* → Isolate it (wrap the call), report upstream with cohort data and a minimal repro, disable the feature remotely if needed, and consider replacing it.
- *"How do you make sure the fix worked?"* → Monitor the specific signature's crash-free users after the rollout, not the global rate.

---

### 5. A third-party SDK causes crashes on certain devices. How would you isolate the issue and protect the application?

**What the interviewer is really asking**
Can you design a **defensive integration layer** around an untrusted dependency — isolating it, gating it, degrading gracefully, and having a plan to remove it — rather than just filing a bug with the vendor?

**Key concepts**
- Isolation: wrap the SDK behind an adapter/façade interface
- Reflection-based / safe-call invocation; SDK entry-point guarding
- Device/OS gates and feature detection
- Remote config kill switch and per-cohort rollout
- `try/catch` + `LinkageError`/`NoClassDefFoundError`/`UnsatisfiedLinkError` (native crashes)
- R8/ProGuard keep rules for the SDK
- `Dependency Verification` / `Signature Verification` (Play)
- Native crashes: tombstone, `ndk-crash`, symbolication
- SDK version pinning and BOM
- Forking vs replacing vs insulating

**Approach**

1. **Confirm it is really the SDK.** Reproduce on an affected device model. Get the stack: for Java/Kotlin crashes it points straight at the SDK; for **native crashes** you need the tombstone:
   ```bash
   adb shell ls /data/tombstones
   # symbolicate with the SDK's .so symbols
   ```
   Also check `ApplicationExitInfo` (API 30+) to see the reason for the exit (`REASON_CRASH_NATIVE`, `REASON_CRASH`, `REASON_ANR`).

2. **Isolate it behind a façade.** The single most valuable structural change: no app code should call the SDK directly. Define your own interface and put the SDK behind an adapter:
   ```kotlin
   interface Analytics {
       fun track(event: String, params: Map<String, Any?> = emptyMap())
       fun setUser(id: String?)
   }

   class SdkAnalytics(private val sdk: VendorSdk) : Analytics {
       override fun track(event: String, params: Map<String, Any?>) {
           sdk.logEvent(event, params)
       }
       override fun setUser(id: String?) { sdk.identify(id) }
   }
   ```
   Benefits: (a) you can swap or disable it in one place; (b) you can fake it in tests (no SDK in unit tests at all); (c) you can guard it centrally.

3. **Guard the invocation.** Never let an SDK call take down the app:
   ```kotlin
   class SafeAnalytics(
       private val delegate: Analytics?,
       private val enabled: Boolean,
   ) : Analytics {
       override fun track(event: String, params: Map<String, Any?>) {
           if (!enabled || delegate == null) return
           try {
               delegate.track(event, params)
           } catch (t: Throwable) {
               // SDK bug must never crash the app. Report and continue.
               CrashReporter.recordNonFatal("analytics_sdk_failure", t)
           }
       }
   }
   ```
   Catch `Throwable` (not just `Exception`) — SDKs throw `NoClassDefFoundError`, `UnsatisfiedLinkError`, and `ExceptionInInitializerError`, none of which are `Exception`.

4. **Initialize lazily and defensively.** SDKs that crash in `Application.onCreate` are the worst case — they kill the app before any UI exists. Move initialisation to the first real use, and wrap it:
   ```kotlin
   class App : Application() {
       override fun onCreate() {
           super.onCreate()
           // Defer SDK init off the critical startup path AND off the main thread.
           AppScope.launch(Dispatchers.Default) {
               runCatching { VendorSdk.initialize(this@App) }
                   .onFailure { SdkStatus.degraded = true }
           }
       }
   }
   ```
   Also **initialise SDKs after your own startup work** so a crash in the SDK does not cost you the whole cold start (see Section 2, Q12).

5. **Gate by device/OS.** If the crash is confined to a cohort, disable the SDK for it:
   ```kotlin
   val sdkEnabled = remoteConfig.getBoolean("sdk.vendor.enabled")
       && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
       && !isKnownBadDevice(Build.MANUFACTURER, Build.MODEL)
   ```
   A hard-coded device denylist is ugly but is a legitimate short-term mitigation.

6. **Pin and verify the dependency.**
   - Pin the exact SDK version; avoid `+` or dynamic ranges.
   - Use a **BOM** if the vendor publishes one, so transitive versions stay consistent.
   - Enable **Gradle dependency verification** (`gradle/verification-metadata.xml`) so a tampered or unexpectedly-swapped artifact fails the build.
   - Ensure the SDK's R8 rules are applied — many SDK crashes in release builds are actually *missing ProGuard rules* (`NoSuchMethodError`, `ClassNotFoundException`). Check the SDK's consumer rules file; this is a very common "crashes only in release" cause.

7. **Native-crash handling.** If the SDK has native code:
   - Collect `ndk-crash`/tombstone symbolication.
   - Use `androidx.core` `ApplicationExitInfo` to classify the exit reason.
   - Keep the SDK's `.so` out of the critical path; consider `abiFilters` to exclude affected ABIs.
   - You cannot catch a native segfault — the process dies. The only protections are: don't call the crashing API, gate by device, or remove the SDK.

8. **Escalate to the vendor with data.** Give them the cohort (device model, OS version, SDK version), the tombstone, and a minimal reproduction. Set an internal deadline; if they do not fix it in time, plan the replacement.

9. **Have a removal plan.** If the SDK is not core, remove it. If it is core (e.g. a payments SDK), the isolation layer plus a kill switch is your contingency. Keep the interface narrow so a replacement is cheap.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Façade/adapter around the SDK | Single point of control; swappable; testable without it | Extra layer; abstraction may leak SDK specifics |
| `try/catch Throwable` at the boundary | The app survives any SDK bug | Can hide a systemic problem; must report non-fatals or you lose visibility |
| Lazy init off the main thread | Protects cold start and startup | Some SDKs require early init (e.g. crash reporting) — you must still init them, just later/async |
| Device/OS gate | Precise mitigation | Hard-coded device lists rot; user-visible feature differences |
| Remote-config kill switch | Instant, no release | Needs remote-config infrastructure; the feature is lost while disabled |
| Remove the SDK | Cleanest | Migration cost; may lose functionality |
| Fork the SDK | You control the fix | Maintenance burden; you inherit security patches |

**Key detail:** A very common real cause of "third-party SDK crashes only in production" is **missing R8/ProGuard keep rules** — the release build strips or renames classes the SDK reflectively uses, producing `NoSuchMethodError`/`ClassNotFoundException` that never happen in debug. Check the vendor's consumer ProGuard rules and the R8 missing-class report in the build output.

**Code example**

```kotlin
// The SDK is only reachable through our own interface.
interface Analytics {
    fun track(event: String, params: Map<String, Any?> = emptyMap())
}

object AnalyticsModule {
    @Volatile var instance: Analytics = NoOpAnalytics

    fun install(app: Application, config: RemoteConfig) {
        val enabled = config.getBoolean("sdk.vendor.enabled")
        if (!enabled) return
        // Off the main thread; a crash here must not kill startup.
        AppScope.launch(Dispatchers.Default) {
            val sdk = runCatching { VendorSdk.create(app) }.getOrNull() ?: return@launch
            instance = SafeAnalytics(SdkAnalytics(sdk))
        }
    }
}

class SafeAnalytics(private val delegate: Analytics) : Analytics {
    override fun track(event: String, params: Map<String, Any?>) {
        try {
            delegate.track(event, params)
        } catch (t: Throwable) {          // Error, not just Exception
            CrashReporter.recordNonFatal("sdk_failure", t)
        }
    }
}
```

**Talking points / follow-ups**
- *"How do you know it is the SDK and not your own code?"* → Stack trace; native tombstone; `ApplicationExitInfo` reason; reproduce with the SDK removed (feature flag) and observe that the crash disappears — a clean A/B bisection.
- *"What if the SDK is required at startup?"* → You cannot remove the dependency, but you can (a) init it on a background thread, (b) catch `Throwable`, (c) set a watchdog timeout so a hung SDK init does not block the UI thread.
- *"Do you sandbox native crashes?"* → Not really — you can only avoid the code path. You can detect the crash after the fact via `ApplicationExitInfo` and disable the feature on that device.
- *"How do you handle SDK updates that break you?"* → Pin the version, read the release notes, and test the upgrade in CI before bumping. Use a Gradle version catalog to make the pin explicit.
- *"What about SDK data collection and privacy?"* → Review the SDK's data collection; route it through your consent manager so it is disabled when the user declines.

---

### 6. The app occasionally crashes with TransactionTooLargeException. What could cause it, and how would you resolve it?

**What the interviewer is really asking**
Do you understand that this is a **Binder transaction size** problem (the ~1MB `Binder` buffer), not an Intent-size problem per se, and can you reason about which of the several distinct causes applies (saved instance state, Intent extras, `PendingIntent`, `ContentProvider` queries, `Bundle` in `SavedStateHandle`, `RemoteViews`, `Binder` data)?

**Key concepts**
- `TransactionTooLargeException` from `android.os.TransactionTooLargeException`
- Binder buffer limit ~1MB per process (not per transaction, in practice ~1MB shared)
- `onSaveInstanceState` Bundle — the classic cause (a large Bitmap or list saved into the Bundle)
- `Intent` extras across process boundaries (activities, services, `PendingIntent`, broadcast receivers, `ContentProvider` results)
- `SavedStateHandle` / `SavedStateRegistry` writes into the saved-instance-state Bundle
- `rememberSaveable` values going into the Bundle
- `ContentProvider` bulk queries returning huge `Cursor`s
- `RemoteViews` / notification extras (limit ~4KB for some notification paths)
- `Bundle` size limits; `Intent` transaction limit
- Parcelable vs Serializable overhead (Serializable is very inefficient)
- `Bitmap` saved via `Bundle` → `TransactionTooLargeException` because of pixel data

**Approach**

1. **Identify which transaction failed.** The stack trace names it:
   - `android.os.TransactionTooLargeException: data parcel size 1234567 bytes` at `ActivityThread.performLaunchActivity` → Activity launch Intent extras.
   - at `ActivityThread.handleStopActivity` / `onSaveInstanceState` → saved instance state Bundle.
   - at `ActivityManagerService` / `Binder.transact` → a Binder call from a `ContentProvider`, `ActivityManager`, or a system service.

2. **Most common cause: a large object saved into the saved instance state.** Anything put in `onSaveInstanceState` (or `rememberSaveable`, or `SavedStateHandle`) is written into the Bundle, which is parcelled through Binder on the next launch — and Binder has a ~1MB limit.
   ```kotlin
   // BAD — a full-resolution Bitmap into the Bundle = guaranteed TLE
   override fun onSaveInstanceState(outState: Bundle) {
       outState.putParcelable("bitmap", screenBitmap)
   }
   ```
   Fix: do **not** save large payloads. Save only **identifiers** and reload the data:
   ```kotlin
   // GOOD — save the id, re-fetch the payload
   override fun onSaveInstanceState(outState: Bundle) {
       super.onSaveInstanceState(outState)
       outState.putString("screen_id", screenId)
   }
   ```

3. **Avoid `rememberSaveable` for large objects.** `rememberSaveable` puts the value into the same Bundle. A `Bitmap`, a big `List`, or a `Parcelable` holding a byte array is a TLE waiting to happen. `rememberSaveable` should hold **small, primitive-ish state** (scroll position, a text field, a selected id).

4. **Keep `SavedStateHandle` small.** `SavedStateHandle` is backed by the saved instance state Bundle. Storing a large list there is the same problem in a new place — a common modern mistake. Store an id, a query, or a page number.

5. **Trim `Intent` extras.** Anything you put in an `Intent` that crosses a process boundary is parcelled through Binder:
   - Do not pass a `Bitmap`, a list of `Parcelable` objects, or a JSON blob of megabytes.
   - Pass a **URI + id** and let the receiving Activity load the data from a shared source (ContentProvider, Room, or the app process itself if it is the same process).
   - Keep the extras to primitives and small `Parcelable`s.

6. **Watch out for `Serializable`.** Java serialization produces a much larger payload than a compact `Parcelable`/`@Parcelize`. Never `@Parcelize` a huge object graph either — `@Parcelize` is efficient but the *content* is still the problem.

7. **`PendingIntent` and notifications.** `PendingIntent` extras are also Binder-transacted; keep them tiny. Notification `RemoteViews` are limited to a small size (custom views are heavily restricted). Use an image URL + a `WorkManager`/`DownloadManager` job for large content.

8. **`ContentProvider` results.** A `query()` returning thousands of rows crosses Binder in chunks (Cursor windows), but a `call()` returning a huge `Bundle` can blow up. Use `openFile`/`ParcelFileDescriptor` or a paginated query for bulk data instead of returning it in one Binder call.

9. **Reduce the payload structurally.**
   - **Paginate** large lists.
   - **Use a ContentProvider** (`insert`/`query` with a `selection`) rather than a giant Bundle.
   - **Use `bindService`** for a same-app IPC channel where you can stream.
   - For very large blobs, write to a file in the app's private storage and pass the path.
   - For cross-app data, use `FileProvider` with a content URI.

10. **Measure it.** There is a diagnostic to log the transaction size:
    ```bash
    adb shell setprop log.tag.TransactionSize DEBUG
    ```
    (On some builds: `adb shell am set-debug-app` plus `StrictMode`.) `StrictMode` on the main thread also flags oversized binder usage in some cases. Also, the exception message itself includes the byte count.

11. **Prevent it proactively.** Add a lint/checklist rule: "never put a Bitmap, a large list, or a JSON blob into an Intent or saved instance state". Add a debug-only helper that asserts the serialised size of any `Bundle` you put into an Intent.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Pass an id, reload the data | Smallest payload; the correct fix | Extra load on the receiving screen; the data source must be reachable (same process, or a ContentProvider) |
| Put the data in the `Intent` | Simple, self-contained | Binder limit; crashes on large data; wasteful across processes |
| Use `Parcelable`/`@Parcelize` | Compact, typed | Still size-limited; `@Parcelize` on a deep graph can still be huge |
| Use `Serializable` | Trivial to write | Large payload, slow, `NotSerializableException` risk — avoid |
| Use a `ContentProvider` | Handles bulk data; supports paging | More infrastructure; observable contract to maintain |
| Write to a file and pass a path | Handles very large payloads | Cleanup, file-permission management |
| `bindService` | Streamed, no size limit | More complex lifecycle; only for same-app (or bound) services |

**Key detail:** The limit is roughly **1MB of Binder buffer per process**, not per transaction — so several medium transactions can also exhaust it. This is why "it worked until we added another extra" is a real symptom.

**Code example**

```kotlin
// BAD — pass large data across a process boundary
startActivity(Intent(this, DetailActivity::class.java).apply {
    putParcelableArrayListExtra("items", thousandItems)     // ← TransactionTooLargeException
})

// GOOD — pass an identifier, resolve the data on the other side
startActivity(
    Intent(this, DetailActivity::class.java)
        .putExtra(EXTRA_LIST_ID, listId)
        .putExtra(EXTRA_POSITION, index)
)

// The receiving side loads the data itself
class DetailActivity : AppCompatActivity() {
    private val viewModel: DetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val listId = savedInstanceState?.getString(EXTRA_LIST_ID)
            ?: intent.getStringExtra(EXTRA_LIST_ID)
        viewModel.load(listId)      // loaded from Room / ContentProvider, not the Intent
    }
}
```

**Talking points / follow-ups**
- *"What is the actual size limit?"* → ~1MB of Binder buffer per process; the exact usable payload is a bit lower. The exception message reports the parcel size.
- *"Do `Intent` extras have their own limit?"* → Yes, effectively the same Binder buffer; in practice ~1MB for the whole transaction.
- *"How do you find it in the field?"* → The crash reporter's stack trace names the framework method; `TransactionTooLargeException` messages include the byte size; the affected screens are identifiable by their navigation entry points.
- *"Is there a way to test it?"* → Yes — pass a large payload deliberately in a debug build and confirm you hit the limit; or write a unit test that measures the parcelled size of the Bundle you build.
- *"What about `rememberSaveable` and `SavedStateHandle`?"* → Both end up in the saved instance state Bundle; keep them small.

---

### 7. A BroadcastReceiver or listener continues running after the associated screen is destroyed. How would you manage its lifecycle?

**What the interviewer is really asking**
Can you tie **every callback registration to a lifecycle**, and do you know the modern registration APIs and their security implications (`RECEIVER_EXPORTED`/`NOT_EXPORTED`, context-registered vs manifest-registered receivers)?

**Key concepts**
- `BroadcastReceiver`: manifest-declared vs context-registered
- `ContextCompat.registerReceiver` and the `RECEIVER_EXPORTED` / `RECEIVER_NOT_EXPORTED` flags (required on API 34 for context-registered receivers)
- `registerReceiver` / `unregisterReceiver` pairing; `onStart`/`onStop` vs `onCreate`/`onDestroy`
- `repeatOnLifecycle` for Flow collection
- `ContentObserver`, `SensorEventListener`, `LocationListener`, `LifecycleEventListener`, `ViewTreeObserver`
- `removeCallbacks` on `Handler`; `removeObserver`
- `ConnectivityManager.registerNetworkCallback` / `unregisterNetworkCallback`
- Manifest-declared receivers cannot be unregistered — use `goAsync()` for long work and `PendingIntent`
- `LocalBroadcastManager` (deprecated) vs app-internal `Flow`
- `StateFlow`/`SharedFlow` for app-wide events instead of raw listeners

**Approach**

1. **Prefer a Flow over a callback.** In modern Android, most "listeners" should be modelled as a `Flow` collected with `repeatOnLifecycle` — the lifecycle binding is then automatic and impossible to get wrong:
   ```kotlin
   // In the ViewModel: app-level event source, survives rotation.
   val messages: StateFlow<String?> = messageBus.messages

   // In the UI: collection stops when the screen stops, restarts when it resumes.
   LaunchedEffect(Unit) {
       viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
           vm.messages.collect { showSnackbar(it) }
       }
   }
   ```
   `repeatOnLifecycle` cancels the collection at `STOPPED` and restarts it at `STARTED` — no manual unregistering, and no missed critical updates (StateFlow replays the latest).

2. **For a `BroadcastReceiver`, pair register/unregister exactly.** Register in `onStart` and unregister in `onStop` (not `onCreate`/`onDestroy`) so the receiver is not active while the screen is not visible:
   ```kotlin
   class ChatActivity : AppCompatActivity() {
       private val receiver = object : BroadcastReceiver() {
           override fun onReceive(context: Context, intent: Intent) {
               // A receiver fires on the main thread — keep it short.
               handleMessage(intent)
           }
       }

       override fun onStart() {
           super.onStart()
           val filter = IntentFilter(ACTION_MESSAGE)
           ContextCompat.registerReceiver(
               this, receiver, filter,
               ContextCompat.RECEIVER_NOT_EXPORTED,   // API 34+ requires a flag
           )
       }

       override fun onStop() {
           super.onStop()
           unregisterReceiver(receiver)
       }
   }
   ```
   Notes to state in the interview:
   - `RECEIVER_NOT_EXPORTED` is the secure default — other apps cannot send you broadcasts. Use `RECEIVER_EXPORTED` only when you genuinely need cross-app broadcasts, and then validate the sender.
   - On API 34+, `registerReceiver` without a flag throws `SecurityException` for non-system broadcasts.
   - Always unregister in a `try/catch` if registration might have failed, or track a `registered` flag.

3. **Manifest-declared receivers cannot be unregistered.** For those:
   - Keep the `onReceive` body short — it runs on the main thread and the system may kill the process afterwards.
   - Use `goAsync()` for anything longer than a few milliseconds, and call `result.finish()` when done.
   - Do not do network work directly; hand it to `WorkManager`.

4. **Other listener types, same rule:**
   | Listener | Pairing |
   |---|---|
   | `ContentObserver` | `registerContentObserver` / `unregisterContentObserver` |
   | `SensorEventListener` | `registerListener` / `unregisterListener` in `onResume`/`onPause` |
   | `LocationListener` | same |
   | `ConnectivityManager.NetworkCallback` | `registerNetworkCallback` / `unregisterNetworkCallback` |
   | `Handler`/`Runnable` | `removeCallbacksAndMessages(null)` |
   | `ViewTreeObserver` | `removeOnScrollListener` / `removeOnGlobalLayoutListener` |
   | `LifecycleEventObserver` | `removeObserver` |
   | `View.OnAttachStateChangeListener` | `removeOnAttachStateChangeListener` |

5. **Scope the work, not just the registration.** Even if you unregister, a **callback already in flight** (or a coroutine started by the callback) can outlive the screen. Use `viewLifecycleOwner.lifecycleScope` for work triggered by a listener, and `viewModelScope` for work that must survive rotation. Prefer `repeatOnLifecycle(STARTED)` so work stops when the screen is not visible.

6. **For app-wide events, use a shared `Flow` in a process-scoped object**, not a static listener registry. A `MutableSharedFlow`/`StateFlow` in a `@Singleton` has no registration to leak — the *collectors* are lifecycle-bound, which solves the problem structurally.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Flow + `repeatOnLifecycle` | Lifecycle-correct by construction; testable | Requires the producer to expose a Flow; `SharedFlow(replay=0)` can drop events if nobody is collecting |
| `BroadcastReceiver` context-registered | Receives system and app broadcasts | Must pair register/unregister; `RECEIVER_EXPORTED` is a security decision |
| Manifest-declared receiver | Receives broadcasts even when the app is dead | Cannot unregister; main-thread; not appropriate for most UI-driven features |
| `LocalBroadcastManager` | App-internal only | **Deprecated** — use a Flow/`SharedFlow` instead |
| Static listener registry (e.g. `EventBus.get().register()`) | Convenient | The classic leak source; requires manual unregister; discouraged |
| Application-scoped listener with no UI binding | Simple | Leaks by design if it captures an Activity — must capture only `applicationContext` |

**Key detail:** The scope of the *registration* and the scope of the *work* are different problems. Registering in `onStart`/`onStop` fixes the first; `repeatOnLifecycle` + `viewLifecycleOwner.lifecycleScope` fixes the second. Saying both is what separates a complete answer.

**Code example**

```kotlin
// App-wide event source — no registration to leak.
@Singleton
class EventBus @Inject constructor() {
    private val _messages = MutableSharedFlow<Message>(replay = 0, extraBufferCapacity = 32)
    val messages: SharedFlow<Message> = _messages.asSharedFlow()

    fun publish(m: Message) { _messages.tryEmit(m) }
}

// UI — collection is bound to the view lifecycle, no manual teardown.
@Composable
fun ChatScreen(vm: ChatViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, vm) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            vm.messages.collect { msg -> snackbarHostState.showSnackbar(msg.text) }
        }
    }
}

// ViewModel — bridges the app-level bus to the screen
class ChatViewModel(private val bus: EventBus) : ViewModel() {
    val messages: SharedFlow<Message> = bus.messages
}
```

**Talking points / follow-ups**
- *"Why not use a static `EventBus` with `register`/`unregister`?"* → Manual pairing is error-prone and leaks are common; a Flow removes the registration entirely.
- *"What about a receiver registered in `Application` that never unregisters?"* → That is fine **only** if it captures nothing but the Application context. It then lives for the process lifetime by design.
- *"How do you handle a broadcast that arrives while the screen is stopped?"* → Decide deliberately: either ignore it (and let `StateFlow` replay the latest on resume) or buffer it (a `Channel`/`SharedFlow` with a buffer). For UI events, replaying the latest state is usually better than replaying every event.
- *"What does `goAsync()` do?"* → Keeps the process alive for a short window so you can finish work in a background thread; you must call `PendingResult.finish()` within ~10 seconds or the system ANRs you.

---

### 8. The app works for hours but eventually becomes slow and consumes more memory. How would you diagnose the issue?

**What the interviewer is really asking**
Can you distinguish the four classic causes of **long-session degradation** — unbounded collection growth, leak accumulation, thread/coroutine accumulation, and cache growth — and apply the right diagnostic for each?

**Key concepts**
- Heap growth vs. leak (bounded vs unbounded)
- `HeapDump` / `hprof`, Android Studio Memory Profiler "record allocations"
- `dumpsys meminfo <pkg>` — PSS, heap, native, graphics
- Unbounded `MutableList`/`MutableMap`/`StringBuilder` in a long-lived object
- `CoroutineScope` accumulation; `Executor` thread leak; `Timer`/`ScheduledExecutorService`
- `Handler` queue growth (messages piling up)
- LruCache without a size bound; `Glide`/`Coil` memory caches
- `Choreographer`/`doFrame` backlog; `StrictMode`
- `Thread.getAllStackTraces()`, `Debug.getNativeHeapAllocatedSize()`
- `adb shell dumpsys activity processes | grep <pkg>`
- ANR traces

**Approach**

1. **Establish that it is real, and get a shape.** Reproduce: run the app for an extended session (or script the exact user flow) and record:
   - `adb shell dumpsys meminfo <pkg>` at intervals → is PSS climbing linearly? Is it Java heap, native heap, or graphics?
   - Frame stats: `adb shell dumpsys gfxinfo <pkg> framestats` → are frames being dropped later in the session?
   - `adb shell dumpsys activity processes <pkg>` → thread count growing?
   - CPU: `top -H -p <pid>` → is a specific thread spinning?

2. **Classify the growth by category:**

   **(a) Java heap growth → unbounded collection.**
   Take two heap dumps (at minute 5 and minute 45) and compare with the "Compare" feature in Android Studio, or use MAT's histogram diff. The delta shows exactly which class count grew. Look for:
   - a `HashMap`/`ArrayList` in a singleton/ViewModel that is only ever added to,
   - a `StringBuilder` accumulating log text,
   - an unbounded `Flow` buffer,
   - a static `List` of tracked items (e.g. every screen visited in a session).

   **(b) Java heap growth → a leak.** LeakCanary will report retained Activity/Fragment instances if it is the cause. Check instance counts by class in the histogram.

   **(c) Native heap growth → graphics or SDK.** `dumpsys meminfo` breaks out "Graphics" and "Native heap". A steadily growing Graphics figure usually means **bitmaps not being freed** (image caches, or bitmaps held by a leaked view hierarchy). A growing Native heap with no Java growth points at a native SDK or an unbounded native buffer.

   **(d) Thread/coroutine growth.** Thread count climbing → each thread has a stack (≈1MB of address space, real memory on demand). Look for:
   - a `newFixedThreadPool` per operation instead of a shared pool,
   - `Thread { ... }` created per event,
   - `Executors.newCachedThreadPool()` growing unbounded,
   - a `CoroutineScope` per screen that is never cancelled,
   - `ScheduledExecutorService` never shut down.
   ```bash
   adb shell "ls /proc/$(pidof com.example)/task | wc -l"   # thread count
   ```

   **(e) Handler message queue growth.** If the UI slows but memory is flat, the main thread's message queue may be backed up. `dumpsys gfxinfo` and `Choreographer` frame stats show the delay. Look for a `Handler` posting faster than it can drain (e.g. a polling loop posting every frame).

   **(f) CPU/thermal throttling.** After hours, the device is hot and the CPU is throttled → everything is slow, memory may be fine. `adb shell dumpsys thermalservice` shows the thermal status. This is a *device* problem, not an app problem, and it is very common on long sessions. Check battery-saver / data-saver modes too.

   **(g) Cache growth.** `LruCache` misconfigured (size in bytes vs. entries, or no `trimMemory` hook), `Coil`/`Glide` memory cache growing, `WebView` cache, `SQLite` page cache.

3. **Find the retention with tooling.**
   - Android Studio Memory Profiler: **Record allocations** while reproducing → see the allocation stack traces for the growing class. This is the fastest way to find an unbounded collection's writer.
   - `HeapDump` → MAT → "dominator tree" → the object retaining the growing collection.
   - `Allocation tracking` for the specific suspect class.

4. **Fix by category:**
   - **Unbounded collection** → make it bounded (an LRU with a cap), or clear it at a lifecycle boundary. If it is a log buffer, cap it at N entries.
   - **Leak** → see Section 7, Q1.
   - **Thread leak** → use a shared, app-scoped `CoroutineDispatcher`/`Executor`; never create a pool per operation; always `shutdown()`.
   - **Bitmap growth** → set `RecyclerView` `setHasFixedSize`, call `ImageView.setImageDrawable(null)` on rebind, use `onViewRecycled`, and cap the image cache via `onTrimMemory`.
   - **Cache** → implement `ComponentCallbacks2.onTrimMemory` and evict caches on `TRIM_MEMORY_UI_HIDDEN` / `TRIM_MEMORY_RUNNING_LOW`.
   - **Thermal** → reduce background work; schedule heavy work when the device is idle and cool; respect `isPowerSaveMode`.

5. **Instrument for the future.** Add a lightweight in-app watchdog that samples memory (`Debug.getMemoryInfo` / `ActivityManager.MemoryInfo`) and the frame rate periodically, and reports a histogram per session length. Then you can see the regression in the field rather than only in a lab.

6. **Long-session test.** Add a soak test to CI or to the manual QA matrix: run the app for 30–60 minutes with a scripted flow and assert that memory at T+60 is within a threshold of T+15, and that the thread count is stable.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Heap dump analysis | Precise, finds the exact retaining path | Requires expertise and time; a dump of a large app is slow to open |
| Allocation tracking | Shows the writer of the growing objects | High overhead; must be enabled deliberately |
| `dumpsys meminfo` / `gfxinfo` | Cheap, available on any device, good for trends | Coarse — tells you *which* memory, not *who* holds it |
| Bounded caches everywhere | Prevents growth by construction | May evict data the user will need; needs sensible limits |
| `onTrimMemory` handling | Reacts to system pressure | Not called predictably; must be paired with proactive limits |
| Soak testing | Catches the class of bug before users do | Expensive in CI time; needs a device farm |
| Watchdog sampling | Field visibility into the regression | Privacy/noise concerns; must sample, not log everything |

**Key detail:** "Slow *and* using more memory" over hours is most often **either** an unbounded collection in a long-lived object **or** thermal throttling. Check `dumpsys thermalservice` early — it is a two-minute check that rules out half the hypotheses.

**Code example**

```kotlin
// Bound every long-lived collection.
class SessionCache {
    private val entries = object : LinkedHashMap<String, ByteArray>(
        /* initialCapacity = */ 16, /* loadFactor = */ 0.75f, /* accessOrder = */ true
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ByteArray>?) =
            size > MAX_ENTRIES
    }
    companion object { const val MAX_ENTRIES = 50 }
}

// Share one dispatcher instead of creating pools per operation.
@Singleton
class AppDispatchers @Inject constructor() {
    val io = Dispatchers.IO
    val default = Dispatchers.Default.limitedParallelism(4)   // bounded
}

// Handle system memory pressure: evict caches.
class App : Application(), ComponentCallbacks2 {
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when {
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> imageCache.evictAll()
            level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> imageCache.trimTo(percent = 0.25f)
        }
    }
}
```

**Talking points / follow-ups**
- *"How do you know it is a leak vs. a cache?"* → A cache is bounded and has an eviction policy; a leak grows with a user action and never shrinks. Check whether the instance count of the suspect class correlates with a specific action.
- *"What is the difference between Java heap and native heap growth?"* → `dumpsys meminfo` shows both. Bitmap pixel data historically lived in native heap (pre-Android 8, `Bitmap` pixel data was on the native heap); since Android 8 it is in the **graphics** bucket. Both matter for `OutOfMemoryError`.
- *"How would you catch this in CI?"* → A soak test with memory assertions; also `Macrobenchmark` for startup and frames.
- *"Does the OS reclaim memory during the session?"* → Yes — `onTrimMemory` and `onLowMemory` are the signals; if the app ignores them, the system may kill the process instead.
- *"Is the CPU thermally throttled a memory issue?"* → No, but it *presents* as "the app got slow". Always check `dumpsys thermalservice`.

---

### 9. How would you investigate and prevent ANRs caused by database access, locks, or expensive main-thread operations?

**What the interviewer is really asking**
Do you understand the **ANR mechanisms** (input dispatch, broadcast, service, and the 5-second input timeout), how to **capture and read an ANR trace**, and how to **prevent** them by moving work off the main thread — plus how to tell an ANR from a "slow but not deadlocked" app?

**Key concepts**
- ANR types: Input dispatch timeout (~5s), Broadcast timeout (~10s foreground / 60s background), Service timeout, `ContentProvider` acquisition timeout
- ANR trace file: `/data/anr/traces.txt` (and `/data/anr/anr_<pid>_<timestamp>` on newer versions)
- `ApplicationExitInfo` (`REASON_ANR`) — API 30+, gives the ANR trace
- `StrictMode` — `detectDiskReads`, `detectDiskWrites`, `detectNetwork`, `detectCustomSlowCalls`, penalty death
- Main-thread I/O: `DiskReadViolation`, `DiskWriteViolation`
- `Choreographer` frame callbacks; `Looper` message queue stalls
- `BlockCanary` — detects main-thread block > threshold
- Deadlock detection: `jstack`, thread dump, `Debug.dumpHprofData`
- `Handler`/`Looper` blocking, `synchronized` contention, `ReentrantLock` misuse
- `runBlocking` on the main thread — a common and subtle cause
- Room: `allowMainThreadQueries()` — never in production
- StrictMode's `penaltyDeath` in debug builds

**Approach**

1. **Understand the four ANR causes** so you can classify quickly:
   - **Input dispatch timeout** — the main thread did not process input within ~5s. This is the user-visible one.
   - **Broadcast timeout** — `onReceive` exceeded ~10s (foreground) or ~60s (background). Fix with `goAsync()` + a `JobIntentWorker`/`WorkManager`.
   - **Service timeout** — `onStartCommand`/`onBind` exceeded ~20s.
   - **ContentProvider acquisition** — a provider's `onCreate`/`query` blocked.
   Plus the **"executing service"** and **"Input dispatching timed out"** variants seen in the traces.

2. **Capture the ANR trace.** The trace is the single most valuable artefact — it shows the main thread's stack at the moment of the timeout.
   ```bash
   # On a debuggable device (or via the crash reporter's ANR capture)
   adb shell ls /data/anr/
   adb pull /data/anr/anr_1234_1700000000
   ```
   On API 30+, capture it programmatically:
   ```kotlin
   val exits = activityManager.getHistoricalProcessExitReasons(packageName, 0, 10)
   exits.firstOrNull { it.reason == ApplicationExitInfo.REASON_ANR }
       ?.let { File(it.traceInputStream.readBytes().toString(Charsets.UTF_8)) }
   ```
   **How to read it:** find the `"main"` thread block. The stack frame where it is stuck is the bug. Common shapes:
   - `at com.example.dao.TransactionDao_Impl.query(...)` → main-thread DB access
   - `at android.database.sqlite.SQLiteDatabase.lockDatabase` / `at java.lang.Object.wait` → **lock contention** on the SQLite file
   - `at kotlinx.coroutines.BuildersKt.runBlocking` → `runBlocking` on the main thread
   - `at java.net.SocketInputStream.socketRead` → network on the main thread
   - `at android.os.MessageQueue.nativePollOnce` → the main thread is *idle*; the ANR was caused by the **input event never being delivered** (a broken window/animation), a different class of problem.

3. **Reproduce with StrictMode in debug builds** — this catches the cause, not just the symptom:
   ```kotlin
   class App : Application() {
       override fun onCreate() {
           super.onCreate()
           if (BuildConfig.DEBUG) {
               StrictMode.setThreadPolicy(
                   StrictMode.ThreadPolicy.Builder()
                       .detectDiskReads()
                       .detectDiskWrites()
                       .detectNetwork()
                       .detectCustomSlowCalls()
                       .penaltyLog()          // or penaltyDeath() to be strict
                       .build()
               )
               StrictMode.setVmPolicy(
                   StrictMode.VmPolicy.Builder().detectLeakedSqlLiteObjects().build()
               )
           }
       }
   }
   ```
   `penaltyDeath()` in debug is the strongest version — the app dies at the first violation, so it cannot ship.

4. **Fix the main causes:**
   - **Never do I/O on the main thread.** Room DAO methods are `suspend` and already dispatch to the query executor; `allowMainThreadQueries()` must never be enabled outside tests. Use `suspend` DAO functions or `Flow` from Room (which runs on Room's executor and emits on the collector's context).
   - **Never call `runBlocking` on the main thread** — it blocks the thread and defeats cancellation. Use `suspend` functions all the way down.
   - **Move `SharedPreferences` reads off the main thread** — use `DataStore` (suspend `data.first()`), or read once in `Application` on a background thread.
   - **Decrypt and parse on a background dispatcher** — `Cipher` operations, large JSON parsing, and file I/O must be on `Dispatchers.IO`.
   - **Use `StrictMode`'s disk detection** to find leaks of `SQLiteDatabase` and `Cursor` (unclosed cursors keep the DB locked).

5. **Diagnose lock contention (the "locks" part of the question).**
   - SQLite uses a **connection pool with a single writer**. If a long write transaction runs on a background thread while the main thread tries to read, the main thread waits on the SQLite lock → ANR.
   - **Symptoms:** ANRs that appear only under write load, or "database is locked" (SQLITE_BUSY) errors.
   - **Fixes:**
     - Keep write transactions **short** — do the CPU work *outside* the transaction; only the DB writes belong inside.
       ```kotlin
       // BAD: network + JSON parsing inside a transaction
       db.withTransaction {
           val data = api.fetch()        // ← holds the write lock across a network call
           dao.insertAll(data)
       }
       // GOOD
       val data = api.fetch()
       db.withTransaction { dao.insertAll(data) }
       ```
     - Use the **WAL journal mode** (Room enables it by default on modern versions) — WAL allows readers concurrent with a writer.
     - Avoid **multiple Room databases** writing to the same file; and avoid opening a second `SQLiteDatabase` connection to a Room file.
     - Set `busy_timeout` / use `setJournalMode`.
   - **JVM-level locks:** `synchronized` blocks held across I/O, `ReentrantLock` acquired in one place and released in another, a `CountDownLatch.await()` on the main thread. A trace showing the main thread in `Object.wait` inside a `synchronized` monitor identifies this.
   - **Detect with BlockCanary** in debug builds — it samples the main thread and reports stalls over a threshold.

6. **Prevent, and verify.**
   - Add `StrictMode` with `penaltyDeath` to debug builds in CI.
   - Add **ANR detection in the field**: on the next launch, check `ApplicationExitInfo` for `REASON_ANR` and report it with the trace.
   - Add a **Macrobenchmark**/frame-timing check in CI to catch regressions in main-thread work.
   - Add a lint rule for `runBlocking` and for `allowMainThreadQueries` (Room already warns).

7. **Prevent ANRs in the components that cause them.**
   - `BroadcastReceiver.onReceive` → keep under ~10s; use `goAsync()` and hand off to `WorkManager`.
   - `Service.onStartCommand` → return quickly; do work in a coroutine.
   - `ContentProvider.onCreate` → keep trivial; initialise lazily.
   - `Activity` startup → no heavy work on the main thread (see Section 2, Q12).

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| `StrictMode` with `penaltyDeath` in debug | Finds the exact violating call | Crashes debug builds; must be disabled in release |
| `BlockCanary` | Finds main-thread stalls without a crash | Needs a threshold; can be noisy |
| Capturing `ApplicationExitInfo` | Real production ANR traces | Only available API 30+; the trace is captured by the system, so it may be truncated |
| WAL journal mode | Readers do not block on a writer | Slightly more disk usage; needs checkpointing |
| Short write transactions | Removes lock contention | Requires discipline; cannot hold a transaction across a UI flow |
| Moving all I/O to `Dispatchers.IO` | Prevents most ANRs | Requires restructuring blocking APIs into `suspend` functions |
| `runBlocking` in a non-UI layer | Simple bridging | Dangerous if it ever runs on the main thread; a common ANR source |

**Key detail:** The most instructive interview insight is that an ANR trace showing the main thread in `nativePollOnce` (idle) is a **different problem** from one showing it in `SQLiteDatabase.lockDatabase` — the former is a broken UI/animation or an unresponsive window, the latter is a genuine blocking bug. Always read the main thread's stack, not just the ANR header.

**Code example**

```kotlin
// Keep the write transaction short — no I/O inside it.
suspend fun ingest(items: List<Item>) {
    val prepared = withContext(Dispatchers.IO) {              // CPU + I/O outside the lock
        mapper.map(items)
    }
    db.withTransaction {                                       // short transaction only
        itemDao.upsertAll(prepared)
        metaDao.setLastIngest(now())
    }
}

// Never block the main thread.
suspend fun loadProfile(): Profile = withContext(Dispatchers.IO) {
    profileDao.getById(id)                                   // Room suspend DAO → IO executor
}

// StrictMode in debug builds
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads().detectDiskWrites()
                    .detectNetwork().detectCustomSlowCalls()
                    .penaltyDeath()
                    .build()
            )
        }
    }
}
```

**Talking points / follow-ups**
- *"What is the ANR threshold?"* → ~5s for input dispatch, ~10s for a foreground broadcast, ~20s for a service, ~60s for a background broadcast, and ~10s for a ContentProvider acquisition. The exact values vary by OEM and load.
- *"Is an ANR always a crash?"* → No — the user sees the "app isn't responding" dialog; the process may be killed or may survive. It is a serious UX failure but not necessarily a crash.
- *"How do you tell a DB-lock ANR from a slow-animation ANR?"* → The main thread's stack: `lockDatabase`/`Object.wait` vs `nativePollOnce`/`Choreographer`.
- *"Does `StrictMode` catch all main-thread work?"* → It catches disk reads/writes, network, and custom slow calls — not arbitrary CPU work. For CPU, use method tracing or `Systrace`.
- *"What is the difference between a jank and an ANR?"* → Jank is a frame that took longer than the deadline (~16.7ms at 60Hz); an ANR is the main thread unresponsive for seconds. Jank is a performance problem; an ANR is a liveness failure.
- *"How do you prevent ANRs in a library you do not control?"* → Wrap the call in a background dispatcher with a timeout and degrade gracefully; report it to the vendor.

---

## Section Summary — Staff-Level Themes

| Theme | Questions |
|---|---|
| **Retaining paths and lifetimes** | 1, 2, 7 — the Activity/Context leak family; lifetime mismatch as the root cause |
| **Measurement before hypothesis** | 1, 3, 4, 8, 9 — heap dumps, `dumpsys meminfo`/`gfxinfo`/`thermalservice`, ANR traces, `ApplicationExitInfo` |
| **Field diagnosis without reproduction** | 3, 4, 5 — cohort slicing, breadcrumbs, remote-config bisection |
| **Defensive boundaries** | 2, 5 — façades, `catch (Throwable)`, lazy init, kill switches |
| **Platform limits** | 6 — the ~1MB Binder budget and its many entry points |
| **Lifecycle correctness** | 7 — `repeatOnLifecycle` over manual register/unregister |

**The one sentence to memorise for this section:**
> *"Stability bugs are diagnosed, not guessed: get the artefact — a heap dump, an ANR trace, a tombstone, a cohort — find the exact retaining path or blocked stack frame, fix the lifetime mismatch that caused it, and then verify with a deterministic reproduction so it cannot come back."*
