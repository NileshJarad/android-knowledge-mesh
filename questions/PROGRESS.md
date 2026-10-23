# Answers Progress Tracker

Source: `question_list.md` — **130 questions** across **11 sections**. (The original plan said 146; the source actually contains 130. Per-section counts verified against the file: 15/15/15/10/12/11/9/9/9/10/15 = 130.)

Started: 2026-10-22. **Status: ALL SECTIONS COMPLETE ✅**

Legend: `[ ]` pending · `[~]` in progress · `[x]` done

---

## Section 1 — Network, API and Distributed Systems → `network-api-and-distributed-systems.md` ✅ DONE
- [x] 1. Payment request times out but server may have processed it — prevent duplicate charges
- [x] 2. User kills app during payment and reopens later — recover payment state
- [x] 3. API request triggered twice by double-tap — prevent duplicate operations
- [x] 4. Two search API responses arrive out of order — stale data must not overwrite latest
- [x] 5. Screen depends on five APIs, only three succeed — handle partial failures
- [x] 6. Network switches Wi-Fi → mobile data mid-request — handle the request
- [x] 7. API takes 30 seconds — design loading, timeout, cancellation, retry
- [x] 8. HTTP 401 with multiple concurrent requests — refresh token without multiple refreshes
- [x] 9. Access token expires mid file-upload — recover without uploading twice
- [x] 10. App must work offline and sync when connectivity returns
- [x] 11. HTTP 429 rate limiting — handle retries
- [x] 12. Server processes request but client cancels coroutine before response
- [x] 13. Backend returns inconsistent data across endpoints — maintain consistent UI
- [x] 14. User starts an operation on one device, continues on another — sync + conflict
- [x] 15. Design a centralized network error-handling mechanism

## Section 2 — Performance, Large Datasets and UI Rendering → `performance-large-datasets-and-ui-rendering.md` ✅ DONE
- [x] 1. API returns 1,000 transactions — display without UI lag
- [x] 2. API returns 100,000 records — data-loading and rendering strategy
- [x] 3. RecyclerView smooth initially, lags after minutes — investigate
- [x] 4. Compose screen recomposes too frequently — identify and reduce
- [x] 5. Images in scrolling list, scrolling slow — optimize loading and caching
- [x] 6. App freezes parsing large JSON — identify and fix
- [x] 7. Large Room query blocks UI — optimize database access
- [x] 8. Excessive memory with thousands of items — investigate and reduce
- [x] 9. Deeply nested layouts render slowly — optimize view hierarchy
- [x] 10. Dropped frames on low-end devices, not reproducible on dev device
- [x] 11. Frequent real-time updates while scrolling — prevent unnecessary UI updates
- [x] 12. Startup time increased after adding SDKs — identify bottleneck
- [x] 13. Large image causes OutOfMemoryError — decoding and memory management
- [x] 14. Dashboard performs expensive calculations on every state change
- [x] 15. Measure and improve cold-start, frame rendering, memory in production

## Section 3 — Kotlin Coroutines, Flow and Concurrency → `kotlin-coroutines-flow-and-concurrency.md` ✅ DONE
- [x] 1. Two coroutines update the same shared variable — prevent race conditions
- [x] 2. Parallel requests, one fails — coroutineScope vs supervisorScope
- [x] 3. Coroutine cancelled mid long-running operation — ensure cleanup
- [x] 4. Two screens request the same API data — prevent duplicate network calls
- [x] 5. Shared cache accessed by multiple coroutines — make it thread-safe
- [x] 6. Cache for 30 seconds then fetch fresh
- [x] 7. StateFlow emits same value repeatedly — unnecessary UI updates
- [x] 8. SharedFlow events missed when screen in background — design event delivery
- [x] 9. viewModelScope cancelled but business operation must survive
- [x] 10. Several coroutines update a shared DB record atomically
- [x] 11. Flow pipeline performs expensive transformations
- [x] 12. Coroutine exception crashes the app — structured concurrency and exception handling
- [x] 13. Request deduplication for concurrent callers
- [x] 14. Mutex creates contention — is another strategy better?
- [x] 15. Cancellation-safe retries with exponential backoff and jitter

