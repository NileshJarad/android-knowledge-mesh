# Service

## What is a Service in Android?

A Service is an Android component that runs in the background to perform long-running operations.

**Key Points:**

- No UI (unlike Activities).
- Useful for downloading files, playing music, handling network calls, etc.
- Keeps running even if the app is closed (depending on the type).
- By default, Service runs on **Main** thread

## Types of Services

### Started Service

- Started using startService() or ContextCompat.startForegroundService().
- Runs indefinitely until you stop it.
- **START_STICKY** - Restarts the service if the system kills it.
- **START_NOT_STICKY** - Doesn’t restart service unless explicitly started again.
-

**Create a Kotlin class extending Service**

```kotlin
class MyStartedService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Do your long-running work here
        Log.d("MyStartedService", "Service Started")
        return START_STICKY // 	Restarts the service if the system kills it.
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Not used in started service
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MyStartedService", "Service Destroyed")
    }
}

```

**Register in AndroidManifest.xml:**

```xml

<service android:name=".MyStartedService"/>
```

**Start and stop the service:**

```kotlin
val intent = Intent(this, MyStartedService::class.java)
startService(intent) // Start

stopService(intent) // Stop

```

### Bound Service

- Bound using bindService().
- Lives as long as another component is bound to it.
- Good for client-server communication within the same app.
- Used when you want components (like an activity) to bind to the service and interact.

```kotlin
class MyBoundService : Service() {

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): MyBoundService = this@MyBoundService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    fun performAction(): String {
        return "Action performed!"
    }
}
```

**Activity code**

```kotlin
class MyActivity : AppCompatActivity() {
    private var service: MyBoundService? = null
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = (binder as MyBoundService.LocalBinder).getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, MyBoundService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}

```

### Foreground Service

- Displays a notification and has higher priority.
- Required for long tasks on Android 8.0+.
- **startForeground()** - Promotes a service to foreground to prevent it from being killed.

```kotlin
class MyForegroundService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, "channelId")
            .setContentTitle("Foreground Service")
            .setContentText("Running in background")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(1, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

```

### AIDL (Android Interface Definition Language)

It’s a way to create Inter-Process Communication (IPC) between different apps or different processes in Android.

**Why Use AIDL?**

Normally, a Bound Service allows components to bind and talk to it — but only within the same process.

If you want:

- An app (or process) to talk to a service in another app (or process)
- To share complex data across apps
- To make a cross-app service API

➡️ **_You need AIDL_**

**How AIDL Works (Under the Hood)**

- You define an `.aidl` file → This is the interface
- Android generates a Binder Stub → Handles IPC for you
- You implement the service and the interface methods
- Client apps bind to your service using bindService()
- You exchange primitive data or Parcelable objects

**Example: Create AIDL Service**

Step 1: Define an AIDL interface
`IMathService.aidl` in `src/main/aidl/com/example/aidlservice/`

```aidl
package com.example.aidlservice;

interface IMathService {
    int add(int a, int b);
}

```

Supported types:

- `int`, `long`, `float`, `double`, `boolean`, `String`
- `List`, `Map`
- Parcelable objects

Step 2: Implement the Service

```kotlin
class MathService : Service() {

    private val binder = object : IMathService.Stub() {
        override fun add(a: Int, b: Int): Int = a + b
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
}
```

Step 3: Register Service in AndroidManifest.xml

```xml

<service
        android:name=".MathService"
        android:exported="true"
        android:enabled="true">
    <intent-filter>
        <action android:name="com.example.aidlservice.IMathService"/>
    </intent-filter>
</service>
```

Step 4: Bind From Another App (Client)

- Add the same `.aidl` file to the client app
- Bind to the service like this:

```kotlin
class ClientActivity : AppCompatActivity() {

    private var mathService: IMathService? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            mathService = IMathService.Stub.asInterface(binder)
            val result = mathService?.add(10, 20)
            Log.d("Client", "Result from service: $result")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mathService = null
        }
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent("com.example.aidlservice.IMathService")
        intent.setPackage("com.example.aidlservice") // Package of service
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        unbindService(connection)
    }
}

```

**Since Android 10+, bound services with AIDL must be explicitly exported and may need permissions for security.**



---

# Work Manager

WorkManager is part of Android Jetpack and is designed to perform deferrable, asynchronous tasks that are guaranteed to
execute, even if the app exits or the device restarts.

**It's best for background tasks that:**

- Should run even if the app process dies
- Are guaranteed to be executed
- Can be delayed or scheduled with constraints

**Examples:**

- Syncing data
- Uploading logs
- Periodic backups

## Types of Work

### OneTimeWorkRequest

Runs the work once.

```kotlin
val request = OneTimeWorkRequestBuilder<MyWorker>().build()
WorkManager.getInstance(context).enqueue(request)
```

### PeriodicWorkRequest

Runs the work repeatedly at defined intervals (minimum 15 minutes).

```kotlin
val periodicRequest = PeriodicWorkRequestBuilder<MyWorker>(15, TimeUnit.MINUTES).build()
WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "MyPeriodicWork",
    ExistingPeriodicWorkPolicy.KEEP,
    periodicRequest
)
```

### UniqueWorkRequest

Avoids duplication. You can define unique names and policies like:

