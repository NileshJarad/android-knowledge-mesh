# Offline-First Architecture and Database

> Answers written from the perspective of a **Staff Android Engineer**. Each answer is structured as:
> *What the interviewer is really asking* → *Key concepts* → *Approach* → *Trade-offs* → *Code* → *Talking points*.

---

### 1. An app must display transaction history immediately, even when the network is unavailable. How would you design the data flow?

**What the interviewer is really asking**
Do you understand **unidirectional data flow with the local database as the single source of truth**, and can you explain *why* the UI must never await the network for display data? A junior answer is "cache the API response in memory". A staff answer is "the database is the only thing the UI observes; the network is a background writer into that database".

**Key concepts**
- Local DB as the single source of truth (SSOT); UI observes Room `Flow`, never the network directly
- `Network → Repository → Room → Flow → UI` — one direction only
- Room `Flow` return type: re-emits automatically on table invalidation
- Stale-while-revalidate: show cached data immediately, refresh in the background
- Bounded cache + TTL/eviction policy so "millions of rows" never become a storage problem
- Freshness metadata (`lastSyncedAt`) surfaced in the UI so staleness is honest, not hidden

**Approach**

1. **Make Room the only read path.** The UI never calls Retrofit directly — not even "for freshness". Define the repository contract so that reads are pure DB reads:
   ```kotlin
   interface TransactionRepository {
       fun observeTransactions(filter: TxFilter): Flow<List<Transaction>>   // DB-backed
       suspend fun refresh(): SyncOutcome                                  // network → DB
   }
   ```
   The ViewModel collects the `Flow`; `refresh()` is a separate, fire-and-forget action. If the ViewModel has a `suspend fun load()` that awaits the network before setting state, the design is already wrong.

2. **Seed the first read.** On a cold install the DB is empty, so "immediate display" still needs a source. Options, in order of preference:
   - **Bundled seed data** shipped as an asset/JSON, imported once via `RoomDatabase.Callback.onCreate` or a pre-populated DB asset.
   - **Last-known snapshot** in DataStore/file, copied into Room at first launch.
   - Otherwise show a first-run empty state — do **not** block the UI on the network.

3. **Use a `Flow`-returning DAO query.** Room re-runs the query and re-emits when the underlying table is invalidated:
   ```kotlin
   @Query("SELECT * FROM transactions WHERE accountId = :id ORDER BY occurredAt DESC LIMIT :limit")
   fun observeForAccount(accountId: String, limit: Int): Flow<List<Transaction>>
   ```
   Note: the query runs on Room's query executor, never the main thread. The `Flow` is *cold* — it re-executes on each new collector and stays live while subscribed. Combine with `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), …)` so the DB query is not running while no UI is watching.

4. **Push network results into the DB, never into the UI state.** The network layer's only job is to *mutate the database*. When the transaction arrives, the `Flow` invalidation makes the UI recompose automatically:
   ```kotlin
   suspend fun refresh(): SyncOutcome = try {
       val remote = api.getTransactions(since = metaDao.lastSyncCursor())
       db.withTransaction {
           remote.items.forEach { txDao().upsert(it.toEntity()) }   // Room 2.5+ @Upsert
           metaDao().setSyncCursor(remote.nextCursor)
       }
       SyncOutcome.Success(remote.items.size)
   } catch (e: IOException) {
       SyncOutcome.Offline           // DB still holds the last good data
   }
   ```

5. **Model staleness explicitly.** Persist `lastSyncedAt` and expose it. The UI can then show "Updated 5 min ago" instead of pretending data is live. This is a trust mechanism: users forgive stale data when it is labelled, and lose trust when it is silently wrong.

6. **Bound the cache.** "Transaction history" is unbounded in principle. Store a rolling window (e.g. last 12 months / last N rows) plus a separate on-demand "load older" path that pages from the server and inserts into the DB. Add a scheduled cleanup job that prunes rows beyond the retention window and rows already synced.

7. **Handle the "first launch while offline" case honestly.** If there is no bundled seed and no cache, the app shows a first-run state explaining that history requires a connection, and the app still functions for anything it can do offline. Never block the whole app on a sync.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| DB as SSOT, UI observes Room only | One code path online and offline; testable; no UI/network race | Writes are amplified (every remote change is a DB write); a sync bug shows stale data everywhere |
| Network → UI directly (anti-pattern) | Always fresh, less code | No offline support; loading spinners on every cold start; race conditions between network and local state |
| In-memory cache only | Fast, trivial | Lost on process death; no query ability; no pagination; cannot be the SSOT |
| Bundled seed data | App is useful on first launch with no network | Bundle size; stale before first sync; needs a versioned seed migration path |
| Unbounded local history | Simplest UX | Storage blow-up; slow queries; violates the user's expectation of a bounded device footprint |
| Bounded rolling window + "load older" | Predictable storage and query cost | Extra UX for old data; needs a clear "load more" affordance and paging |
| Show stale data with a banner | Honest, preserves trust | Slightly worse "instantly correct" feel; requires freshness plumbing everywhere |

**Key detail:** Room `Flow` queries are *invalidated by table changes* — Room's `InvalidationTracker` re-runs the query and pushes new results to active collectors. This is what makes "the DB is the SSOT" work without any manual refresh calls in the UI layer.

**Code example**

```kotlin
@Entity(tableName = "transactions", indices = [Index("accountId", "occurredAt")])
data class TransactionEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val amountCents: Long,
    val currency: String,
    val occurredAt: Long,
    val memo: String?,
    val isDeleted: Boolean = false,   // tombstone, never hard-delete
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND isDeleted = 0 ORDER BY occurredAt DESC")
    fun observeForAccount(accountId: String): Flow<List<TransactionEntity>>

    @Upsert                                   // Room 2.5+: insert-or-update in one statement
    suspend fun upsertAll(items: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE occurredAt < :cutoff AND isDeleted = 0")
    suspend fun pruneOlderThan(cutoff: Long): Int
}

class TransactionRepository(
    private val dao: TransactionDao,
    private val metaDao: SyncMetaDao,
    private val api: TransactionApi,
    private val db: AppDatabase,
) {
    // UI path: pure DB read, emits again whenever the table changes.
    fun observe(accountId: String): Flow<List<Transaction>> =
        dao.observeForAccount(accountId).map { list -> list.map { it.toDomain() } }

    // Network path: writes into the DB; the Flow pushes it to the UI.
    suspend fun refresh(): SyncOutcome = try {
        val remote = api.getTransactions(since = metaDao.lastSyncCursor())
        db.withTransaction {
            dao.upsertAll(remote.items.map { it.toEntity() })
            metaDao.setSyncCursor(remote.nextCursor)
            metaDao.setLastSyncedAt(System.currentTimeMillis())
        }
        SyncOutcome.Success(remote.items.size)
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        SyncOutcome.Offline
    }
}
```

**Talking points / follow-ups**
- *"Why not just cache the Retrofit response in a `StateFlow`?"* → Because an in-memory cache dies with the process and cannot be queried, paginated, or joined with other local data. It also makes the "current" value ambiguous when two sources race.
- *"Where does `lastSyncedAt` live?"* → In Room (a `sync_meta` table), not in DataStore, so that it is updated **in the same transaction** as the data it describes. A cursor stored outside the DB can drift out of sync with the data.
- *"How do you know the Flow re-emits?"* → Room's `InvalidationTracker` observes the table; after an `@Upsert` the invalidation tracker notifies observers, the `Flow` re-runs the query and emits. You can observe it directly with `db.invalidationTracker.addObserver(InvalidationTracker.Observer("transactions") { … })`.
- *"What if the network returns 50 records but the DB already has 500?"* → That is what the sync cursor is for. Delta sync means the server only sends what changed since the cursor.
- *"How do you test this?"* → Fake the API to throw; assert the `Flow` still emits the previously seeded rows and the UI shows the stale banner.

---

### 2. The same record is modified locally and on the server while the device is offline. How would you resolve the conflict?

**What the interviewer is really asking**
This is the **conflict-resolution** question. The interviewer wants to know whether you default to *last-write-wins* by reflex or whether you can reason about which fields are actually in conflict, whether you can *detect* the conflict (optimistic concurrency), and how you surface it to the user rather than silently discarding someone's work.

**Key concepts**
- Optimistic concurrency with a server-assigned `version`; client sends `baseVersion`; server returns `409 Conflict`
- Last-write-wins (LWW) — and exactly when it is acceptable
- Three-way merge (base / local / remote) at field granularity
- Tombstones (`isDeleted`) so a delete is not resurrected by a stale pull
- `updatedAt` (server clock, monotonic) vs `version` (monotonic counter) — prefer `version`
- Conflict UX: silent merge, "keep mine / keep theirs", or a merged preview
- The outbox: the local edit must be retained until the server accepts it

**Approach**

1. **Detect the conflict before you overwrite anything.** Every synced row carries a server-assigned `version` (an integer the server increments on every accepted write, or a monotonic ULID/ETag). The client stores the `version` it last saw. When pushing, it sends `baseVersion`:
   ```json
   PATCH /v1/transactions/{id}
   If-Match: "v7"        // or {"baseVersion": 7, ...}
   ```
   If the server's current version is still 7, it accepts and bumps to 8. If it is 8, the server rejects with `409` and returns the **current server state** so the client can merge. This converts silent data loss into a detectable, recoverable event — and it is the right default.

2. **Do not use client timestamps for conflict detection.** "Last write wins by `updatedAt`" is wrong whenever clocks disagree: device clocks drift, the user can change the device clock, and the *offline* edit necessarily has an *older* timestamp than the server change. Timestamps are fine for *display ordering*, never for correctness.

3. **Prefer LWW only for genuinely commutative fields.** For a transaction record, "category" edited on the server and "memo" edited on the device are **not** in conflict — a blind whole-record LWW would drop the memo. Field-level merge (a three-way merge using the last-synced base row) resolves it cleanly:
   ```kotlin
   fun merge(base: Tx, local: Tx, remote: Tx): Tx = base.copy(
       amountCents = if (local.amountCents == base.amountCents) remote.amountCents else local.amountCents,
       memo       = if (local.memo       == base.memo)       remote.memo       else local.memo,
       categoryId = if (local.categoryId == base.categoryId) remote.categoryId else local.categoryId,
   )
   ```
   This is the classic three-way merge: for each field, if only one side changed it, take that side; if both changed it to different values, that *is* a true conflict and must be surfaced.

4. **True conflicts: surface them, do not silently pick.** If the same field was changed on both sides, keep the local edit in the outbox, store the server version in a `conflicts` table, and let the user resolve. The auto-resolution policy can be "prefer the later `updatedAt`" *for display only*, while the local edit stays queued until the user or a server-side rule resolves it.

5. **Never lose the local edit.** The local change lives in two places: the entity row (so the UI shows it immediately) and the **outbox** row (so it survives process death and is retried). When the push gets a `409`, mark the outbox row `CONFLICT`, store the server payload alongside it, and rebase. The outbox row is deleted only when the server accepts.

6. **Handle deletes as tombstones.** A local delete sets `isDeleted = true` locally and enqueues a delete in the outbox. On pull, a remote tombstone for a row we have locally must not be resurrected by an older local copy. Tombstones need a retention window (e.g. keep for 30 days) before they can be hard-deleted, otherwise an offline device that syncs after 30 days will resurrect deleted rows.

7. **Decide the policy per field type, not per entity.** A good staff answer: *"I would not pick one strategy globally. Money amounts are last-write-wins with a hard conflict prompt, because silently overwriting a wrong amount is unacceptable. Memo text is a three-way merge, because both sides are usually meaningful. Presence flags (isRead) are commutative LWW, because merging is meaningless."*

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Blind last-write-wins (client clock) | Simplest, no conflict UX | Silent data loss; wrong whenever clocks drift; an offline edit almost always loses |
| Blind last-write-wins (server clock) | Deterministic ordering | Still loses data; an offline edit made later in wall-clock terms may be older server-side |
| Optimistic concurrency (`version` / `If-Match`) | Detects every conflict; no silent loss | Needs conflict UX; can produce repeated 409 loops if the client does not rebase |
| Field-level three-way merge | Preserves independent edits; feels "correct" | Needs the base row stored locally; true conflicts still need a decision |
| Server-side merge endpoint | Conflicts resolved authoritatively | Extra backend surface; still needs a client-side conflict UI for UX |
| CRDT / vector clock | No conflicts, multi-device safe | Large implementation cost; wrong tool for record-shaped data |
| Silently prefer server on conflict | Never overwrites server truth | Discards the user's offline work — the classic bug |

**Key detail:** `version` must be **server-assigned and monotonic**. A client-generated `updatedAt` or a client-side UUID cannot express "how many times this row changed", so it cannot support `If-Match` semantics. Also, the conflict check must be a compare-and-set in the *same* database transaction on the server, otherwise two concurrent pushes can both pass the check.

**Code example**

```kotlin
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val amountCents: Long,
    val categoryId: String,
    val memo: String?,
    val version: Long,            // server-assigned, monotonic
    val updatedAt: Long,           // server clock, display only
    val isDeleted: Boolean = false,
)

@Entity(tableName = "outbox")
data class OutboxRow(
    @PrimaryKey val id: String,            // idempotency key
    val entityId: String,
    val payload: String,
    val baseVersion: Long,                 // version the edit was made against
    val state: String,                     // PENDING | CONFLICT | DONE
    val serverPayload: String?,            // populated on 409
    val createdAt: Long,
)

class ConflictResolver {
    /** Three-way merge: take a side only if the other side did not change that field. */
    fun merge(base: TransactionEntity, local: TransactionEntity, remote: TransactionEntity): MergeResult =
        MergeResult(
            amountCents = pick(local.amountCents, base.amountCents, remote.amountCents),
            categoryId  = pick(local.categoryId,  base.categoryId,  remote.categoryId),
            memo        = pick(local.memo,        base.memo,        remote.memo),
        )

    private fun <T> pick(local: T, base: T, remote: T): T = when {
        local == remote -> local
        local == base   -> remote      // only the remote side changed it
        remote == base  -> local       // only the local side changed it
        else            -> throw TrueConflictException()   // both changed differently
    }
}

class PushWorker(...) : CoroutineWorker(...) {
    override suspend fun doWork(): Result {
        val row = db.outboxDao().nextPending() ?: return Result.success()
        return try {
            val resp = api.patchTransaction(
                id = row.entityId,
                baseVersion = row.baseVersion,          // If-Match semantics
                payload = row.payload,
                idempotencyKey = row.id,                 // same key on every retry
            )
            db.withTransaction {
                db.transactionDao().applyServerVersion(row.entityId, resp.version, resp.entity)
                db.outboxDao().markDone(row.id)
            }
            Result.success()
        } catch (e: ConflictException) {
            // Keep the local edit; rebase onto the server state and re-queue.
            db.withTransaction {
                db.outboxDao().markConflict(row.id, e.serverPayload)
                db.conflictDao().insert(row.entityId, e.serverPayload)
            }
            Result.success()   // not a transient failure — do not hammer the server
        } catch (e: IOException) {
            Result.retry()
        }
    }
}
```