## Section 4 — Android Lifecycle and Process Death → `android-lifecycle-and-process-death.md` ✅ DONE
- [x] 1. Activity recreated on rotation mid-request — prevent duplicate requests
- [x] 2. Process killed in background — restore user's screen state
- [x] 3. User navigates away mid-request — continue or cancel?
- [x] 4. Configuration change causes duplicate StateFlow collectors
- [x] 5. Back pressed in multi-step form — preserve data without keeping Activity alive
- [x] 6. Deep link opens screen while app running — navigation and back stack
- [x] 7. Push notification arrives while app terminated — navigate correctly on tap
- [x] 8. Activity launches external payment app — handle process death in the result flow
- [x] 9. Dialog/bottom sheet reappears after configuration change
- [x] 10. remember vs rememberSaveable vs ViewModel vs SavedStateHandle vs persistent storage

## Section 5 — Offline-First Architecture and Database → `offline-first-architecture-and-database.md` ✅ DONE
- [x] 1. Display transaction history immediately even offline
- [x] 2. Same record modified locally and on server while offline
- [x] 3. Room migration fails for upgrading users
- [x] 4. Database with millions of records — queries, indexes, pagination, storage
- [x] 5. Duplicate synchronization events — idempotent processing
- [x] 6. Local update succeeds but server sync fails — recover
- [x] 7. Reliable synchronization queue surviving app restarts
- [x] 8. Multiple devices editing the same document — conflict resolution
- [x] 9. Offline file downloads with pause, resume, retry, integrity verification
- [x] 10. Server schema changes while older app versions remain in use
- [x] 11. Local DB transaction across several related tables — atomicity
- [x] 12. Offline-first repository using Room, Retrofit, Flow, WorkManager

## Section 6 — Jetpack Compose and UI Architecture → `jetpack-compose-and-ui-architecture.md` ✅ DONE
- [x] 1. Compose screen recomposes on unrelated state change
- [x] 2. LaunchedEffect launches API request multiple times
- [x] 3. LazyColumn loses scroll position after data refresh
- [x] 4. Composable collects a Flow but UI updates stop in background
- [x] 5. One-time navigation event emitted but never received by UI
- [x] 6. Complex UI state: loading, success, empty, error, pagination
- [x] 7. Prevent state updates reaching a screen after navigation away
- [x] 8. Expensive sorting and filtering on every recomposition
- [x] 9. Accessible, testable Compose screen with reusable components and UDF
- [x] 10. When to choose XML/RecyclerView over Compose — trade-offs
- [x] 11. Shared ViewModel across several navigation destinations — scope and lifecycle

## Section 7 — Memory Leaks, Stability and Crash Investigation → `memory-leaks-stability-and-crash-investigation.md` ✅ DONE
- [x] 1. Activity leaks memory after repeatedly opening/closing a screen
- [x] 2. Singleton retains a reference to an Activity
- [x] 3. Crash occurs only on Android 14+
- [x] 4. The app crashes intermittently with IllegalStateException in production, but you cannot reproduce it locally. What is your investigation strategy?
- [x] 5. Third-party SDK crashes on certain devices — isolate and protect
- [x] 6. Intermittent TransactionTooLargeException
- [x] 7. BroadcastReceiver or listener running after screen destroyed
- [x] 8. App works for hours then becomes slow and memory-hungry
- [x] 9. Investigate and prevent ANRs from DB access, locks, main-thread work

## Section 8 — Security, Authentication and Privacy → `security-authentication-and-privacy.md` ✅ DONE
- [x] 1. Access token stored locally — protect it
- [x] 2. Store sensitive documents offline — encryption, key management, secure access
- [x] 3. Certificate pinning without breaking rotation
- [x] 4. User logs out while API requests are running
- [x] 5. Prevent sensitive payment info in logs, analytics, crash reports
- [x] 6. WebView loads authenticated content — JS injection, unsafe navigation, token leakage
- [x] 7. Detect and respond to a compromised device without root detection alone
- [x] 8. Replay of an authenticated API request — client vs backend protections
- [x] 9. Secure session expiration and token refresh across concurrent requests

