# Pending


## Network, API and Distributed Systems
1. A payment request times out, but the server may have processed the payment. How would you prevent duplicate charges?
2. The user kills the app during a payment and reopens it later. How would you recover the payment state?
3. An API request is triggered twice because the user double-taps a button. How would you prevent duplicate operations?
4. Two search API requests return responses out of order. How would you ensure that stale data doesn't overwrite the latest results?
5. A screen depends on five APIs, but only three succeed. How would you handle partial API failures?
6. The network switches from Wi-Fi to mobile data while an API request is in progress. How would you handle the request?
7. An API takes 30 seconds to respond. How would you design the loading, timeout, cancellation, and retry behavior?
8. The API returns HTTP 401 while multiple requests are running concurrently. How would you refresh the access token without triggering multiple refresh requests?
9. The access token expires while a file upload is in progress. How would you recover without uploading the file twice?
10. The app must work offline and synchronize data when connectivity returns. How would you design the architecture?
11. An API returns HTTP 429 because of rate limiting. How would you handle retries?
12. The server processes a request, but the client cancels the coroutine before receiving the response. What happens, and how would you handle it?
13. The backend occasionally returns inconsistent data across multiple endpoints. How would you maintain a consistent UI?
14. A user starts an operation on one device and continues on another. How would you handle synchronization and conflicting updates?
15. How would you design a centralized network error-handling mechanism for the entire application?
## Performance, Large Datasets and UI Rendering
1. An API returns 1,000 transactions. How would you display them without making the UI lag?
2. An API returns 100,000 records. How would you design the data-loading and rendering strategy?
3. A RecyclerView scrolls smoothly initially but starts lagging after several minutes. How would you investigate the issue?
4. A Jetpack Compose screen recomposes too frequently. How would you identify and reduce unnecessary recompositions?
5. A screen displays images in a scrolling list, and scrolling becomes slow. How would you optimize image loading and caching?
6. The app freezes while parsing a large JSON response. How would you identify and fix the problem?
7. A large Room database query blocks the UI. How would you optimize database access?
8. The application consumes excessive memory when displaying thousands of items. How would you investigate and reduce memory usage?
9. A screen contains deeply nested layouts and takes too long to render. How would you optimize its view hierarchy?
10. A user reports dropped frames on low-end Android devices, but the issue cannot be reproduced on your development device. How would you investigate it?
11. A list receives frequent real-time updates while the user scrolls. How would you prevent unnecessary UI updates?
12. The app's startup time increases significantly after adding several SDKs. How would you identify the bottleneck?
13. A large image causes an OutOfMemoryError. How would you handle image decoding and memory management?
14. A dashboard performs multiple expensive calculations whenever its state changes. How would you optimize it?
15. How would you measure and improve cold-start time, frame rendering, and memory usage in a production Android app?
## Kotlin Coroutines, Flow and Concurrency
1. Two coroutines update the same shared variable simultaneously. How would you prevent race conditions?
2. Multiple API requests need to run in parallel, but one request fails. How would you decide between coroutineScope and supervisorScope?
3. A coroutine is cancelled while executing a long-running operation. How would you ensure proper cleanup?
4. Two screens request the same API data simultaneously. How would you prevent duplicate network calls?
5. A shared cache is accessed by multiple coroutines. How would you make it thread-safe?
6. An API should return cached data for 30 seconds and fetch fresh data afterward. How would you implement this?
7. A StateFlow emits the same value repeatedly, causing unnecessary UI updates. How would you handle it?
8. Events emitted through SharedFlow are missed when the screen is in the background. How would you design event delivery?
9. A coroutine launched in viewModelScope is cancelled when the ViewModel is cleared, but the business operation must survive. How would you handle it?
10. Several coroutines need to update a shared database record atomically. How would you prevent inconsistent updates?
11. A Flow pipeline performs expensive transformations and causes performance problems. How would you optimize its execution?
12. A coroutine exception crashes the application unexpectedly. How would you design structured concurrency and exception handling?
13. How would you implement request deduplication for concurrent callers using Kotlin Coroutines?
14. A Mutex protects shared state but creates contention. How would you identify whether another synchronization strategy is more appropriate?
15. How would you implement cancellation-safe retries with exponential backoff and jitter?
## Android Lifecycle and Process Death
1. An Activity is recreated during screen rotation while an API request is running. How would you prevent unnecessary duplicate requests?
2. The Android process is killed while the app is in the background. How would you restore the user's screen state?
3. A user navigates away from a screen while a network request is in progress. Should the request continue or be cancelled?
4. A configuration change causes duplicate collectors for a StateFlow. How would you prevent them?
5. A user presses Back during a multi-step form. How would you preserve entered data without keeping the Activity alive?
6. A deep link opens a screen while the app is already running. How would you handle navigation and existing back-stack entries?
7. An app receives a push notification while it is terminated. How would you navigate to the correct screen when the user taps it?
8. An Activity launches an external payment application and later receives a result. How would you handle process death during that flow?
9. A dialog or bottom sheet reappears unexpectedly after configuration changes. How would you manage its state?
10. How would you distinguish state that belongs in remember, rememberSaveable, ViewModel, SavedStateHandle, and persistent storage?
## Offline-First Architecture and Database
1. An app must display transaction history immediately, even when the network is unavailable. How would you design the data flow?
2. The same record is modified locally and on the server while the device is offline. How would you resolve the conflict?
3. A Room database migration fails for users upgrading from an older app version. How would you investigate and recover?
4. A database contains millions of records. How would you optimize queries, indexes, pagination, and storage?
5. The app receives duplicate synchronization events. How would you ensure that processing them is idempotent?
6. Local data is updated successfully, but the subsequent server synchronization fails. How would you recover?
7. How would you implement a reliable synchronization queue that survives app restarts?
8. The application must support multiple devices editing the same document. How would you handle conflict resolution?
9. How would you implement offline file downloads with pause, resume, retry, and integrity verification?
10. The server schema changes while older app versions remain in use. How would you maintain backward compatibility?
11. A local database transaction involves several related tables. How would you ensure atomicity?
12. How would you design an offline-first repository using Room, Retrofit, Flow, and WorkManager?
## Jetpack Compose and UI Architecture
1. A Compose screen recomposes whenever an unrelated state variable changes. How would you identify the cause?
2. A LaunchedEffect unexpectedly launches an API request multiple times. How would you fix it?
3. A LazyColumn loses the user's scroll position after a data refresh. How would you preserve it?
4. A composable collects a Flow, but UI updates stop when the app enters the background. How would you investigate it?
5. A one-time navigation event is emitted but never received by the UI. How would you design event handling?
6. A screen has complex UI state involving loading, success, empty, error, and pagination. How would you model it?
7. How would you prevent state updates from reaching a screen after the user has navigated away?
8. A composable performs expensive sorting and filtering on every recomposition. How would you optimize it?
9. How would you implement an accessible, testable Compose screen with reusable components and unidirectional data flow?
10. When would you choose XML with RecyclerView over Jetpack Compose, and what trade-offs would you consider?
11. A shared ViewModel is used across several navigation destinations. How would you determine its appropriate lifecycle and scope?
## Memory Leaks, Stability and Crash Investigation
1. An Activity leaks memory after the user repeatedly opens and closes a screen. How would you identify and fix the leak?
2. A singleton retains a reference to an Activity. Why is this dangerous, and how would you redesign the dependency?
3. A crash occurs only on Android 14 and later. How would you investigate platform-specific behavior?
4. The app crashes intermittently with IllegalStateException in production, but you cannot reproduce it locally. What is your investigation strategy?
5. A third-party SDK causes crashes on certain devices. How would you isolate the issue and protect the application?
6. The app occasionally crashes with TransactionTooLargeException. What could cause it, and how would you resolve it?
7. A BroadcastReceiver or listener continues running after the associated screen is destroyed. How would you manage its lifecycle?
8. The app works for hours but eventually becomes slow and consumes more memory. How would you diagnose the issue?
9. How would you investigate and prevent ANRs caused by database access, locks, or expensive main-thread operations?
## Security, Authentication and Privacy
1. An access token is stored locally. How would you protect it against unauthorized access?
2. The app must store sensitive documents offline. How would you design encryption, key management, and secure file access?
3. How would you implement certificate pinning without making the app unusable when certificates or public keys rotate?
4. A user logs out while API requests are still running. How would you prevent stale requests from accessing or updating authenticated data?
5. How would you prevent sensitive payment information from appearing in logs, analytics, or crash reports?
6. A WebView loads authenticated content. How would you protect it against JavaScript injection, unsafe navigation, and token leakage?
7. How would you detect and respond to a compromised device without relying exclusively on root detection?
8. An attacker replays an authenticated API request. Which protections belong in the client, backend, or both?
9. How would you implement secure session expiration and token refresh across multiple concurrent requests?
## Background Processing, Notifications and Media
1. A file upload must continue reliably when the app moves to the background. How would you design it?
2. A WorkManager job fails repeatedly because the network is unstable. How would you configure retries and constraints?
3. A background synchronization job is scheduled twice. How would you avoid duplicate work?
4. A push notification arrives while the user is already viewing the relevant screen. How would you avoid duplicate navigation or redundant data loading?
5. A video must resume from the exact position where the user stopped, even after process death. How would you implement it?
6. Audio playback is interrupted by a phone call or another audio application. How would you handle audio focus and lifecycle changes?
7. A large file download must support resuming after a network disconnect or device restart. How would you design it?
8. A background task must run at a particular time, but Android may delay execution due to Doze or battery restrictions. How would you approach scheduling?
9. How would you choose between WorkManager, a foreground service, and a regular coroutine for long-running work?
##  Testing, CI/CD and Release Engineering
1. A payment feature works manually but fails intermittently in automated tests. How would you make the tests deterministic?
2. How would you test a ViewModel that combines several Flows and concurrent API requests?
3. How would you test a timeout scenario where the server processes a request but the response never reaches the app?
4. A test passes individually but fails when the full test suite runs. How would you investigate shared state, dispatchers, and test isolation?
5. A production crash occurs after a release, but rollback is not immediately available. How would you mitigate the impact?
6. A build works locally but fails in CI. How would you investigate dependency resolution, build configuration, and environment differences?
7. How would you introduce contract tests between an Android app and a backend team?
8. A new release causes a significant increase in app startup time. How would you detect and prevent performance regressions?
9. How would you roll out a risky feature gradually and disable it remotely if problems occur?
10. How would you test database migrations, backward compatibility, and offline synchronization across app upgrades?
## Mobile System Design and Staff Engineer Scenarios
1. Design a banking transaction-history screen that supports millions of records, offline access, search, and filtering.
2. Design a payment architecture that prevents duplicate charges despite timeouts, retries, process death, and concurrent requests.
3. Design a common networking layer for 20 Android feature modules with authentication, retries, caching, logging, and error handling.
4. Design a reusable offline-first synchronization framework for multiple features.
5. An application has 100 feature modules and slow build times. How would you restructure the architecture and improve build performance?
6. Two teams implement different networking and dependency-injection patterns. How would you standardize the architecture without blocking delivery?
7. Your team wants to migrate a large XML-based application to Compose. How would you plan a safe, incremental migration?
8. How would you design a feature-flag system that supports staged rollouts, emergency disabling, and offline behavior?
9. How would you design observability for a mobile application to connect client-side failures with backend request traces?
10. A critical production bug affects only 2% of users. How would you prioritize investigation, mitigate impact, and coordinate with backend and QA teams?
11. Your team must reduce crash rate and improve app startup performance within one quarter. How would you establish measurable goals and prioritize work?
12. A product manager requests a major feature halfway through a sprint, while the team is already committed to a critical release. How would you handle the trade-off?
13. How would you introduce code reviews, automated testing, and architecture standards when you do not have direct authority over every developer?
14. A monolithic Android application is becoming difficult to maintain as multiple teams contribute. How would you plan modularization?
15. How would you decide whether a performance issue requires a local code optimization, an architectural change, or a backend redesign?