**Talking points / follow-ups**
- *"What is the difference between `updatedAt` and `version`?"* → `updatedAt` is a timestamp (useful for display and rough ordering, but affected by clock skew); `version` is an opaque server-assigned token that changes on every accepted write and supports compare-and-set.
- *"How do you handle the case where the server does not support versions?"* → Fall back to a three-way merge against the local base row, but you lose the ability to *detect* a conflict — you can only detect field-level divergence heuristically. Be honest that this is strictly weaker.
- *"What if the user edits the same record twice offline?"* → Coalesce the outbox rows for the same entity — keep one pending row with the merged payload and the *original* `baseVersion`; changing the base version would break the compare-and-set.
- *"How do you test conflicts?"* → Fake the server to return `409` with a known server state; assert the merge result and that the local edit is still queued.
- *"Is LWW ever acceptable?"* → Yes, for commutative fields and for "last state of a presence flag" — but make it a *per-field* decision, not a global one.

---

### 3. A Room database migration fails for users upgrading from an older app version. How would you investigate and recover?

**What the interviewer is really asking**
This is a **production-crash** question. The interviewer wants to see if you understand that a failed Room migration is a *crash loop* on upgrade (the app cannot even open the DB), and that the answer is a combination of *prevention* (tested migrations with exported schemas) and *recovery* (a safe fallback that does not destroy user data).

**Key concepts**
- `Migration(n, m) { db -> execSQL(...) }`; `MigrationTestHelper` with `room.schemaLocation`
- `exportSchema = true` + the `room.schemaLocation` Gradle argument → schemas versioned in `schemas/`
- `AutoMigration` (`@AutoMigration(from = 1, to = 2)`) for simple additive changes
- `fallbackToDestructiveMigration()` — **destroys user data**; a last-resort dev tool only
- `IllegalStateException: Migration didn't properly handle: ...` — the exact failure message
- `RoomDatabase.Callback` / `onOpen` for post-migration data fixups
- Crash telemetry: keep a DB version + schema hash in crash reports
- Ship-the-migration-before-the-code rule: never change the entity model without shipping the migration in the *same or an earlier* release

**Approach**

1. **Understand the failure mode.** Room throws `IllegalStateException("Migration didn't properly handle: (table, column) ...")` from `RoomOpenHelper` when, after running the migration, the `PRAGMA table_info` check finds a column the new entity expects but the DB does not have. The DB is opened on a background thread but the exception surfaces as a crash at first access. The app then **crash-loops** on every launch until the user reinstalls or the OS clears data.

2. **Reproduce it exactly.** The whole point of `exportSchema = true` is that you can replay the real upgrade path in a test:
   ```kotlin
   @Test fun migrate_1_to_2() {
       val helper = MigrationTestHelper(
           InstrumentationRegistry.getInstrumentation(),
           AppDatabase::class.java,
           emptyList(),            // the auto-migrations under test
           FrameworkSQLiteOpenHelperFactory(),
       )
       helper.createDatabase(TEST_DB, 1).apply {
           execSQL("INSERT INTO transactions (id, amountCents) VALUES ('a', 100)")
           close()
       }
       val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, Migration_1_2())
       // runMigrationsAndValidate validates the resulting schema against the exported JSON for v2
   }
   ```
   For this you must configure the Gradle `room.schemaLocation` argument so the schema JSONs are exported:
   ```groovy
   android {
       defaultConfig {
           javaCompileOptions {
               annotationProcessorOptions {
                   arguments += ["room.schemaLocation": "$projectDir/schemas".toString()]
               }
           }
       }
   }
   ```
   (`ksp { arg("room.schemaLocation", "$projectDir/schemas") }` for KSP.) Without exported schemas, `MigrationTestHelper` cannot validate, and you have no record of what v1 actually looked like.

3. **Read the crash and diff the schemas.** Take the exported `1.json` and `2.json` and diff them — the table/column delta tells you exactly which statements the migration must contain. `Room` validates the *result*, so the fix is usually "the migration did not create/alter/rename this column".

4. **Fix the migration properly — do not paper over it with destructive fallback.** A typical missed case:
   ```kotlin
   val MIGRATION_1_2 = object : Migration(1, 2) {
       override fun migrate(db: SupportSQLiteDatabase) {
           db.execSQL("ALTER TABLE transactions ADD COLUMN categoryId TEXT NOT NULL DEFAULT ''")
           db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_accountId_occurredAt ON transactions(accountId, occurredAt)")
           db.execSQL("CREATE TABLE IF NOT EXISTS sync_meta (id INTEGER PRIMARY KEY, lastSyncCursor TEXT, lastSyncedAt INTEGER)")
       }
   }
   ```
   Common migration mistakes to look for:
   - Adding a **NOT NULL column without a DEFAULT** → fails on existing rows. Add with a default, then (on a later migration) rebuild the table to drop the default.
   - Adding an **index** but forgetting `IF NOT EXISTS` → fails on re-run.
   - **Renaming a column** → `ALTER TABLE ... RENAME COLUMN` requires SQLite 3.25+ (Android 10 / API 29+). For older devices use the create-copy-drop-rename dance, or `AutoMigration` (Room 2.4+ handles it by table rebuild).
   - **Changing a column type** → SQLite is dynamically typed, so `ALTER TABLE ... ALTER COLUMN` is not supported; you must create a new table, copy, drop, rename.
   - **Adding an entity** without creating its table.

5. **Use `AutoMigration` where it applies.** For purely additive changes (new table, new column with default, new index), Room 2.4+ can generate the migration:
   ```kotlin
   @Database(version = 3, autoMigrations = [AutoMigration(from = 2, to = 3)])
   abstract class AppDatabase : RoomDatabase() { … }
   ```
   It still needs the schema JSONs exported and should be covered by `MigrationTestHelper`. Use hand-written `Migration` for anything that moves or transforms data.

6. **Recovery for users who are already stuck.** If you have already shipped a broken migration, the fix must ship as a **new release with a higher version** — you cannot fix version 1→2 in place for users who already failed it (their DB is at v1 and they will retry the same broken migration). Options:
   - Ship `Migration(1, 2)` corrected in a hotfix — works for users still on v1 who haven't crashed away their data.
   - For users whose DB is already corrupted or in a half-migrated state, provide a **recovery path**: catch the exception at the `Room.databaseBuilder` call site and rebuild from scratch *only* with explicit user consent and a server-side re-sync:
     ```kotlin
     val db = try {
         Room.databaseBuilder(context, AppDatabase::class.java, "app.db")
             .addMigrations(*Migrations.ALL)
             .build()
     } catch (e: IllegalStateException) {
         // Migration failed: offer "reset and re-download" rather than crash-looping.
         context.deleteDatabase("app.db")
         null
     }
     ```
   - **Never** ship `fallbackToDestructiveMigration()` in production. It silently drops the user's local data, which for an offline-first app means losing anything not yet synced. It is acceptable only in debug builds.

7. **Prevent recurrence with process and CI.**
   - Enforce `exportSchema = true` and commit `schemas/` to version control; CI fails if the schema JSON is not regenerated (diff check).
   - `MigrationTestHelper` tests for **every** version pair, plus a "migrate from the oldest supported version" test — it is possible to have a valid 2→3 and 3→4 but an invalid 1→3 path.
   - A "latest version" DB test: open the newest schema, insert a realistic row set, then run all migrations and compare data.
   - In production crash reporting, attach the DB version (`RoomDatabase.getOpenHelper().readableDatabase.version`) and the `room_master_table` identity hash to every crash so you can see which users are on which version.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Hand-written `Migration` | Full control; can transform data | Every version pair needs a test; easy to miss an index or a default |
| `AutoMigration` | Less code for additive changes | Cannot handle type changes or data transforms; still needs schema export + tests |
| `fallbackToDestructiveMigration()` | Never crash-loops | **Destroys all local data** — unacceptable in a production offline-first app |
| Delete DB and rebuild on failure | Simple recovery path | Loses local-only data; must offer a user-visible re-sync flow |
| Ship a hotfix with a higher version | Users get the fix | Users already on the broken version need a data path; larger release overhead |
| Exported schemas + `MigrationTestHelper` | Catches the bug before release | Requires CI discipline and a schema directory committed to git |
| One migration per version | Straightforward | Long upgrade chains; each hop is a chance to lose data |

**Key detail:** `runMigrationsAndValidate` in `MigrationTestHelper` validates the **resulting schema** against the exported JSON for the target version. That is the test that would have caught the bug — it is not enough to assert the migration SQL "ran without exception", because the common failure is a *missing* statement, not a *bad* one.

**Code example**

```kotlin
object Migrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // NOT NULL columns need a DEFAULT for existing rows.
            db.execSQL("ALTER TABLE transactions ADD COLUMN categoryId TEXT NOT NULL DEFAULT ''")
            // Indices are part of the schema Room validates.
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_accountId_occurredAt ON transactions(accountId, occurredAt)")
            db.execSQL("CREATE TABLE IF NOT EXISTS sync_meta (id INTEGER NOT NULL PRIMARY KEY, lastSyncCursor TEXT, lastSyncedAt INTEGER)")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Type change: SQLite cannot ALTER COLUMN → rebuild the table.
            db.execSQL("CREATE TABLE transactions_new (id TEXT NOT NULL, amountCents INTEGER NOT NULL, categoryId TEXT NOT NULL, memo TEXT, version INTEGER NOT NULL, updatedAt INTEGER NOT NULL, isDeleted INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(id))")
            db.execSQL("INSERT INTO transactions_new (id, amountCents, categoryId, memo, version, updatedAt, isDeleted) SELECT id, amountCents, categoryId, memo, 0, updatedAt, 0 FROM transactions")
            db.execSQL("DROP TABLE transactions")
            db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_accountId_occurredAt ON transactions(accountId, occurredAt)")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
}

fun buildDatabase(context: Context): AppDatabase =
    Room.databaseBuilder(context, AppDatabase::class.java, "app.db")
        .addMigrations(*Migrations.ALL)
        // NO fallbackToDestructiveMigration() in production.
        .addCallback(object : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                db.execSQL("PRAGMA foreign_keys = ON")
            }
        })
        .build()
```

**Talking points / follow-ups**
- *"Where do the exported schemas live and are they committed?"* → `$projectDir/schemas/<db-class>/<version>.json`, committed to git; CI regenerates and fails on an uncommitted diff.
- *"Why is `fallbackToDestructiveMigration` dangerous in an offline-first app?"* → Because the local DB *is* the product's data model — it holds unsynced user edits. Destroying it destroys work the user has not yet pushed to the server.
- *"What is the difference between a crash and a silent failure?"* → A failed migration throws and crash-loops. But a migration that "succeeds" while leaving the data in the wrong shape is worse — hence schema *validation*, not just execution.
- *"How do you migrate on a device whose SQLite version is old?"* → `ALTER TABLE RENAME COLUMN` needs SQLite 3.25 (API 29+). For a `minSdk` below that, use the create-copy-drop pattern or `AutoMigration`'s table rebuild, which generates the right SQL for the device.
- *"How do you know how many users are affected?"* → Report `RoomDatabase.version` (and the `room_master_table` identity hash) in crash reports and analytics; the DB version tells you exactly which migration path failed.

---

### 4. A database contains millions of records. How would you optimize queries, indexes, pagination, and storage?

**What the interviewer is really asking**
Can you reason about **SQLite's query planner**, not just "add an index"? The interviewer wants to see `EXPLAIN QUERY PLAN`, understanding of covering indexes and `WITHOUT ROWID`, awareness that **offset pagination degrades as O(n)**, and the difference between *query* optimisation and *storage* optimisation.

**Key concepts**
- `EXPLAIN QUERY PLAN` — read it before optimising; look for `SCAN` vs `SEARCH ... USING INDEX`
- Composite index column order: equality columns first, then range/sort columns
- Covering index (all selected columns in the index) → `USING COVERING INDEX`
- `WITHOUT ROWID` tables for wide composite primary keys
- FTS5/FTS4 for search vs `LIKE 'prefix%'` (full scan) vs `LIKE '%x%'`
- `COLLATE NOCASE` for case-insensitive matching
- Paging 3: `PagingSource`, `PagingConfig(pageSize, maxSize, prefetchDistance, jumpThreshold)`
- Keyset / seek pagination (`WHERE (createdAt, id) < (?, ?)`) vs `LIMIT ? OFFSET ?`
- `Partial`/`covering` index, `ANALYZE` / `PRAGMA optimize`

**Approach**

1. **Measure before you optimise.** Open the DB in a copy on-device (pull it via `adb`, or use `sqlite3` locally) and run `EXPLAIN QUERY PLAN` on the actual hot queries:
   ```sql
   EXPLAIN QUERY PLAN
   SELECT id, amountCents, occurredAt FROM transactions
   WHERE accountId = 'acc_1' AND occurredAt >= 1700000000000
   ORDER BY occurredAt DESC LIMIT 50;
   ```
   - `SEARCH transactions USING INDEX index_... (accountId=? AND occurredAt<?)` → good.
   - `SCAN transactions` → full table scan; the query cannot use an index.
   - `USE TEMP B-TREE FOR ORDER BY` → SQLite had to sort in memory because the index did not provide the order; that is the classic sign of a missing composite index.
   Also run `PRAGMA optimize;` / `ANALYZE;` so SQLite has real statistics for the planner.

2. **Design composite indices deliberately.** The rule of thumb: **equality predicates first, then the range/sort column, then covering columns.**
   ```kotlin
   @Entity(
       tableName = "transactions",
       indices = [
           Index(value = ["accountId", "occurredAt"]),
           Index(value = ["categoryId"]),
       ],
   )
   data class TransactionEntity(…)
   ```
   For the query above, `(accountId, occurredAt)` lets SQLite seek directly and satisfy the `ORDER BY occurredAt DESC` from the index — no temp B-tree.