## Section 9 — Background Processing, Notifications and Media → `background-processing-notifications-and-media.md` ✅ DONE
- [x] 1. File upload must continue reliably in background
- [x] 2. WorkManager job fails repeatedly on unstable network
- [x] 3. Background sync job scheduled twice
- [x] 4. Push arrives while user views the relevant screen
- [x] 5. Video resumes from exact position even after process death
- [x] 6. Audio interrupted by call or another app — audio focus and lifecycle
- [x] 7. Large file download resumable after disconnect or restart
- [x] 8. Task must run at a particular time but Doze may delay it
- [x] 9. WorkManager vs foreground service vs regular coroutine for long-running work

## Section 10 — Testing, CI/CD and Release Engineering → `testing-cicd-and-release-engineering.md` ✅ DONE
- [x] 1. Payment feature works manually, fails intermittently in automated tests
- [x] 2. Test a ViewModel combining several Flows and concurrent API requests
- [x] 3. Test a timeout where server processes the request but the response never arrives
- [x] 4. Test passes individually, fails in full suite
- [x] 5. Production crash after release, rollback not immediately available
- [x] 6. Build works locally, fails in CI
- [x] 7. Introduce contract tests between Android app and backend team
- [x] 8. New release causes significant startup-time increase — detect and prevent regressions
- [x] 9. Roll out a risky feature gradually and disable it remotely
- [x] 10. Test DB migrations, backward compatibility, offline sync across upgrades

## Section 11 — Mobile System Design and Staff Engineer Scenarios → `mobile-system-design-and-staff-engineer-scenarios.md` ✅ DONE
- [x] 1. Design a banking transaction-history screen that supports millions of records, offline access, search, and filtering.
- [x] 2. Design a payment architecture that prevents duplicate charges despite timeouts, retries, process death, and concurrent requests.
- [x] 3. Design a common networking layer for 20 Android feature modules with authentication, retries, caching, logging, and error handling.
- [x] 4. Design a reusable offline-first synchronization framework for multiple features.
- [x] 5. An application has 100 feature modules and slow build times. How would you restructure the architecture and improve build performance?
- [x] 6. Two teams implement different networking and dependency-injection patterns. How would you standardize the architecture without blocking delivery?
- [x] 7. Your team wants to migrate a large XML-based application to Compose. How would you plan a safe, incremental migration?
- [x] 8. How would you design a feature-flag system that supports staged rollouts, emergency disabling, and offline behavior?
- [x] 9. How would you design observability for a mobile application to connect client-side failures with backend request traces?
- [x] 10. A critical production bug affects only 2% of users. How would you prioritize investigation, mitigate impact, and coordinate with backend and QA teams?
- [x] 11. Your team must reduce crash rate and improve app startup performance within one quarter. How would you establish measurable goals and prioritize work?
- [x] 12. A product manager requests a major feature halfway through a sprint, while the team is already committed to a critical release. How would you handle the trade-off?
- [x] 13. How would you introduce code reviews, automated testing, and architecture standards when you do not have direct authority over every developer?
- [x] 14. A monolithic Android application is becoming difficult to maintain as multiple teams contribute. How would you plan modularization?
- [x] 15. How would you decide whether a performance issue requires a local code optimization, an architectural change, or a backend redesign?

---

## Verification (final pass, 2026-10-22)

| Check | Result |
|---|---|
| Answer files present | 11 / 11 |
| `### ` answer headings across all files | **130** |
| Source numbered questions in `question_list.md` | **130** |
| Question text matches source verbatim | **130 / 130** (two headings corrected during the pass) |
| Every answer has *What the interviewer is really asking* | ✅ 130 |
| Every answer has *Key concepts* | ✅ 130 |
| Every answer has *Approach / architecture sketch* | ✅ 130 |
| Every answer has *Trade-offs* table | ✅ 130 (min 3 rows, max 11) |
| Every answer has *Talking points / follow-ups* | ✅ 130 |
| Kotlin code block per answer | 129 (Section 11 omits code by design — design/staff scenarios only) |
| Each file ends with `## Section Summary — Staff-Level Themes` | ✅ 11 / 11 |
| `question_list.md` unmodified | ✅ |

**Answer template used throughout:**
1. Question (verbatim) → 2. *What the interviewer is really asking* → 3. *Key concepts* → 4. *Approach* → 5. *Trade-offs* (table) → 6. *Code example* (Kotlin) → 7. *Talking points / follow-ups*.