- KEEP
- REPLACE
- APPEND

```kotlin
WorkManager.getInstance(context).enqueueUniqueWork(
    "uniqueName",
    ExistingWorkPolicy.KEEP,
    OneTimeWorkRequestBuilder<MyWorker>().build()
)

```

## Chaining and Constraints

You can chain multiple workers using .then() and add constraints like:

- Network type
- Charging state
- Battery not low
- Storage not low
- Device idle (API 23+)

```kotlin
val constraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .setRequiresCharging(true)
    .build()

val request = OneTimeWorkRequestBuilder<MyWorker>()
    .setConstraints(constraints)
    .build()

WorkManager.getInstance(context).enqueue(request)

```

## Combine worker

**Define Your Workers**

```kotlin
class UploadWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        Log.d("UploadWorker", "Uploading file...")
        // Simulate work
        Thread.sleep(1000)
        return Result.success()
    }
}

class CompressWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        Log.d("CompressWorker", "Compressing file...")
        Thread.sleep(1000)
        return Result.success()
    }
}

class NotifyWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        Log.d("NotifyWorker", "Sending notification...")
        Thread.sleep(1000)
        return Result.success()
    }
}

```

**Enqueue Workers Using `.then()`**

```kotlin
val uploadWork = OneTimeWorkRequestBuilder<UploadWorker>().build()
val compressWork = OneTimeWorkRequestBuilder<CompressWorker>().build()
val notifyWork = OneTimeWorkRequestBuilder<NotifyWorker>().build()

WorkManager.getInstance(context)
    .beginWith(uploadWork)
    .then(compressWork)
    .then(notifyWork)
    .enqueue()

```

**Tips**

- If any worker returns `Result.failure()`, the chain stops.
- You can also create parallel work by combining `List<WorkRequest>` in .then().
- Use `beginWith()` to start with multiple workers in parallel if needed.
- Chain `.then()` to enforce sequential execution.
- Monitor each job via WorkManager.getWorkInfoByIdLiveData(...).

----

# Questions

**Q.** What happens if you don’t call startForeground() within 5 seconds of startForegroundService()?
> Android will crash your app with an IllegalStateException on Android 8+.
>

**Q.** Can a BoundService also be started as a StartedService? What happens then?
> Yes, a service can be both bound and started. It’ll only stop when:
> All clients unbind
> ou explicitly call stopSelf()

**Q.** What’s the risk of returning START_REDELIVER_INTENT in onStartCommand() in a service that downloads a file?
> The system will re-deliver the same intent, potentially downloading the same file twice if you're not careful.

**Q.** Can a Service run indefinitely in the background without being foreground or bound?
>Nope. Since Android 8+, background services will be stopped by the system unless promoted to foreground (with
>notification).

**Q.** Is it safe to perform heavy work directly in a service?
> No. Services run on the main thread by default. Heavy work should use Thread, HandlerThread, Coroutine, or
> WorkManager.

**Q.** How would you design a service to continue location tracking across app restarts, device reboots, and Android
power constraints?
> Foreground service with location type
>
>BOOT_COMPLETED receiver
>
>Exclusion from battery optimizations (request from user)
>
>Proper handling of Doze Mode

**Q.** Explain how you’d handle communication between an Activity and a Service (bi-directional)
> Techniques include:
>
>AIDL
>
>Messenger (with Handler)
>
>Local BroadcastReceiver
>
>LiveData via a bound service

**Q.** How can you prevent a foreground service from being killed if the user removes the app from Recent Apps?
> Make it foreground
>
>Return START_STICKY
>
>Use onTaskRemoved() to restart service

## Cheat Sheet

## 📊 Android Background Task Mechanisms Comparison

| Use Case                       | Foreground Service | Background Service | WorkManager        | AlarmManager   | AIDL (IPC)                  |
|--------------------------------|--------------------|--------------------|--------------------|----------------|-----------------------------|
| 🔄 Run Immediately             | ✅ Yes              | ⚠️ Limited         | ❌ No               | ❌ No           | ⚠️ Only for IPC use         |
| ⏳ Deferred Execution           | ❌ Not suitable     | ⚠️ Risky           | ✅ Best choice      | ❌ Not designed | ❌ Not for scheduling        |
| 🕒 Scheduled at Fixed Time     | ⚠️ Not ideal       | ❌ No               | ⚠️ Approximate     | ✅ Best suited  | ❌                           |
| 🔁 Repeated at Fixed Intervals | ❌                  | ❌                  | ✅ ≥15 mins         | ✅ Yes          | ❌                           |
| 📶 Works with Constraints      | ❌                  | ❌                  | ✅ Yes              | ❌              | ❌                           |
| 🔋 Battery Optimized           | ❌ No               | ❌ No               | ✅ Yes              | ❌ No           | ❌                           |
| 🔒 Survives Reboot             | ⚠️ Needs handling  | ❌ No               | ✅ Yes (limited)    | ✅ Yes          | ❌                           |
| 🔧 Runs in Background          | ✅ Yes              | ⚠️ Risky on 8+     | ✅ Yes              | ✅ Yes          | ⚠️ For background IPC       |
| 🧠 Use When                    | Real-time tasks    | Legacy use only    | Deferred, reliable | Timed events   | Cross-process communication |