3. **Make the index covering when the query is hot and the row is wide.** If the list screen only needs `(id, amountCents, occurredAt)`, put those in the index so SQLite can answer from the index alone (`USING COVERING INDEX`) without touching the table — this is the single biggest win for long list queries over big tables:
   ```kotlin
   @Entity(indices = [Index(value = ["accountId", "occurredAt", "amountCents", "memo"])])
   ```
   The trade-off: a covering index is larger, so writes get slower and the DB grows. Do it for a handful of genuinely hot queries, not for everything.

4. **Do not use offset pagination.** `LIMIT 50 OFFSET 100000` makes SQLite walk and discard 100,000 rows — O(offset) per page, and it gets worse the deeper the user scrolls. Use **keyset (seek) pagination**: carry the last seen sort key forward.
   ```sql
   SELECT * FROM transactions
   WHERE accountId = ? AND (occurredAt, id) < (?, ?)     -- tuple comparison, SQLite 3.15+
   ORDER BY occurredAt DESC, id DESC
   LIMIT 50;
   ```
   This is O(log n) per page. Note the tie-breaker column (`id`) — without it, rows with identical `occurredAt` are skipped or repeated across pages. SQLite supports row-value comparisons since 3.15 (Android API 21+ ships SQLite 3.8, so on very old devices express it as `(occurredAt < ? OR (occurredAt = ? AND id < ?))`).

5. **Use Paging 3 for the UI layer.** `PagingSource` implements the keyset query; `PagingConfig` controls the memory window:
   ```kotlin
   class TransactionPagingSource(private val db: AppDatabase, private val accountId: String) :
       PagingSource<String, TransactionEntity>() {
       override suspend fun load(params: LoadParams<String>): LoadResult<String, TransactionEntity> { … }
       override fun getRefreshKey(state: PagingState<String, TransactionEntity>): String? =
           state.anchorPosition?.let { state.getKey(it)?.nextKey }
   }
   ```
   `PagingConfig(pageSize = 50, maxSize = 300, prefetchDistance = 10, jumpThreshold = 20)` — `maxSize` bounds how many rows are held in memory; `prefetchDistance` triggers the next page before the user reaches the end; `jumpThreshold` enables "jump to item" by position (which forces a scroll-to-item operation). If the list is also synced from the network, add a `RemoteMediator` that refreshes from the API and serves pages from Room.

6. **Store the right physical layout.** For a table with a wide composite primary key (e.g. `(accountId, occurredAt, id)`), `WITHOUT ROWID` stores the rows directly in the index B-tree, avoiding a second lookup and shrinking the file. Room supports this via `@Entity` + a raw `WITHOUT ROWID` table only with `@RawQuery`/custom creation — in practice, most teams accept Room's default rowid tables and focus on indices.

7. **Reduce storage.**
   - **Prune.** A retention policy job deleting rows older than N months keeps the table bounded; run it in `WorkManager` with `ExistingWorkPolicy.KEEP`.
   - **Don't store blobs in rows.** Large payloads (receipt images, JSON blobs) go to the filesystem, and the row keeps a path. Store `Long` cents, not `Double` amounts; store ISO timestamps as `Long` epoch millis, not strings.
   - **`WITHOUT ROWID`** and `PRAGMA page_size`/`VACUUM` for compaction after a big prune.
   - **Avoid `TEXT` for hot filters** — an `INTEGER` category id beats a string for index size and comparison speed.

8. **Search: use FTS, not `LIKE`.** `WHERE memo LIKE '%coffee%'` cannot use an index and scans the whole table. FTS5 gives tokenised search plus ranking:
   ```kotlin
   @Database(entities = [TransactionEntity::class, TransactionFts::class], version = 4)
   …
   @Query("SELECT t.* FROM transactions t JOIN transactions_fts f ON f.rowid = t.rowid WHERE transactions_fts MATCH :query")
   fun search(query: String): Flow<List<TransactionEntity>>
   ```
   Note: `LIKE 'prefix%'` *can* use an index (it is a range scan on the prefix), but `LIKE '%x%'` cannot. `COLLATE NOCASE` on the column or in the index makes case-insensitive prefix matching index-backed.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| `LIMIT/OFFSET` pagination | Trivial to implement | O(offset) per page — unusable at depth; rows shift under concurrent writes |
| Keyset / seek pagination | O(log n) per page; stable under inserts | Requires a deterministic sort with a tie-breaker; cannot "jump to page 42" |
| Offset + Paging 3 `jumpThreshold` | Supports jump-to-item | Requires an accurate count query; jump is expensive |
| Composite index `(eq, range)` | Fast filtered + sorted queries | More write cost, more disk; a badly ordered composite index is useless |
| Covering index | Avoids table lookups entirely | Larger index; slower writes; must be maintained per hot query |
| FTS5 | Real search with ranking | Extra table to keep in sync (triggers or app-side writes); heavier schema |
| `LIKE '%x%'` | One-liner | **Full table scan** — O(n) per keystroke |
| Prune old rows | Bounded table size | Loses history; needs retention policy + a "load from server" path |
| Store blobs on disk | Small rows, fast queries | File lifecycle management, orphan cleanup, more IO on read |

**Key detail:** `EXPLAIN QUERY PLAN` is the single most useful command in this area — `"USE TEMP B-TREE FOR ORDER BY"` is the giveaway that a composite index is missing, and `"USING COVERING INDEX"` tells you the query is fully served from the index. Also remember SQLite's `ANALYZE` statistics: without them the planner can pick a bad plan.

**Code example**

```kotlin
@Dao
interface TransactionDao {
    // Keyset pagination: no OFFSET, so cost is constant per page.
    @Query("""
        SELECT id, amountCents, occurredAt FROM transactions
        WHERE accountId = :accountId
          AND (occurredAt < :lastOccurredAt OR (occurredAt = :lastOccurredAt AND id < :lastId))
        ORDER BY occurredAt DESC, id DESC
        LIMIT :limit
    """)
    suspend fun pageAfter(
        accountId: String,
        lastOccurredAt: Long,
        lastId: String,
        limit: Int,
    ): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :accountId")
    suspend fun countFor(accountId: String): Int
}

@OptIn(ExperimentalPagingApi::class)
class TransactionPagingSource(
    private val dao: TransactionDao,
    private val accountId: String,
) : PagingSource<Long, TransactionEntity>() {

    override suspend fun load(params: LoadParams<Long>): LoadResult<Long, TransactionEntity> = try {
        val page = params.key?.let { offsetKey ->
            // offsetKey encodes the last (occurredAt, id) of the previous page.
            dao.pageAfter(accountId, offsetKey.first, offsetKey.second, params.loadSize)
        } ?: dao.pageAfter(accountId, Long.MAX_VALUE, "", params.loadSize)

        LoadResult.Page(
            data = page,
            prevKey = null,
            nextKey = if (page.size == params.loadSize) page.last().let { it.occurredAt to it.id } else null,
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        LoadResult.Error(e)
    }

    override fun getRefreshKey(state: PagingState<Long, TransactionEntity>): Long? =
        state.anchorPosition?.let { state.getKey(it) }
}
```

**Talking points / follow-ups**
- *"Why did your list get slow only for some accounts?"* → The query plan depends on the *selectivity* of the predicates; the planner may pick a scan for a small table and an index seek for a large one. Per-account data skew plus a missing composite index is the classic cause.
- *"How do you measure on device?"* → `androidx.sqlite.db.SwipeSQLite`/`SQLiteDatabase` query plans via `EXPLAIN QUERY PLAN` on a debug build, `StrictMode`, and `androidx.tracing` slices around the query. Room's `@Query` can also be logged with a `QueryCallback` in the `RoomDatabase.Builder`.
- *"What about `PRAGMA foreign_keys`?"* → Off by default in SQLite; Room enables it per-connection, but raw `SupportSQLiteDatabase` access needs `db.setForeignKeyConstraintsEnabled(true)`.
- *"Do you ever use `OFFSET`?"* → Only for shallow admin/debug screens where the total row count is small and known.
- *"How do you test a keyset pager?"* → Insert rows while paging and assert no duplicates and no skipped rows across pages.

---

### 5. The app receives duplicate synchronization events. How would you ensure that processing them is idempotent?

**What the interviewer is really asking**
This is the **idempotency-of-the-consumer** question, the mirror image of the server-side idempotency key in Section 1. The interviewer wants to see that you treat a sync event as a *message that may be delivered more than once*, and that you make the *effect* idempotent rather than trying to prevent the duplicate.

**Key concepts**
- At-least-once delivery; WorkManager gives at-least-once, not exactly-once
- Idempotent writes: `@Upsert` (Room 2.5+), `@Insert(onConflict = OnConflictStrategy.IGNORE)`, unique indices
- Event identity: a server-assigned monotonic `eventId` / sequence number / cursor
- Dedupe table or a `processed_events` guard, or "last applied cursor" watermark
- Idempotency key on the *push* side so a retry does not create a duplicate server-side row
- Idempotent operations: absolute state (set to X) rather than relative deltas (increment by 1)

**Approach**

1. **Give every sync event a stable identity.** The server assigns each change a monotonic `seq` (or a ULID) and returns it in the delta. The client persists the highest applied `seq`. Any event with `seq <= lastAppliedSeq` is skipped. This is the simplest and most effective dedupe — a **watermark**, not a per-event table:
   ```kotlin
   suspend fun applyDelta(delta: Delta) = db.withTransaction {
       val last = metaDao.lastAppliedSeq()
       delta.changes.filter { it.seq > last }.forEach { txDao().upsert(it.toEntity()) }
       delta.tombstones.filter { it.seq > last }.forEach { txDao().softDelete(it) }
       metaDao.setLastAppliedSeq(delta.maxSeq)
   }
   ```
   The filter and the watermark update happen **in the same transaction**, so a crash mid-apply replays safely.

2. **Make the write itself idempotent.** Even with a watermark, an event can arrive twice (two devices, a retried request, a duplicate push). Use `@Upsert` (Room 2.5+) keyed on the primary key, not `@Insert`:
   ```kotlin
   @Upsert
   suspend fun upsert(item: TransactionEntity)
   ```
   `@Upsert` issues `INSERT ... ON CONFLICT DO UPDATE`, so applying the same event twice yields the same row. For `@Insert`, use `OnConflictStrategy.IGNORE` (keeps the existing row) or `REPLACE` (overwrites it — note `REPLACE` is a delete + insert, so it **resets unspecified columns to defaults**; do not use it on a partial row).

3. **Enforce uniqueness in the schema, not just in code.** A unique index makes the invariant hold even if a code path forgets to check:
   ```kotlin
   @Entity(indices = [Index(value = ["clientRequestId"], unique = true)])
   data class TransactionEntity(…)
   ```
   Then `INSERT OR IGNORE` semantics come free, and a duplicate event cannot create a second row.

4. **Prefer absolute over relative semantics.** `incrementBalance(10)` applied twice is 20; `setBalance(current)` applied twice is the same. Design the *event payload* to be a full state or an absolute value, so replaying it is harmless. If you must send deltas, send them with a unique event id and dedupe in a `processed_events` table.

5. **Push side: the same key, always.** Each queued local change carries a client-generated idempotency key that is **persisted with the outbox row** and reused on every retry. If the server keys on it, a duplicated push is collapsed:
   ```kotlin
   api.push(payload = row.payload, idempotencyKey = row.id)   // row.id is stable
   ```
   Never mint a new key per attempt — that is exactly how duplicates get created.

6. **Deduping WorkManager, not the data.** WorkManager itself may run the same sync twice (retry after a crash, `REPLACE` policy, expedited + periodic). Use `enqueueUniqueWork("sync", ExistingWorkPolicy.KEEP, request)` so only one sync runs at a time. That prevents *concurrent* duplicates; the upsert + watermark handles the *sequential* duplicates.

7. **Prove idempotency in tests.** A dedicated test: apply the same delta twice, assert the row count and the content are identical and that `lastAppliedSeq` is correct. Apply deltas out of order, assert the watermark logic still converges.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Watermark (`lastAppliedSeq`) | O(1) state; simplest; handles reordering | Relies on a monotonic server sequence; a reset of the server sequence breaks it |
| `processed_events` dedupe table | Per-event exactness; survives sequence resets | Unbounded growth (needs pruning); a write per event |
| `@Upsert` on the primary key | Idempotent by construction; no extra state | Overwrites local-only edits if the server copy is stale — must be paired with version checks |
| `@Insert(onConflict = IGNORE)` | Never duplicates rows | Silently drops the *update* half of an event — wrong for mutable entities |
| `@Insert(onConflict = REPLACE)` | Full overwrite | Delete + insert semantics — resets columns not present in the new row |
| Unique index on a business key | Invariant enforced by the DB | A business-key collision aborts the transaction; needs careful key choice |
| `enqueueUniqueWork` with `KEEP` | Prevents concurrent duplicate work | Does not prevent a sequential re-run after a crash — that is what the upsert covers |
| Relative delta events | Compact payloads | **Not idempotent**; doubles on replay unless deduped explicitly |

**Key detail:** `ExistingWorkPolicy.KEEP` prevents *concurrent* runs of the same unique work chain; `REPLACE` cancels the running one and starts a new one (which can cause the "cancel mid-flight" unknown-outcome problem from Section 1); `APPEND` chains new work after the existing chain. Use `KEEP` for sync, and rely on data-level idempotency for the crash-retry case.

**Code example**

```kotlin
@Entity(
    tableName = "transactions",
    indices = [Index(value = ["accountId", "occurredAt"]), Index(value = ["clientRequestId"], unique = true)],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val clientRequestId: String?,
    val seq: Long,                      // server-assigned monotonic change sequence
    val version: Long,
    val amountCents: Long,
    val occurredAt: Long,
    val isDeleted: Boolean = false,
)

@Dao
interface TransactionDao {
    @Upsert                                   // Room 2.5+: INSERT ... ON CONFLICT DO UPDATE
    suspend fun upsert(item: TransactionEntity)

    @Query("SELECT MAX(seq) FROM transactions")
    suspend fun maxSeq(): Long?
}

@Dao
interface SyncMetaDao {
    @Query("SELECT value FROM sync_meta WHERE key = 'lastAppliedSeq'")
    suspend fun lastAppliedSeq(): Long

    @Query("INSERT OR REPLACE INTO sync_meta (key, value) VALUES ('lastAppliedSeq', :seq)")
    suspend fun setLastAppliedSeq(seq: Long)
}

class DeltaApplier(private val db: AppDatabase) {
    suspend fun apply(delta: Delta) = db.withTransaction {
        val watermark = db.syncMetaDao().lastAppliedSeq()

        // 1. Skip anything already applied — order-insensitive duplicate handling.
        val fresh = delta.changes.filter { it.seq > watermark }
        fresh.forEach { db.transactionDao().upsert(it.toEntity()) }

        // 2. Tombstones applied the same way, so a delete is never resurrected.
        delta.tombstones.filter { it.seq > watermark }.forEach {
            db.transactionDao().softDelete(it.id, it.seq)
        }

        // 3. Advance the watermark in the SAME transaction as the data.
        db.syncMetaDao().setLastAppliedSeq(maxOf(watermark, delta.maxSeq))
    }
}
```

**Talking points / follow-ups**
- *"How do you even get a duplicate event?"* → WorkManager retry after a crash mid-apply, a retried HTTP pull, a duplicate push from another device, a server fan-out to two tabs of the same account, or an at-least-once message bus.
- *"Does `@Upsert` fire a `Flow` emission even when nothing changed?"* → Room's invalidation tracker fires on the write, so the `Flow` re-emits. If that is a problem, compare the incoming row against the stored one inside the transaction and skip the write if identical — but you pay the comparison cost.
- *"What if the server cannot give you a monotonic sequence?"* → Fall back to a per-event `eventId` stored in a `processed_events` table with a TTL/pruning policy, or use the entity `version` + a conditional write.
- *"How does this relate to idempotency keys on the server?"* → Same idea, opposite direction: the server dedupes pushes by our key; the client dedupes pulls by the server's sequence. Both are needed.

---

### 6. Local data is updated successfully, but the subsequent server synchronization fails. How would you recover?

**What the interviewer is really asking**
This is the **local-write-succeeded / push-failed** gap — the classic "ghost write". The interviewer wants to see that you understand the local DB and the server are now **out of sync**, that you need a **durable outbox** to bridge the gap, and that the UI must not present the local state as if it were confirmed.

**Key concepts**
- Outbox pattern: local write + queued mutation in one transaction
- `sync_state` table: PENDING / IN_FLIGHT / CONFLICT / DONE per mutation
- Retry with exponential backoff; `Result.retry()` lets WorkManager own the backoff
- `baseVersion` for optimistic concurrency on retry
- Idempotency key on the push so retries are safe
- Optimistic UI with a pending/syncing indicator; `lastSyncedAt` for freshness
- "Never lose the user's edit" — the local row stays until the server accepts it

**Approach**

1. **Make the local write and the queue entry atomic.** This is the crux. If you write the entity and *then* enqueue the outbox row in two separate transactions, a crash in between loses the mutation forever. Both must be in one Room transaction:
   ```kotlin
   @Transaction
   suspend fun applyLocalEdit(tx: Transaction): Long {
       txDao().upsert(tx.toEntity())
       outboxDao().insert(OutboxRow(
           id = UUID.randomUUID().toString(),      // idempotency key, stable across retries
           entityId = tx.id,
           payload = tx.toJson(),
           baseVersion = tx.version,
           state = "PENDING",
           createdAt = now(),
       ))
       return 1
   }
   ```
   For a multi-DAO operation use `db.withTransaction { … }` (room-ktx), which gives you one real SQLite transaction across DAOs.

2. **Never tell the user the write succeeded until the server has accepted it.** The UI shows the local state plus a `syncState` badge ("Pending sync" / "Syncing" / "Synced" / "Conflict"). The row carries the sync state so the UI can render per-item status:
   ```kotlin
   @Entity(tableName = "transactions")
   data class TransactionEntity(
       …,
       val syncState: String,   // SYNCED | PENDING | CONFLICT | FAILED
   )
   ```

3. **Let WorkManager own the retry loop.** The sync worker drains the outbox; on a transient failure it returns `Result.retry()` and WorkManager applies the configured exponential backoff (set it explicitly with `setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)` — the default is 30s with a 5h cap). Do not build your own `while` retry loop inside the worker; you will fight the scheduler.

4. **Push with a stable idempotency key and the original `baseVersion`.** Both are already in the outbox row. If the server processed the request but the response was lost, the retry with the same key returns the original outcome instead of creating a duplicate (Section 1, Q1). Do **not** re-read the local row to build the payload on each attempt — if the user edited the record again in the meantime, you would send a newer payload under an older key, which is a correctness bug. **Send exactly what was queued.**

5. **Distinguish transient from permanent failures.**
   - `IOException`, `408`, `429`, `5xx` → transient → `Result.retry()`.
   - `401` → token issue → refresh, then retry (do not burn the outbox row).
   - `403`/`422` validation → permanent → mark the row `FAILED` with the reason and surface it to the user; retrying forever is pointless.
   - `409` conflict → mark `CONFLICT` and rebase (see Q2) — this is not a retryable failure.

6. **Bound the retry horizon and escalate.** After N attempts (e.g. 5) or T hours, mark the row `FAILED`, surface a banner ("3 changes could not be synced — tap to retry"), and keep the local data. The user's edit is never discarded because the network is down. Also cap the outbox size: if it grows beyond a limit (e.g. 10k rows), the user is not going to sync — surface that explicitly.

7. **Handle the "server accepted, we never heard" case.** Because the push is idempotent, the retry is safe. But there is a subtler variant: the server accepted, applied a new `version`, and the client never updated its `version`. The next push would then carry a stale `baseVersion` and get a `409` — which the conflict path handles. Make the server's idempotent replay return the **current** version so the client can reconcile without a conflict.

8. **Trigger the sync.** After a local write, enqueue an expedited one-time sync if the network is available (`setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)`), plus a periodic job as a safety net. Use `enqueueUniqueWork("outbox-sync", ExistingWorkPolicy.KEEP, …)` so the write-triggered sync and the periodic one never run concurrently.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Local write + outbox in one transaction | No lost mutations; durable across process death | Extra write per mutation; outbox needs pruning |
| Write locally, push later, no outbox | Simple | **Silent data loss** on failure — the edit is never sent and nobody notices |
| Optimistic UI with a pending badge | Fast, honest | Requires per-row sync state and a UI for it |
| Optimistic UI with no badge | Simplest UI | Users believe failed edits are saved — a trust bug |
| `Result.retry()` + WorkManager backoff | Leverages the scheduler; survives process death | Minimum delay granularity; requires correct failure classification |
| Custom in-worker retry loop | Full control | Fights the scheduler; burns battery; can run for a long time |
| Send the queued payload, not the live row | Correct under concurrent edits | Requires serialising edits per entity or a snapshot payload |
| Drop the outbox row on permanent failure | Keeps the table small | **Loses user work** — always keep the local data, escalate instead |

**Key detail:** The outbox row's `payload` must be a **snapshot taken at edit time**. If you re-serialise the entity on each retry, a second edit while offline would be sent under the first edit's idempotency key — the server would collapse them and the second edit would be silently dropped.

**Code example**

```kotlin
@Entity(tableName = "outbox")
data class OutboxRow(
    @PrimaryKey val id: String,             // idempotency key — stable across retries
    val entityId: String,
    val payload: String,                   // SNAPSHOT at edit time, not re-read later
    val baseVersion: Long,
    val state: String,                     // PENDING | FAILED | CONFLICT | DONE
    val attempts: Int = 0,
    val lastError: String? = null,
    val createdAt: Long,
)

@Dao
interface OutboxDao {
    @Query("SELECT * FROM outbox WHERE state = 'PENDING' ORDER BY createdAt LIMIT :limit")
    suspend fun nextBatch(limit: Int): List<OutboxRow>

    @Query("UPDATE outbox SET attempts = attempts + 1, lastError = :err WHERE id = :id")
    suspend fun recordFailure(id: String, err: String)

    @Query("DELETE FROM outbox WHERE id = :id")
    suspend fun delete(id: String)
}

class OutboxSyncWorker(
    context: Context,
    params: WorkerParameters,
    private val db: AppDatabase,
    private val api: SyncApi,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val batch = db.outboxDao().nextBatch(50)
        if (batch.isEmpty()) return Result.success()

        for (row in batch) {
            try {
                val resp = api.pushTransaction(
                    entityId = row.entityId,
                    payload = row.payload,        // exact snapshot that was queued
                    baseVersion = row.baseVersion,
                    idempotencyKey = row.id,       // same key on every retry
                )
                db.withTransaction {
                    db.transactionDao().applyServerVersion(row.entityId, resp.version)
                    db.outboxDao().delete(row.id)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ConflictException) {
                db.outboxDao().markConflict(row.id, e.serverPayload)   // not retryable
            } catch (e: HttpException) {
                db.outboxDao().recordFailure(row.id, e.code().toString())
                return if (e.code() >= 500 || e.code() == 429) Result.retry() else Result.failure()
            } catch (e: IOException) {
                db.outboxDao().recordFailure(row.id, e.message.orEmpty())
                return Result.retry()      // WorkManager applies exponential backoff
            }
        }
        return Result.success()
    }
}
```

**Talking points / follow-ups**
- *"Why is the outbox better than a flag on the entity row?"* → A flag cannot carry the payload snapshot or the idempotency key, and it cannot represent multiple queued edits to the same entity.
- *"How does the user know something failed?"* → Per-row sync state plus an aggregate banner. Silent failure is the worst outcome.
- *"What if the device is offline for a month?"* → The outbox grows; cap it, and consider coalescing edits to the same entity (keep the newest payload, keep the oldest `baseVersion`).
- *"How do you test this?"* → Fake the API to fail with a 500; assert the row stays `PENDING`, `attempts` increments, the local data is intact, and a later success drains it.
- *"Should the write wait for the network?"* → No, unless the business requires it (e.g. a money movement that must be confirmed). Optimistic-with-badge is the default.

---

### 7. How would you implement a reliable synchronization queue that survives app restarts?

**What the interviewer is really asking**
Can you build a **durable queue on top of Room + WorkManager** and articulate the guarantees it gives (at-least-once, ordered per entity, durable) and the ones it does not (exactly-once, cross-entity ordering)? The interviewer wants to see the queue *schema*, the *claiming* mechanism, and the *failure* handling.

**Key concepts**
- Outbox table as the durable queue; `state` transitions PENDING → IN_FLIGHT → DONE/FAILED
- Claiming a batch atomically (`UPDATE ... SET state='IN_FLIGHT' WHERE id IN (...)`) so two workers never process the same row
- `enqueueUniqueWork` + `ExistingWorkPolicy.KEEP` for one sync at a time
- `setBackoffCriteria`, `Result.retry()`, `Constraints(NetworkType.CONNECTED)`
- Reclaiming stale `IN_FLIGHT` rows after a crash (a lease/heartbeat)
- Per-entity ordering (sequence numbers) vs global ordering
- `PeriodicWorkRequest` (15-minute minimum) as a safety net; expedited one-time work for user-visible freshness

**Approach**

1. **The queue is a Room table, not memory.** Each row is one pending mutation with everything needed to execute it: entity id, payload snapshot, `baseVersion`, idempotency key, `createdAt`, `attempts`, `state`, and a monotonically increasing `seq` for ordering.
   ```kotlin
   @Entity(tableName = "sync_queue", indices = [Index(value = ["state", "seq"])])
   data class SyncQueueRow(
       @PrimaryKey val id: String,
       val entityId: String,
       val payload: String,
       val baseVersion: Long,
       val seq: Long,                 // AUTOINCREMENT ordering
       val state: String,             // PENDING | IN_FLIGHT | DONE | FAILED
       val attempts: Int = 0,
       val leaseUntil: Long = 0,      // for reclaiming crashed workers
       val lastError: String? = null,
   )
   ```

2. **Ordering guarantees.** Use an `AUTOINCREMENT` `seq` (or a `Long` from an atomic counter) to process mutations in the order the user made them. Preserve **per-entity** ordering strictly: two edits to the same record must be pushed in order, or the second will be rejected as a conflict against a stale base version. Across different entities, ordering does not matter and you can parallelise.

3. **Claim atomically before pushing.** If two workers race, both could pick the same row. Claim with a conditional UPDATE inside a transaction:
   ```kotlin
   @Transaction
   suspend fun claimBatch(limit: Int, now: Long): List<SyncQueueRow> {
       val candidates = queryPending(limit)                       // ordered by seq
       val ids = candidates.map { it.id }
       db.query("UPDATE sync_queue SET state='IN_FLIGHT', leaseUntil=:until WHERE id IN (:ids)", ids, now + LEASE_MS)
       return queryByIds(ids)
   }
   ```
   The `UPDATE … WHERE state='PENDING'` predicate makes the claim a compare-and-set: a row already claimed by another worker is not re-claimed.

4. **Reclaim leases after a crash.** If the process dies mid-push, rows are stuck in `IN_FLIGHT`. Use a **lease**: `leaseUntil = now + 2 min`; a reaper query treats `IN_FLIGHT AND leaseUntil < now` as `PENDING` again. Because the push is idempotent (same key), the retry is safe.

5. **Drain the queue in the worker.** `WorkManager` provides the durability: it persists the work request, survives process death and reboots, and re-runs it. The worker:
   ```kotlin
   override suspend fun doWork(): Result {
       val claimed = dao.claimBatch(50, System.currentTimeMillis())
       if (claimed.isEmpty()) return Result.success()
       var failed = false
       for (row in claimed) {
           try {
               val resp = api.push(row.payload, row.baseVersion, row.id)   // idempotencyKey = row.id
               db.withTransaction {
                   db.entityDao().applyServerVersion(row.entityId, resp.version)
                   dao.complete(row.id)
               }
           } catch (e: CancellationException) { throw e }
           catch (e: TransientException) { dao.fail(row.id, e.message); failed = true }
           catch (e: PermanentException) { dao.markFailed(row.id, e.message) }   // keep for user action
       }
       return if (failed) Result.retry() else Result.success()
   }
   ```
   Because a failure anywhere in the batch returns `Result.retry()`, the whole batch is retried later — which is why every push must be idempotent.

6. **Schedule it correctly.**
   ```kotlin
   val request = OneTimeWorkRequestBuilder<SyncQueueWorker>()
       .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
       .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
       .build()
   WorkManager.getInstance(ctx).enqueueUniqueWork("sync-queue", ExistingWorkPolicy.KEEP, request)
   ```
   `ExistingWorkPolicy.KEEP` means: if a sync is already running, keep it and skip enqueuing a new one. Use `REPLACE` only when you *want* to cancel the running sync (dangerous — see Section 1, Q12). Use `APPEND_OR_REPLACE` to chain a follow-up run after the current one finishes.

7. **Add a periodic safety net.** `PeriodicWorkRequestBuilder<SyncQueueWorker>(15, TimeUnit.MINUTES)` with `NetworkType.CONNECTED` catches anything the event-driven triggers missed. 15 minutes is the *minimum* period — WorkManager will not run it more often, and in Doze it may be delayed further. So the periodic job is a backstop, not the primary path.

8. **Backpressure and caps.** Cap the queue length (e.g. 5,000 rows). Past that, coalesce: for the same entity, keep only the newest payload but the *oldest* `baseVersion`, and delete the intermediate rows. Also cap the batch size per run to avoid a huge `Result.retry()` loop that starves the app.

9. **Observability.** Log the queue depth, the age of the oldest pending row, and the failure count. "Oldest pending row age > 1 hour" is the metric that tells you sync is broken before users complain.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Room table as the queue | Durable, transactional with the data write, queryable | Requires schema, pruning, and migration discipline |
| In-memory queue + `SharedPreferences` | Simple | Lost on process death; not transactional with the entity write |
| `enqueueUniqueWork` + `KEEP` | Exactly one sync at a time; no duplicate work | A request enqueued while one is running is *dropped* — ensure the running sync drains the queue fully, or use `APPEND` |
| `APPEND` for follow-up runs | Guarantees a second pass after the current one | Can build a long chain of work |
| `REPLACE` | Guarantees the newest data is pushed | Cancels an in-flight push → unknown outcome; only safe with idempotency keys |
| Atomic claiming with a conditional UPDATE | No two workers process the same row | More complex than a plain SELECT |
| Lease + reaper | Recovers rows orphaned by a crash | Needs a clock and a reaper query; a long push can exceed the lease (make the lease generous or extend it) |
| Periodic backstop | Catches missed triggers | 15-minute minimum; delayed in Doze; not user-visible freshness |
| Coalescing per entity | Keeps the queue bounded | Can lose an intermediate intent — only safe when the newer payload subsumes the older |

**Key detail:** `ExistingWorkPolicy.KEEP` *drops* the new request if one is already running. That is usually correct, but it means the enqueuing code must ensure the running sync will pick up the newly queued rows — which it will, because it drains the table, not the request payload. If you instead put the work items *inside* the request (e.g. `Data` or a list of ids), `KEEP` would lose them. This is a real, common bug.

**Code example**

```kotlin
@Entity(tableName = "sync_queue", indices = [Index(value = ["state", "seq"])])
data class SyncQueueRow(
    @PrimaryKey val id: String,
    val entityId: String,
    val payload: String,
    val baseVersion: Long,
    val seq: Long = 0L,
    val state: String = "PENDING",
    val attempts: Int = 0,
    val leaseUntil: Long = 0L,
    val lastError: String? = null,
)

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue WHERE state = 'PENDING' ORDER BY seq LIMIT :limit")
    suspend fun pending(limit: Int): List<SyncQueueRow>

    @Query("UPDATE sync_queue SET state = 'IN_FLIGHT', leaseUntil = :until WHERE id IN (:ids) AND state = 'PENDING'")
    suspend fun claim(ids: List<String>, until: Long): Int      // compare-and-set

    @Query("UPDATE sync_queue SET state = 'PENDING', leaseUntil = 0 WHERE state = 'IN_FLIGHT' AND leaseUntil < :now")
    suspend fun reclaimExpired(now: Long): Int                  // crash recovery

    @Query("UPDATE sync_queue SET state = 'DONE' WHERE id = :id")
    suspend fun complete(id: String)

    @Query("UPDATE sync_queue SET state = 'PENDING', attempts = attempts + 1, lastError = :err WHERE id = :id")
    suspend fun requeue(id: String, err: String?)

    @Query("SELECT COUNT(*) FROM sync_queue WHERE state IN ('PENDING','IN_FLIGHT')")
    suspend fun depth(): Int
}

class SyncQueueWorker(
    context: Context,
    params: WorkerParameters,
    private val dao: SyncQueueDao,
    private val api: SyncApi,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        dao.reclaimExpired(System.currentTimeMillis())          // recover orphans
        val candidates = dao.pending(50)
        if (candidates.isEmpty()) return Result.success()

        val ids = candidates.map { it.id }
        val claimed = dao.claim(ids, System.currentTimeMillis() + LEASE_MS)
        if (claimed == 0) return Result.success()                // someone else won the race

        var transient = false
        for (row in candidates.take(claimed)) {
            try {
                val resp = api.push(row.entityId, row.payload, row.baseVersion, row.id)
                dao.complete(row.id)
                entityDao.applyServerVersion(row.entityId, resp.version)
            } catch (e: CancellationException) {
                throw e
            } catch (e: TransientException) {
                dao.requeue(row.id, e.message); transient = true
            } catch (e: PermanentException) {
                dao.markFailed(row.id, e.message)                  // escalate, do not retry forever
            }
        }
        return if (transient) Result.retry() else Result.success()
    }

    companion object {
        const val LEASE_MS = 2 * 60 * 1000L
    }
}

// Enqueue: one sync at a time; the running sync drains the table, so nothing is lost.
fun scheduleSync(context: Context) {
    val request = OneTimeWorkRequestBuilder<SyncQueueWorker>()
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
        .build()
    WorkManager.getInstance(context)
        .enqueueUniqueWork("sync-queue", ExistingWorkPolicy.KEEP, request)
}
```

**Talking points / follow-ups**
- *"What guarantees does this give?"* → At-least-once execution, durable across restarts, per-entity ordering. It does **not** give exactly-once — the pushes are idempotent, which is the practical substitute.
- *"Why not just use WorkManager's own chaining?"* → Because `WorkManager` is a scheduler for *work*, not a queue of *data items*. Carrying a payload list inside the request loses items when `KEEP` drops the request, and it is not transactional with the data write.
- *"How do you avoid an infinite retry loop on a permanently bad payload?"* → Classify permanent failures (4xx validation) and move them to a `FAILED` state with a user-facing resolution path; only transient failures go back to `PENDING`.
- *"What happens if the queue has 50,000 rows?"* → Cap and coalesce; a worker that drains 50,000 rows in one run will hit WorkManager's execution limits and be stopped. Process in bounded batches and let `KEEP`/reschedule continue.
- *"How would you test restarts?"* → Enqueue, kill the process (`adb shell am force-stop`), relaunch, and assert the queue drains.

---

### 8. The application must support multiple devices editing the same document. How would you handle conflict resolution?

**What the interviewer is really asking**
This escalates Q2 from "one device vs the server" to **true multi-device collaboration**. The interviewer wants to see whether you know when to reach for **OT/CRDT**, what **operational transformation** or **vector clocks** actually buy you, and — importantly — whether you can say *when not to* use them.

**Key concepts**
- Multi-device convergence: the same document must end up identical on every device
- Vector clocks (per-device counters) vs Lamport timestamps vs hybrid logical clocks
- Last-write-wins per field, vs per-document LWW
- Operational Transformation (OT) — server-sequenced operations
- CRDTs (e.g. Yjs/Automerge) — commutative, idempotent, convergent merges
- Real-time transport: WebSocket + server as the ordering authority
- Presence / cursor awareness is separate from document state
- Tombstones and garbage collection of history

**Approach**

1. **Split the problem: state vs operations.** Two fundamentally different designs:
   - **State-based CRDT** (e.g. a grow-only set, a last-writer-wins register with per-device clocks, a PN-counter): each device computes a mergeable *value*; merging two states yields a state that dominates both. Devices exchange whole states.
   - **Operation-based OT**: each device produces *operations* (insert char at position 5); a central server transforms concurrent operations into a consistent order (`transform(op_a, op_b)`) and broadcasts the transformed sequence. Requires a central authority and is far more complex.
   State-based CRDTs are usually the right choice on mobile because they are naturally **idempotent** and **commutative** — which is exactly what you need when you cannot guarantee ordered delivery.

2. **Choose the merge granularity per field.** Most product documents are not collaborative *text* — they are structured records with a few fields. For those, **field-level LWW with per-device vector clocks** is enough and far simpler than a CRDT:
   ```kotlin
   data class VersionVector(val clocks: Map<String, Long>)   // deviceId -> counter
   ```
   Each field carries its own vector clock; when merging, the field with the **higher** clock wins, and a tie is broken deterministically (e.g. by device id) so all devices converge. Vector clocks detect causality (i.e. "did this device see my edit?") which a plain timestamp cannot.

3. **Use a server as the ordering authority for true collaborative text.** If the requirement really is simultaneous character-level editing, implement it with a server that sequences operations and a client-side OT engine (or embed a proven library such as Yjs). Do not write your own OT — it is notoriously hard to get right (TP2 problem: convergence must hold for all operation pairs).

4. **Make every mutation carry a device identity and a logical counter.** `deviceId` is generated once per install (persisted in DataStore), and the counter is monotonic per device. The pair `(deviceId, counter)` is the **Lamport-style** identity of an operation and is what makes merges deterministic.

5. **Merge deterministically and identically on every device.** The single most important property: **merge(a, b) == merge(b, a) == merge(merge(a,b), c)**. If the merge function is not commutative and associative, devices will diverge and no amount of retrying will fix it. This is the property to state in the interview.

6. **Sync transport.** Use a WebSocket for real-time collaboration (operations broadcast to other sessions of the same user) and a delta pull (`GET /v2/documents/{id}/ops?since=<seq>`) for reconnect. The server assigns a global `seq` to each accepted operation; on reconnect the client pulls the ops it missed. Because the client merges by *state*, not by sequence, out-of-order delivery is tolerated.

7. **Handle deletes with tombstones.** A document deleted on device A must not reappear because device B has an older copy. Keep a tombstone with its vector clock; garbage-collect tombstones only after every device has acknowledged a newer version (an "acknowledged by all" watermark, often tracked per device).

8. **Presence is separate.** Live cursors, "Alice is typing", and selections are *ephemeral* state — never persist them in the document, never merge them into it. Send them over a separate channel and let them expire after a timeout.

9. **Decide deliberately which algorithm you are choosing.** The staff-level framing: *"This is a CRDT problem if concurrent editing of the same region must be preserved; it is a version-merge problem if the fields are independent; and it is a plain optimistic-concurrency problem if the document is usually edited by one person at a time. I would not implement a CRDT unless the product genuinely needs character-level concurrent editing."*

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Last-write-wins (whole document) | Trivial | **Guaranteed data loss** for concurrent edits; unacceptable for shared docs |
| Field-level LWW with vector clocks | Good for structured records; detects causality | Same field edited concurrently still loses one change; vector clock state grows with device count |
| Field-level LWW with wall-clock timestamps | Simplest | Clock skew breaks determinism and convergence |
| OT with a central server | Proven for character-level collaborative editing | Very complex; must get the transform function right; needs a reliable server |
| State-based CRDT | Convergent, idempotent, works offline-first | Large metadata; requires a library or deep expertise; can grow unboundedly without compaction |
| Server-sequenced operations (no client merge) | Client is simple | Offline edits must wait for the server; "last writer" is the server's order, so offline edits lose |
| Server-authoritative merge endpoint | One implementation of the merge rules | Extra round trip; the server needs the whole history |

**Key detail:** Vector clocks tell you **causality** ("did this write happen after mine?") which timestamps cannot; a plain timestamp comparison is wrong whenever two devices' clocks disagree. But vector clocks do not tell you *how* to merge concurrent edits — that is still a policy decision (LWW per field, or a CRDT).

**Code example**

```kotlin
/** Per-device logical clock; identifies an operation and detects causality. */
data class VersionVector(val clocks: Map<String, Long>) {
    fun increment(deviceId: String) = copy(clocks = clocks + (deviceId to (clocks[deviceId] ?: 0L) + 1))

    /** Compare: >0 means "this happened after", <0 "before", 0 concurrent. */
    fun compareTo(other: VersionVector): Int {
        val ids = clocks.keys + other.clocks.keys
        var result = 0
        for (id in ids) {
            val a = clocks[id] ?: 0L
            val b = other.clocks[id] ?: 0L
            if (a > b) result = 1 else if (a < b) result = -1
        }
        return result
    }
}

data class Field<T>(val value: T, val clock: VersionVector)

data class DocumentState(
    val id: String,
    val title: Field<String>,
    val body: Field<String>,
    val fields: Map<String, Field<String>> = emptyMap(),
    val deletedAt: VersionVector? = null,
    val deviceId: String,
)

object DocumentMerger {
    /** Commutative + associative: every device converges to the same state. */
    fun merge(a: DocumentState, b: DocumentState): DocumentState = DocumentState(
        id = a.id,
        title = mergeField(a.title, b.title),
        body  = mergeField(a.body,  b.body),
        fields = (a.fields.keys + b.fields.keys).associateWith { key ->
            mergeField(a.fields[key], b.fields[key])
        },
        deletedAt = mergeClock(a.deletedAt, b.deletedAt),
        deviceId = a.deviceId,
    )

    private fun <T> mergeField(x: Field<T>?, y: Field<T>?): Field<T> {
        if (x == null) return y!!
        if (y == null) return x
        val cmp = x.clock.compareTo(y.clock)
        return when {
            cmp > 0 -> x                                  // strictly newer
            cmp < 0 -> y
            else -> if (x.value.hashCode() >= y.value.hashCode()) x else y   // deterministic tie-break
        }
    }

    private fun mergeClock(a: VersionVector?, b: VersionVector?): VersionVector? = when {
        a == null -> b
        b == null -> a
        else -> VersionVector(a.clocks.keys + b.clocks.keys.associateWith { id ->
            maxOf(a.clocks[id] ?: 0L, b.clocks[id] ?: 0L)
        })
    }
}
```

**Talking points / follow-ups**
- *"How do you test convergence?"* → Property test: generate random interleavings of edits on 3 simulated devices, apply merges in different orders, and assert all devices end up byte-identical. That is the only meaningful test for a merge algorithm.
- *"How do you bound the metadata growth?"* → Compact vector clocks, or switch to a hybrid logical clock (HLC) that encodes causality in a single timestamp-like scalar.
- *"What if a user edits on a device that has been offline for two weeks?"* → Its vector clock has a large gap; the merge still resolves per field, but you may want a policy where a very stale edit to a field the server changed is flagged for review rather than silently applied.
- *"Do I need a CRDT for a shared notes app?"* → Only if two people edit the *same paragraph* at the same time. For "mostly one editor, occasional second device", vector-clock field LWW is enough and far cheaper to operate.
- *"Who owns the CRDT library?"* → Do not hand-roll OT. Use Yjs/Automerge or a well-reviewed implementation, and keep the transport and the persistence layers yours.

---

### 9. How would you implement offline file downloads with pause, resume, retry, and integrity verification?

**What the interviewer is really asking**
Can you combine **HTTP byte-range resumption**, a **durable local checkpoint**, and **cryptographic verification** into one design? The interviewer wants to see that you know `Range: bytes=N-`, `ETag`/`If-Range`, and that the checkpoint survives process death — and that you know the difference between `DownloadManager` and a custom downloader.

**Key concepts**
- `Range: bytes=N-` + `206 Partial Content`; server must support byte ranges
- `ETag` + `If-Range` so a resumed download does not splice two different file versions
- `Content-Length` vs `Content-Range` total size
- SHA-256 checksum verification; content-addressed storage
- Durable checkpoint in Room (or a sidecar file) so resume survives process death
- `DownloadManager` vs custom OkHttp downloader vs `WorkManager` foreground worker
- `WorkManager` foreground notification for long downloads; `setForeground`
- Storage: write to a `.part` file, atomically rename on completion

**Approach**

1. **Choose the mechanism deliberately.**
   - **`android.app.DownloadManager`**: system-managed, survives app death, shows a notification, resumes automatically, handles HTTP redirects and background limits. But it gives you **limited control**: no custom headers beyond a fixed set, no easy custom integrity verification, no easy pause, and it can behave oddly behind captive portals or with auth that needs token refresh. Good for "download a public asset"; poor for a signed, authenticated, user-visible media download.
   - **Custom OkHttp downloader inside a `WorkManager` foreground worker**: full control over ranges, headers, checksums, pause/resume, progress, and cancellation. This is the answer for anything user-visible or authenticated.
   - **`ForegroundService`**: needed only if the download must continue while the device is in a state WorkManager cannot satisfy (e.g. an ongoing navigation-style media download). Prefer `WorkManager` with `setForeground` first.

2. **Use byte ranges for resumption.** Store the number of bytes already written (`downloadedBytes`). On resume, request `Range: bytes=<downloadedBytes>-`:
   ```kotlin
   val request = Request.Builder()
       .url(url)
       .header("Range", "bytes=$downloadedBytes-")
       .header("If-Range", checkpoint.etag)     // only resume if the file is unchanged
       .build()
   ```
   The server replies `206 Partial Content` with `Content-Range: bytes start-end/total`. If the server replies `200 OK` (it ignored the range) the client must **truncate** the partial file and start over — appending a full body to a partial file is the classic corruption bug.

3. **Guard against version skew with `If-Range`.** If the file changed on the server between attempts, resuming at byte N splices two different versions. `If-Range: <etag>` tells the server: "only give me a partial response if the entity is still the same one". If the ETag changed, the server returns `200` with the full body and the client restarts cleanly. Store the `ETag` and the expected `Content-Length`/total in the checkpoint.

4. **Make the checkpoint durable.** A row in Room:
   ```kotlin
   @Entity(tableName = "downloads")
   data class DownloadEntity(
       @PrimaryKey val url: String,
       val fileName: String,
       val targetPath: String,      // app-private files dir
       val downloadedBytes: Long,
       val totalBytes: Long,
       val etag: String?,
       val sha256: String?,         // expected checksum from the API
       val state: String,           // QUEUED | DOWNLOADING | PAUSED | VERIFYING | DONE | FAILED
   )
   ```
   Update `downloadedBytes` periodically (e.g. every 1 MB or 2 seconds) — updating per chunk is too many DB writes. The `.part` file on disk plus the DB row together are the resume state; the file length is the ground truth for `downloadedBytes`, so on startup use `min(dbValue, fileLength)` or just trust the file length and truncate any excess.

5. **Verify integrity before publishing.** After the download completes, compute the SHA-256 of the file and compare it to the expected digest (provided by the API as part of the download metadata — e.g. a `sha256` field, or from a signed manifest). Only then **atomically move** the file into place: `part` file → `final` file via `File.renameTo` (atomic within the same filesystem). A partially downloaded file must never be visible as a completed download.

6. **Pause, cancel, and retry semantics.**
   - **Pause**: set `state = PAUSED`, cancel the worker coroutine. The `.part` file stays; the checkpoint stays.
   - **Cancel**: delete the `.part` file and the DB row.
   - **Retry**: `Result.retry()` on transient failures, with `setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)`. Distinguish `IOException`/5xx (retry) from 4xx (permanent — e.g. 403 because the signed URL expired; the fix is to re-request a fresh URL, not to retry the same one).

7. **Run it as foreground work.** Long downloads need a foreground `Worker`:
   ```kotlin
   override suspend fun doWork(): Result {
       setForeground(createForegroundInfo(0))
       …
   }
   ```
   with `ForegroundInfo` including a notification with progress. On Android 14+ (API 34) declare `FOREGROUND_SERVICE_DATA_SYNC` permission and the service type. Without the foreground promotion, WorkManager's 10-minute execution limit kills the job.

8. **Storage hygiene.** Check free space before starting (a 2 GB download on a device with 100 MB free will fail confusingly). Write into `filesDir` (app-private, cleaned on uninstall) or `getExternalFilesDir` (app-specific external, larger, no permission needed on API 19+). Clean up orphaned `.part` files on startup.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| `DownloadManager` | System-managed; survives app death; notification for free; resumes | Limited headers (no custom auth refresh); no custom checksum verification; poor pause control; quirky with redirects/captive portals |
| Custom OkHttp + `WorkManager` foreground | Full control of ranges, headers, checksum, pause, progress | You own the lifecycle, the notification, and the 10-minute limit |
| `ForegroundService` | Uninterrupted long downloads | Notification/permission burden; `foregroundServiceType` requirements on API 34+; more lifecycle risk |
| Byte-range resume | Saves bandwidth and time on flaky networks | Requires server support for `Range`; must handle the `200` fallback correctly |
| `If-Range` with ETag | Prevents splicing two file versions | Requires a stable ETag; some CDNs change it on every request (then always restart) |
| SHA-256 verification | Detects corruption and truncation | Costs CPU (a few hundred MB/s) and a full extra read pass |
| ETags only (no checksum) | Cheap; catches most changes | Does not detect in-transit corruption |
| DB checkpoint updated per chunk | Precise resume | Thousands of DB writes; write amplification |
| DB checkpoint updated per MB | Good balance | Slightly over-downloads after a crash; harmless |
| Write to `.part` then rename | Never publishes a partial file | Extra rename step; need cleanup of orphans |

**Key detail:** `Range` + `ETag`/`If-Range` only helps if the server honours ranges. If it returns `200 OK` for a ranged request, the client must **discard the partial file and restart** — never append. Also, `Content-Range: bytes 0-1023/4096` gives you the total size; a `Content-Length` on a `206` is the *length of this response*, not the total.

**Code example**

```kotlin
@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads WHERE url = :url")
    suspend fun byUrl(url: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(d: DownloadEntity)

    @Query("UPDATE downloads SET downloadedBytes = :bytes, state = :state WHERE url = :url")
    suspend fun updateProgress(url: String, bytes: Long, state: String)
}

class FileDownloadWorker(
    context: Context,
    params: WorkerParameters,
    private val dao: DownloadDao,
    private val api: DownloadApi,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL)!!
        val meta = api.downloadMeta(url)                    // { url, sha256, size, fileName }
        val part = File(filesDir, "${meta.fileName}.part")
        val target = File(filesDir, meta.fileName)

        var checkpoint = dao.byUrl(url)
        if (checkpoint == null) {
            checkpoint = DownloadEntity(url, meta.fileName, target.path, 0, meta.size, null, meta.sha256, "QUEUED")
            dao.upsert(checkpoint)
        }
        if (target.exists() && target.length() == meta.size) return verify(target, meta.sha256)

        val resumeFrom = if (part.exists()) part.length() else 0L
        val request = Request.Builder()
            .url(meta.url)
            .apply { if (resumeFrom > 0) {
                header("Range", "bytes=$resumeFrom-")
                checkpoint.etag?.let { header("If-Range", it) }
            } }
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body ?: return Result.failure()
                val append = response.code == 206 && resumeFrom > 0
                if (!append && resumeFrom > 0) part.delete()      // server ignored Range → restart

                val sink = if (append) part.openOutputStream(append = true) else part.openOutputStream(false)
                var written = if (append) resumeFrom else 0L
                body.byteStream().use { input ->
                    sink.use { out ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            ensureActive()                                    // honour pause/cancel
                            val n = input.read(buf); if (n < 0) break
                            out.write(buf, 0, n); written += n
                            if (written % (1L shl 20) < 64 * 1024) {         // checkpoint ~1 MB
                                dao.updateProgress(url, written, "DOWNLOADING")
                                setProgress(Data.Builder().putLong("bytes", written).build())
                            }
                        }
                    }
                }
                checkpoint = checkpoint.copy(etag = response.header("ETag") ?: checkpoint.etag)
                dao.upsert(checkpoint.copy(downloadedBytes = written))
            }

            // Integrity gate BEFORE publishing.
            if (!verify(part, meta.sha256)) {
                part.delete(); dao.updateProgress(url, 0, "FAILED")
                return Result.failure()                                  // corrupt → restart from scratch
            }
            check(part.renameTo(target))                                 // atomic publish
            dao.updateProgress(url, target.length(), "DONE")
            Result.success()
        } catch (e: CancellationException) {
            dao.updateProgress(url, part.length(), "PAUSED")               // resumable
            throw e
        } catch (e: IOException) {
            dao.updateProgress(url, part.length(), "FAILED")
            Result.retry()                                                // WorkManager backoff
        }
    }

    private fun verify(file: File, expectedSha: String?): Boolean {
        if (expectedSha == null) return file.length() > 0
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) { val n = input.read(buf); if (n < 0) break; digest.update(buf, 0, n) }
        }
        return digest.digest().toHexString().equals(expectedSha, ignoreCase = true)
    }
}
```

**Talking points / follow-ups**
- *"Why not `DownloadManager`?"* → It cannot do a custom checksum, cannot carry a short-lived signed header set reliably, and gives little control over pause. Use it for simple public assets; use the custom worker for authenticated, user-visible media.
- *"What if the server does not support ranges?"* → Detect the `200` response and restart; consider a "resume unsupported" flag so you do not keep trying ranges for every subsequent file.
- *"How do you handle the signed URL expiring mid-download?"* → On a `403`, re-fetch a fresh signed URL from the API and retry once with the same byte offset — the `Range`/`If-Range` headers make this safe.
- *"Where do you store the file?"* → App-private `filesDir` (deleted on uninstall, no permission) or `getExternalFilesDir` for larger media; content-provider them out via `FileProvider` or `MediaStore` for sharing.
- *"How do you avoid filling the user's device?"* → Check `StatFs` free space before starting; cap the total download cache and evict least-recently-used files.

---

### 10. The server schema changes while older app versions remain in use. How would you maintain backward compatibility?

**What the interviewer is really asking**
Can you design a **versioned, additive-only API contract** that old clients do not break, and can you make the *client* tolerant of unknown fields and missing fields? The interviewer wants to see the combination of API versioning, tolerant deserialisation, and a "never break old clients" deployment discipline.

**Key concepts**
- Versioned endpoints (`/v1/…`, `/v2/…`) or additive-only evolution of `/v1`
- Additive-only changes: new optional fields, new endpoints — never remove or rename
- Unknown-field tolerance: `@JsonIgnoreProperties(ignoreUnknown = true)` (Moshi/Gson), `ignoreUnknownKeys = true` (kotlinx.serialization)
- Nullability/defaults: new fields must be optional or have defaults, otherwise old clients break
- Server must keep old versions alive for the support window; measure the client-version distribution
- Remote config / feature flags to gate new behaviour on the client
- Graceful degradation: old client + new server must return a response the old client can parse
- Field removal requires a deprecation window and a `v2` endpoint, not a silent drop

**Approach**

1. **Adopt a support window and measure the client distribution.** The single most important operational practice: instrument the client version (`BuildConfig.VERSION_NAME` / a `clientVersion` header) and watch the adoption curve. You may only break a version when its usage has fallen below a threshold (e.g. < 0.1% for 2 release cycles). Everything else follows from this number.

2. **Prefer additive-only evolution over versioning.** Adding an optional field to a response is safe for old clients *only if the old client tolerates unknown fields* — which it must (see point 4). Adding a new endpoint is always safe. Changing a field's type, removing a field, or making an optional field required is a breaking change.

3. **Use explicit versions when you must break.** If a breaking change is unavoidable, expose a **new version** and keep the old one alive:
   ```kotlin
   interface ApiV1 { @GET("v1/transactions") suspend fun transactions(): List<TxDtoV1> }
   interface ApiV2 { @GET("v2/transactions") suspend fun transactions(): List<TxDtoV2> }
   ```
   Route by the version the client declares, so both coexist server-side. Migrate clients gradually: an old client keeps hitting `v1`; a new client uses `v2`; when `v1` traffic drops to ~0, delete it.

4. **Make deserialisation tolerant.** This is the client-side half of the contract:
   ```kotlin
   @JsonClass(generateAdapter = true)
   @JsonIgnoreProperties(ignoreUnknown = true)   // Moshi
   data class TxDto(
       @Json(name = "id") val id: String,
       @Json(name = "amountCents") val amountCents: Long,
       @Json(name = "categoryId") val categoryId: String? = null,   // optional + default
       @Json(name = "newField") val newField: String? = null,       // unknown to v1 clients
   )
   ```
   ```kotlin
   @Serializable
   data class TxDto(                        // kotlinx.serialization
       val id: String,
       val amountCents: Long,
       val categoryId: String? = null,
   )
   // Json { ignoreUnknownKeys = true } in the Json configuration
   ```
   - Moshi: `@JsonIgnoreProperties(ignoreUnknown = true)` or `JsonClass(generateAdapter = true)` with `moshi-kotlin-codegen` (the codegen adapter **ignores unknown keys by default** — the reflective adapter does not).
   - kotlinx.serialization: `Json { ignoreUnknownKeys = true }` — **must** be set explicitly, or a new server field crashes the old client with `SerializationException`.
   - Gson: `GsonBuilder().ignoreUnknownKeys()` — but Gson silently leaves the field null, which hides errors.
   A tolerant *reader* is what makes additive-only evolution safe.

5. **Never make a new server field required without a default.** If the server adds `"discount": 123`, the client model must have `val discount: Long? = null` or `val discount: Long = 0L`. A non-null field with no default breaks deserialisation of older payloads (and of payloads produced by *older servers* that the new client talks to — this direction matters too).

6. **Handle the reverse direction — new client, old server.** A new client talking to a not-yet-upgraded server must tolerate *missing* fields. Nullable fields with defaults handle it. Also gate new features behind a capability check: the server exposes `/v1/capabilities` or the client feature-flag service, and the UI hides the feature until the server supports it. This is the "old server + new client" half of compatibility, which people forget.

7. **Use feature flags / remote config for rollout, not for breaking changes.** A remote flag lets you enable new server behaviour for new clients only, and roll back instantly if something breaks in production. It does not replace versioning, but it reduces how often you need it.

8. **Contract testing as the safety net.** Use Pact-style consumer-driven contracts, or at minimum a golden-file test that deserialises a *recorded production response* into your DTOs. Store real (redacted) payloads in `src/test/resources` and assert they still parse — that catches "the server added a field and we crashed" before release.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Additive-only, no endpoint versions | Simplest for clients; one codebase path | Accumulates cruft; some changes genuinely cannot be additive (renames, type changes) |
| Parallel `/v1` and `/v2` endpoints | Clean, explicit; old clients unaffected | Server must maintain two implementations; doubles the contract surface |
| Route by client version header | Transparent to the client; server owns the routing | Every request pays a routing decision; must be monitored for traffic split |
| `ignoreUnknownKeys = true` | Old clients survive new server fields | **Silently ignores typos** in your own DTOs — a renamed field becomes `null` with no error |
| Strict deserialisation | Catches server/client drift immediately | Any new server field crashes old clients — unacceptable in production |
| Feature flags for rollout | Instant rollback; no app release needed | Flag sprawl; needs a kill switch; not a substitute for versioning |
| Contract tests | Catches drift in CI | Requires the backend to participate in the contract |
| Support window with telemetry | Data-driven deprecation | Slower cleanup; the old endpoint must stay maintained |

**Key detail:** The dangerous half is not "old client + new server" — it is "old client + new server" where the new field is **not tolerated**, which crashes the old client at deserialisation time (often on the launch screen). The second dangerous half is "new client + old server", where a newly-required field is missing and deserialisation fails or silently yields `null` into business logic.

**Code example**

```kotlin
// ---- DTOs: unknown fields tolerated, new fields optional with defaults ----
@JsonClass(generateAdapter = true)
data class TransactionDto(
    @Json(name = "id") val id: String,
    @Json(name = "amountCents") val amountCents: Long,
    @Json(name = "memo") val memo: String? = null,
    // Added by the server in v1.7 — absent on older servers, unknown to older clients.
    @Json(name = "loyaltyPoints") val loyaltyPoints: Long? = null,
)

private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()                                   // codegen adapters ignore unknown keys

private val json = Json {
    ignoreUnknownKeys = true                   // kotlinx.serialization: MUST be explicit
    coerceInputValues = true                  // null → default for a non-null field
    explicitNulls = false
}

sealed class Capabilities {
    object Unknown : Capabilities()
    data class Known(val serverVersion: Int, val supportsLoyalty: Boolean) : Capabilities()
}

class TransactionApi(
    private val retrofit: Retrofit,
    private val featureFlags: FeatureFlags,
) {
    /** v1 stays backward compatible; new behaviour is gated by a capability check. */
    @GET("v1/transactions")
    suspend fun transactions(@Header("If-None-Match") etag: String? = null): List<TransactionDto>

    @GET("v1/capabilities")
    suspend fun capabilities(): CapabilitiesResponse
}

class TransactionMapper(
    private val caps: Flow<Capabilities>,
) {
    fun toDomain(dto: TransactionDto): Transaction = Transaction(
        id = dto.id,
        amountCents = dto.amountCents,
        memo = dto.memo.orEmpty(),
        // Only surface the new field if BOTH the server and the rollout flag support it.
        loyaltyPoints = dto.loyaltyPoints?.takeIf { caps.value is Capabilities.Known },
    )
}

// ---- Server side (conceptual) ----
// GET /v1/transactions
// {
//   "id": "tx_1",
//   "amountCents": 2500,
//   "memo": "coffee",
//   "loyaltyPoints": 12          // NEW: additive, optional → old clients must ignore it
// }
//
// GET /v2/transactions           // only if a genuinely breaking change is required
// { "id": "tx_1", "amount": { "minor": 2500, "currency": "USD" } }
```

**Talking points / follow-ups**
- *"What if the server removes a field?"* → That is a breaking change. Keep the field (deprecated but populated) for the support window, or version the endpoint. Silently removing it breaks old clients that expect it.
- *"How do you know which clients are still on v1?"* → Send a client version header on every request and report the distribution; drive deprecation from that dashboard.
- *"Is `ignoreUnknownKeys` risky?"* → Yes — it also hides your own typos. Mitigate with strict-mode unit tests over recorded payloads and a lint rule that fails the build if a DTO field is missing from a fixture.
- *"How do you version a GraphQL/REST hybrid?"* → The same rules apply: additive fields on the same type, deprecate before removing, keep the old field populated.
- *"Should the client ever call two versions?"* → No — pick the version at build time via a `BuildConfig`/flavor switch, or by a capability negotiation, but do not have a client calling `v1` and `v2` for the same resource simultaneously.

---

### 11. A local database transaction involves several related tables. How would you ensure atomicity?

**What the interviewer is really asking**
Can you distinguish the **three levels of atomicity** in Room — a single `@Insert`, a `@Transaction` DAO method, and `db.withTransaction { }` across DAOs — and do you know the *failure modes* (partial writes, nested transaction errors, the "cannot access database on the main thread" trap) and how to test atomicity?

**Key concepts**
- `@Transaction` on a DAO method → Room wraps it in a SQLite transaction on the query executor
- `db.withTransaction { }` (room-ktx) — a real transaction spanning multiple DAOs
- `RoomDatabase.transaction {}` / `runInTransaction` — the non-suspend equivalent
- SQLite's transaction semantics: deferred vs immediate; `BEGIN IMMEDIATE` avoids upgrade deadlocks
- "Cannot access database on the main thread" — Room's main-thread check
- Deadlock risk: a transaction that triggers a `Flow` collector that writes back
- Foreign keys: `PRAGMA foreign_keys = ON` (off by default in SQLite)
- Cascades: `onDelete = CASCADE` via `@ForeignKey` + `@Index` on the child FK

**Approach**

1. **Know the three mechanisms.**
   - A single `@Insert`/`@Update`/`@Delete` is atomic by itself — SQLite wraps it in a transaction.
   - A **multi-statement DAO method** annotated `@Transaction` is executed as one SQLite transaction. Room generates the code to open a transaction, run the statements, and commit.
   - A **cross-DAO operation** needs `db.withTransaction { }` from `room-ktx` — it acquires the database's single transaction slot and gives you a coroutine-safe, thread-confined scope. `runInTransaction { }` is the blocking equivalent (do not call it on the main thread).

2. **Prefer `db.withTransaction { }` for anything crossing DAOs.** It is the right tool and it handles the coroutine context correctly:
   ```kotlin
   class TxRepository(private val db: AppDatabase) {
       suspend fun applyEdit(tx: Transaction, lines: List<LineItem>) = db.withTransaction {
           db.transactionDao().upsert(tx.toEntity())
           db.lineItemDao().deleteForTransaction(tx.id)
           db.lineItemDao().insertAll(lines.map { it.toEntity() })
           db.syncQueueDao().insert(OutboxRow(id = UUID.randomUUID().toString(), …))
       }
   }
   ```
   Everything inside is one SQLite transaction: either all the tables are updated, or none are. If the app is killed mid-transaction, SQLite's WAL/journal guarantees a rollback on next open.

3. **Prefer a DAO-level `@Transaction` when the operation is within one DAO** — it avoids the overhead of acquiring the database-level transaction and is the more idiomatic form:
   ```kotlin
   @Dao
   interface OrderDao {
       @Transaction
       suspend fun replaceOrder(order: OrderEntity, items: List<ItemEntity>) {
           upsertOrder(order)
           deleteItems(order.id)
           insertItems(items)
       }
   }
   ```
   Note that `@Transaction` on a `suspend` function runs on Room's **query executor** (a background thread), not the main thread, and the function's body must be a suspend function or a plain function in the same DAO.

4. **Set up foreign keys and cascades so the DB enforces the invariants.** SQLite has foreign keys **disabled by default**; Room enables them when the entity declares `@ForeignKey` relationships and you don't disable them. Declare them explicitly:
   ```kotlin
   @Entity(
       tableName = "line_items",
       foreignKeys = [
           ForeignKey(
               entity = TransactionEntity::class,
               parentColumns = ["id"],
               childColumns = ["transactionId"],
               onDelete = ForeignKey.CASCADE,
           ),
       ],
       indices = [Index("transactionId")],   // child FK columns MUST be indexed
   )
   data class LineItemEntity(…)
   ```
   The `Index` on the child FK column is **required** — SQLite needs it to enforce the constraint efficiently, and Room will fail to compile without it.

5. **Avoid deadlocks and nested-transaction errors.**
   - A `Flow` collector that writes to the same database while inside a transaction can deadlock: the transaction holds the write lock, the collector's write waits, and the transaction waits for the collector. Rule: **never write to the DB from inside a transaction triggered by observing that same DB.** Collect the `Flow` *outside* the transaction and act on it there.
   - `db.withTransaction { }` inside another `db.withTransaction { }` on a **different** coroutine/thread throws ("Cannot perform this operation because there is no current transaction" / a SQLite "database is locked" error). Never nest across coroutines; keep the transaction body non-suspending-heavy and do not launch parallel coroutines inside it.
   - Prefer `BEGIN IMMEDIATE` semantics for write transactions that will also read, to avoid a deferred-transaction upgrade deadlock where two writers both read then both try to write. Room uses `BEGIN IMMEDIATE` for transactions that contain writes in most configurations — but be aware of it when mixing `Flow` observers.

6. **Do not do long work inside a transaction.** A transaction holds a lock on the database file. No network call, no file I/O, no heavy computation inside `db.withTransaction { }` — it blocks every other reader and writer in the process, and can trigger ANRs if you do it on the main thread. Do the expensive work *outside*, then open a short transaction for the writes.

7. **Test atomicity explicitly.** Use `MigrationTestHelper`-style in-memory tests and deliberately fail mid-transaction:
   ```kotlin
   @Test fun insertOrderAndItems_isAtomic() = runTest {
       val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
           .allowMainThreadQueries()          // for tests only
           .build()
       assertThrows<SQLiteConstraintException> {
           db.withTransaction {
               db.orderDao().insert(OrderEntity(id = "o1", …))
               db.itemDao().insert(ItemEntity(id = "i1", orderId = "o1", price = -1))  // violates CHECK
           }
       }
       assertEquals(0, db.orderDao().count())    // the first insert was rolled back
   }
   ```
   The assertion that the first table is empty after the failure is the actual test of atomicity.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| `@Transaction` on a DAO method | Idiomatic, cheap, compiler-checked | Only works within one DAO; the body must not call other DAOs' suspend functions in a way that escapes the transaction |
| `db.withTransaction { }` across DAOs | General purpose, coroutine-friendly | Acquires the DB-wide transaction slot; a long body blocks everything |
| `runInTransaction { }` | Works from non-suspend code | Blocking — must not be called on the main thread |
| Manual `db.beginTransaction()/setTransactionSuccessful()/endTransaction()` | Maximum control | Easy to get wrong; leaks the transaction on an exception if not in a `try/finally`; prefer `withTransaction` |
| Foreign keys + cascades | DB enforces referential integrity | Slight write overhead; cascades can delete a lot of rows silently — use `RESTRICT` where you want an explicit error |
| Doing the whole aggregate in one transaction | Strong consistency | Long transaction = lock contention; scale with the number of rows |
| Application-level "undo" instead of transactions | Users can recover from mistakes | Does not prevent partial writes on crash |

**Key detail:** `db.withTransaction { }` is *not* the same as a database-level exclusive lock across the whole app — it uses SQLite's transaction mechanism on the database's own connection, and Room serialises writers. The critical rule is that **no network or long-running work may run inside the transaction body**, because the write lock is held for the duration.

**Code example**

```kotlin
@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val totalCents: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("orderId")],          // required: the child FK column must be indexed
)
data class OrderItemEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val sku: String,
    val qty: Int,
    val priceCents: Long,
)

@Dao
interface OrderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: OrderEntity)

    @Query("DELETE FROM order_items WHERE orderId = :orderId")
    suspend fun deleteItems(orderId: String)

    @Insert
    suspend fun insertItems(items: List<OrderItemEntity>)

    @Transaction                                   // single-DAO variant
    suspend fun replaceOrder(order: OrderEntity, items: List<OrderItemEntity>) {
        insert(order)
        deleteItems(order.id)
        insertItems(items)
    }
}

class OrderRepository(private val db: AppDatabase) {

    /** Cross-DAO variant: one SQLite transaction over three tables. */
    suspend fun saveOrder(order: OrderEntity, items: List<OrderItemEntity>) {
        // Do the slow work OUTSIDE the transaction — never hold the write lock for I/O.
        val priced = items.map { it.copy(priceCents = priceService.price(it.sku)) }

        db.withTransaction {
            db.orderDao().replaceOrder(order, priced)
            db.syncQueueDao().insert(
                OutboxRow(
                    id = UUID.randomUUID().toString(),   // idempotency key, persisted atomically
                    entityId = order.id,
                    payload = order.toJson(),
                    baseVersion = 0,
                    createdAt = System.currentTimeMillis(),
                )
            )
        }
    }
}

// Atomicity test: the first insert must be rolled back.
@Test
fun saveOrder_isAtomic_acrossTables() = runTest {
    val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    db.orderDao().insert(OrderEntity("o1", 1000, 0))          // seed a valid parent

    assertThrows<SQLiteConstraintException> {
        db.withTransaction {
            db.orderDao().insert(OrderEntity("o2", 2000, 0))
            db.orderItemDao().insert(OrderItemEntity("i2", "o2", "sku", 1, -1)) // violates CHECK
        }
    }
    assertEquals(0, db.orderDao().countById("o2"))            // rolled back → atomic
    assertEquals(1, db.orderDao().countById("o1"))            // untouched
}
```

**Talking points / follow-ups**
- *"What happens if the process dies mid-transaction?"* → SQLite's WAL (or rollback journal) makes the transaction atomic: on next open the DB is either fully before or fully after. This is why Room's writes survive process death.
- *"Does `withTransaction` block the main thread?"* → It suspends; the transaction runs on Room's query executor. But it *does* block other DB access in the app while it holds the write lock.
- *"Can I call `withTransaction` from a `Flow` collector?"* → Yes, but never collect the *same* table you are writing inside the transaction — that is the classic deadlock. Collect outside, write inside.
- *"How do you handle a long-running transaction over 10k rows?"* → Batch it into chunks of a few hundred rows, each its own transaction, and make the operation resumable/idempotent (a progress marker), so a crash mid-batch can resume.
- *"Should I use `@Transaction` on a non-suspend DAO method?"* → Yes, Room supports it on blocking DAO methods too — but only call those from a background thread.

---

### 12. How would you design an offline-first repository using Room, Retrofit, Flow, and WorkManager?

**What the interviewer is really asking**
This is the **capstone** question. The interviewer wants to see the whole architecture assembled coherently — one repository that owns the read path (Room `Flow`), the write path (local write + outbox), the network path (Retrofit), and the scheduling path (WorkManager) — and whether you can articulate the **guarantees** that design provides and where it breaks down.

**Key concepts**
- Single writer pattern: only the repository writes to Room
- Read path: `Flow` from Room; write path: local write + outbox row in one transaction
- `WorkManager` as the sync scheduler; `ExistingWorkPolicy.KEEP`; backoff; constraints
- `Retrofit` + `OkHttp` with an auth interceptor, a cache header interceptor, and a `Retry-After`-aware error mapper
- Dependency injection: Hilt — `@Singleton` repository, `@IoDispatcher` qualifier, constructor injection of DAOs and the API
- `SharingStarted.WhileSubscribed` / `stateIn` for the UI layer
- The repository's API surface: `observe…` (Flow), `mutate…` (suspend), `refresh()` (suspend), `syncStatus` (StateFlow)
- Testability: inject a fake API, use an in-memory Room DB

**Approach**

1. **Define the contract first.** The repository's public surface tells the whole story:
   ```kotlin
   interface TransactionRepository {
       fun observeTransactions(accountId: String): Flow<List<Transaction>>   // read: DB only
       fun observeSyncStatus(): StateFlow<SyncStatus>                        // read: DB only
       suspend fun upsert(transaction: Transaction)                          // write: DB + outbox
       suspend fun delete(id: String)                                        // write: tombstone + outbox
       suspend fun refresh(): SyncOutcome                                    // pull: network → DB
   }
   ```
   Note the asymmetry: **`observe*` never touches the network.** That is the single most important property of an offline-first repository, and it is what makes the UI trivially testable.

2. **Single-writer discipline.** Only the repository writes to Room. The UI never calls `dao.upsert` directly; it calls `repository.upsert`, which writes the entity *and* enqueues the outbox row in one transaction. This means there is exactly one place where the invariants (outbox ↔ data consistency) are maintained, and it is unit-testable without a ViewModel.

3. **Layer the code into three collaborators.** Keep the repository thin and delegate:
   - `TransactionDao` / `OutboxDao` / `SyncMetaDao` (Room)
   - `TransactionApi` (Retrofit) — pure DTOs in/out, no DB knowledge
   - `TransactionSyncWorker` (WorkManager) — owns the *scheduling* and the push loop
   The repository owns the *orchestration*: read → Room, write → Room + outbox, refresh → API → Room.

4. **Wire the read path with `Flow` and `stateIn`.** In the ViewModel:
   ```kotlin
   val transactions: StateFlow<List<Transaction>> =
       repository.observeTransactions(accountId)
           .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
   ```
   `WhileSubscribed(5_000)` keeps the DB query alive for 5 seconds after the last collector leaves, avoiding a query storm on configuration changes while not holding resources indefinitely.

5. **Wire the write path.** `upsert` writes locally and enqueues in one transaction, then (opportunistically) requests a sync:
   ```kotlin
   override suspend fun upsert(transaction: Transaction) {
       db.withTransaction {
           dao.upsert(transaction.toEntity())
           outboxDao.insert(OutboxRow(
               id = UUID.randomUUID().toString(),
               entityId = transaction.id,
               payload = transaction.toJson(),          // snapshot
               baseVersion = transaction.version,
               createdAt = clock.nowMillis(),
           ))
       }
       syncScheduler.requestSync()      // fire-and-forget; the worker is the real guarantee
   }
   ```
   The write is durable *before* the network is involved — so the UI can show the change immediately and the user can close the app.

6. **Wire the network path.** The API returns DTOs; the repository maps them to entities and upserts them **idempotently**:
   ```kotlin
   override suspend fun refresh(): SyncOutcome = try {
       val remote = api.getTransactions(since = metaDao.lastSyncCursor())
       db.withTransaction {
           remote.items.forEach { dao.upsert(it.toEntity()) }     // @Upsert → replay-safe
           metaDao.setSyncCursor(remote.nextCursor)
           metaDao.setLastSyncedAt(clock.nowMillis())
       }
       SyncOutcome.Success(remote.items.size)
   } catch (e: IOException) {
       SyncOutcome.Offline            // DB still has the last good data
   }
   ```
   Note the ordering: `withTransaction` so the cursor and the data advance together. If the process dies between them, you would re-fetch the same delta (harmless) rather than skip it (data loss).

7. **Wire the scheduler.** `SyncScheduler` wraps `WorkManager` so the repository does not depend on the Android framework directly (and so it can be faked in tests):
   ```kotlin
   class WorkManagerSyncScheduler(context: Context) : SyncScheduler {
       private val wm = WorkManager.getInstance(context)
       override fun requestSync() {
           val request = OneTimeWorkRequestBuilder<TransactionSyncWorker>()
               .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
               .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
               .build()
           wm.enqueueUniqueWork("transaction-sync", ExistingWorkPolicy.KEEP, request)
       }
   }
   ```
   The worker drains the outbox, then pulls the delta. Both halves are idempotent, so re-running is safe.

8. **Expose sync status.** A `SyncStatus` (lastSyncedAt, pendingCount, state) stored in the `sync_meta` table and exposed as a `StateFlow` lets the UI show "Syncing…" / "3 changes pending" / "Updated 5 min ago". This is where most offline-first implementations fail — not in the sync logic but in the *lack of feedback* that turns sync bugs into silent data loss.

9. **Inject dependencies with Hilt.** The repository is `@Singleton`; DAOs come from the `RoomDatabase` provider; the API comes from a Retrofit provider with an auth interceptor and a `Retry-After`-aware error mapper. Qualifiers keep `Dispatchers.IO` explicit:
   ```kotlin
   @Qualifier annotation class IoDispatcher
   @IoDispatcher @Provides fun provideIo() = Dispatchers.IO
   ```

10. **Make it testable.** With `Room.inMemoryDatabaseBuilder`, a fake API, and a fake scheduler, the whole repository is testable with no Android framework:
    ```kotlin
    @Test fun upsert_queues_outboxRow() = runTest {
        val db = inMemoryDb()
        val repo = TransactionRepository(db, db.txDao(), db.outboxDao(), db.metaDao(), FakeApi(), FakeScheduler(), clock)
        repo.upsert(sample)
        assertEquals(1, db.outboxDao().pendingCount())
        assertEquals(sample.id, db.txDao().byId(sample.id)!!.id)
    }
    ```

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Repository as the only writer | One place for the invariants; testable; UI cannot bypass them | Repository grows large; needs clear internal decomposition |
| Repository touches `WorkManager` directly | Fewer abstractions | Hard to unit test; couples domain code to the framework — prefer a `SyncScheduler` interface |
| `SyncScheduler` interface | Testable; fakeable; the framework is an implementation detail | One more layer |
| UI calls `refresh()` on pull-to-refresh | Fresh data on demand | Writes to the DB while the user is looking — fine, but the UI must not swap the list for a spinner; keep showing stale data with a refresh indicator |
| `stateIn(WhileSubscribed(5_000))` | Avoids query churn; keeps data warm across rotation | Keeps a DB query alive 5s after the last collector; fine, but not `Eagerly` for a big table |
| `Eagerly` | Data is always warm | Holds a DB query forever; wasteful for large datasets |
| Network triggered only by the worker | Simple, one sync path | Slower freshness after a local write; mitigated by `requestSync()` on write |
| Network triggered on every write | Fresh quickly | Chatty; wasteful; mitigated by `ExistingWorkPolicy.KEEP` |

**Key detail:** The repository's `observe*` functions must return data **without** doing any network I/O, even "just to check freshness". The moment a read path can fail because of the network, the offline guarantee is gone and the UI must start modelling loading/error states for data it already has.

**Code example**

```kotlin
interface SyncScheduler {
    fun requestSync()
    fun observeStatus(): Flow<SyncStatus>
}

@Singleton
class TransactionRepository(
    private val db: AppDatabase,               // for db.withTransaction { } across DAOs
    private val txDao: TransactionDao,
    private val outboxDao: OutboxDao,
    private val metaDao: SyncMetaDao,
    private val api: TransactionApi,
    private val scheduler: SyncScheduler,
    private val clock: Clock,
) {
    // ---------- READ PATH: database only, never the network ----------
    fun observeTransactions(accountId: String): Flow<List<Transaction>> =
        txDao.observeForAccount(accountId).map { it.map(OrderEntity::toDomain) }

    fun observeSyncStatus(): Flow<SyncStatus> = combine(
        metaDao.observeLastSyncedAt(),
        outboxDao.observePendingCount(),
    ) { last, pending -> SyncStatus(lastSyncedAt = last, pendingCount = pending) }

    // ---------- WRITE PATH: local write + durable outbox, one transaction ----------
    suspend fun upsert(tx: Transaction) {
        db.withTransaction {
            txDao.upsert(tx.toEntity())
            outboxDao.insert(
                OutboxRow(
                    id = UUID.randomUUID().toString(),    // idempotency key, stable across retries
                    entityId = tx.id,
                    payload = tx.toJson(),                 // snapshot at edit time
                    baseVersion = tx.version,
                    createdAt = clock.nowMillis(),
                )
            )
        }
        scheduler.requestSync()                          // the worker is the real guarantee
    }

    suspend fun delete(id: String) {
        db.withTransaction {
            txDao.softDelete(id, clock.nowMillis())       // tombstone, never a hard delete
            outboxDao.insert(OutboxRow(id = UUID.randomUUID().toString(), entityId = id, payload = """{"deleted":true}""", …))
        }
        scheduler.requestSync()
    }

    // ---------- NETWORK PATH: idempotent upsert into the DB ----------
    suspend fun refresh(): SyncOutcome = try {
        val remote = api.getTransactions(since = metaDao.lastSyncCursor())
        db.withTransaction {
            remote.items.forEach { txDao.upsert(it.toEntity()) }
            metaDao.setSyncCursor(remote.nextCursor)
            metaDao.setLastSyncedAt(clock.nowMillis())
        }
        SyncOutcome.Success(remote.items.size)
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        SyncOutcome.Offline
    }
}

class TransactionSyncWorker(
    context: Context,
    params: WorkerParameters,
    private val db: AppDatabase,
    private val api: TransactionApi,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        // 1. PUSH the durable outbox (each push is idempotent by outbox id).
        while (true) {
            val rows = db.outboxDao().nextBatch(50)
            if (rows.isEmpty()) break
            for (row in rows) {
                val resp = api.push(row.entityId, row.payload, row.baseVersion, row.id)
                db.withTransaction {
                    db.txDao().applyServerVersion(row.entityId, resp.version)
                    db.outboxDao().delete(row.id)
                }
            }
        }
        // 2. PULL the delta (idempotent upsert + monotonic cursor).
        val delta = api.getTransactions(since = db.metaDao().lastSyncCursor())
        db.withTransaction {
            delta.items.forEach { db.txDao().upsert(it.toEntity()) }
            db.metaDao().setSyncCursor(delta.nextCursor)
            db.metaDao().setLastSyncedAt(System.currentTimeMillis())
        }
        Result.success()
    }
}
```

**Talking points / follow-ups**
- *"Where does the ViewModel fit?"* → It exposes `StateFlow`s from the repository's `Flow`s and forwards user intents to the repository. It contains no Room or Retrofit knowledge — which is what makes it testable with fakes.
- *"How do you keep the UI from showing a spinner on every refresh?"* → Because the UI reads from the DB, a refresh never blanks the list; it only updates it. Show a small refresh indicator instead of a full-screen spinner.
- *"What if the app is killed between the local write and the sync?"* → The outbox row is already committed; the worker drains it on the next run. Nothing is lost.
- *"How do you know the sync is actually working?"* → Instrument: outbox depth over time, age of the oldest pending row, sync success/failure rate, delta size. A rising queue depth is the earliest signal.
- *"How would you migrate this to a multi-account device?"* → Scope every query by `accountId` (already indexed), and include the account in the outbox so switching accounts does not push another account's edits.
- *"What about the first launch with an empty DB?"* → Ship a bundled seed or show a first-run empty state; never block the UI on the network.

---

## Section Summary — Staff-Level Themes

| Theme | Questions |
|---|---|
| **The local database is the source of truth** | 1, 3, 4, 11, 12 — the UI reads Room, never the network |
| **Durability and the outbox** | 1, 6, 7, 12 — a local write and its queued mutation must be one transaction |
| **Idempotency at every layer** | 5, 6, 7, 12 — upserts, watermarks, stable keys, unique indices, `KEEP` policy |
| **Conflict resolution** | 2, 8, 12 — detect with `version`/`If-Match`, merge per field, converge deterministically |
| **Schema, migrations and compatibility** | 3, 10, 11 — exported schemas, `MigrationTestHelper`, additive APIs, tolerant readers |
| **Scale and performance** | 4, 9, 11 — composite/covering indices, keyset pagination, bounded storage, short transactions |
| **Background execution** | 6, 7, 9, 12 — `WorkManager`, backoff, constraints, leases, foreground work |
| **Staff framing** | 2, 4, 8, 10, 12 — pick the algorithm deliberately; say what you would *not* build |

**The one sentence to memorise for this section:**
> *"The local database is the only thing the UI observes, every local write is committed together with a durable outbox entry, and every network operation is idempotent — so the app behaves identically whether the network is fast, slow, or absent, and no user edit is ever lost or applied twice."*
