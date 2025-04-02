# Thread, Handler, Looper , Executor, Message Queue


## Thread

A thread is a unit of execution within a process. 
In Android, the main thread (UI thread) is responsible for handling UI updates and interactions. Running long tasks (e.g., network requests, database operations) on the main thread can lead to ANR (Application Not Responding) errors.

Threads in Android can be created using the `Thread` class or `Runnable` interface.

**Example**

```kotlin
class MyThread : Thread() {
    override fun run() {
        // Background task
        for (i in 1..5) {
            println("Thread running: $i")
            Thread.sleep(1000) // Simulating work
        }
    }
}

val thread = MyThread()
thread.start()

```

or

```kotlin
val runnable = Runnable {
    for (i in 1..5) {
        println("Runnable running: $i")
        Thread.sleep(1000)
    }
}

val thread = Thread(runnable)
thread.start()

```

## Handler
A Handler allows you to send and process Message and Runnable objects associated with a thread's MessageQueue. 

Each Handler instance is associated with a single thread and that thread's message queue. When you create a new Handler it is bound to a Looper. 

It will deliver messages and runnables to that Looper's message queue and execute them on that Looper's thread.


There are two main uses for a Handler: 
1. to schedule messages and runnables to be executed at some point in the future; and 
2. to enqueue an action to be performed on a different thread than your own.


## Looper
Class used to run a message loop for a thread. Threads by default do not have a message loop associated with them; to create one, call prepare() in the thread that is to run the loop, and then loop() to have it process messages until the loop is stopped.

Most interaction with a message loop is through the [Handler](#handler) class.

This is a typical example of the implementation of a Looper thread, using the separation of prepare() and loop() to create an initial Handler to communicate with the Looper.

```kotlin
internal class LooperThread : Thread() {
    var mHandler: Handler? = null

    override fun run() {
        Looper.prepare() // initialise Message queue for thread

        mHandler = object : Handler(Looper.myLooper()!!) {
            override fun handleMessage(msg: Message) {
                // process incoming messages here
            }
        }

        Looper.loop() //  enters an infinite loop and starts waiting for messages in the MessageQueue
    }
}
```


## Message Queue

Low-level class holding the list of messages to be dispatched by a [Looper](#looper). 
Messages are not added directly to a `MessageQueue`, but rather through [Handler](#handler) objects associated with the [Looper](#looper).

You can retrieve the `MessageQueue` for the current thread with `Looper.myQueue()`.



Example for Thread , Handler, Looper and Message Queue

**Creation of Thread**
```kotlin
        // Start the LooperThread
        val looperThread = LooperThread()
        looperThread.start()

        // Wait for thread to be ready
        Thread.sleep(500)

        // Post a delayed stop after 5 seconds
        looperThread.mHandler?.postDelayed({
            Looper.myLooper()?.quit()
            Log.d("LooperThread","Looper stopped!")
        }, 5000)


        // send message to handler using 500 ms delay
        looperThread.mHandler?.post {
            for (i in 1..50) {
                val msg = Message.obtain()
                msg.what = i
                looperThread.mHandler?.sendMessage(msg)
                Thread.sleep(500)
            }
        }
```



```kotlin
   internal class LooperThread : Thread() {
        var mHandler: Handler? = null

        override fun run() { 
            Looper.prepare() // initialise Message queue for thread

            mHandler = object : Handler(Looper.myLooper()!!) {
                override fun handleMessage(msg: Message) {
                    Log.d("LooperThread","Processing message: ${msg.what}")
                }
            }

            Looper.loop() //  enters an infinite loop and starts waiting for messages in the MessageQueue
        }
    }
```


**Do not call Looper.loop() Before Creating a Handler**

```kotlin
internal class LooperThread : Thread() {
    var mHandler: Handler? = null

    override fun run() {
        Looper.prepare()

        // Calling Looper.loop() before creating a Handler
        Looper.loop()  // ❌ Thread is stuck here! The handler will never be created.

        mHandler = object : Handler(Looper.myLooper()!!) {
            override fun handleMessage(msg: Message) {
                println("Processing message: ${msg.what}")
            }
        }
    }
}

val looperThread = LooperThread()
looperThread.start()

// Trying to post a message (This will fail!)
Thread.sleep(500)
looperThread.mHandler?.sendMessage(Message.obtain().apply { what = 1 })

```


## Executor

### 🔹 Types of Executors in Android

An Executor is a higher-level thread management framework that simplifies background task execution. It is part of the Java Concurrency API (java.util.concurrent.Executor).

Instead of manually creating and managing threads, an Executor efficiently handles thread pooling and task scheduling.

| Executor Type               | Description                                             | Usage                                          |
|-----------------------------|---------------------------------------------------------|------------------------------------------------|
| `newSingleThreadExecutor()` | One background thread for all tasks.                    | Serial execution, background database updates. |
| `newFixedThreadPool(n)`     | Pool of `n` threads.                                    | Parallel execution of tasks.                   |
| `newCachedThreadPool()`     | Creates threads as needed and reuses idle threads.      | Dynamic workloads with short tasks.            |
| `newScheduledThreadPool(n)` | Runs tasks **after a delay** or **at fixed intervals**. | Periodic tasks, scheduled background sync.     |

### ✅ Explanation:
- **`newSingleThreadExecutor()`** → Useful for sequential execution (one task at a time).
- **`newFixedThreadPool(n)`** → Good for parallel execution, limits the number of threads.
- **`newCachedThreadPool()`** → Best for dynamic workloads (threads created as needed).
- **`newScheduledThreadPool(n)`** → Used for periodic or delayed execution.

### ✅ Example: Using `Executor` to Run Background Tasks
```kotlin
import java.util.concurrent.Executors

val executor = Executors.newSingleThreadExecutor()

executor.execute {
    println("Running task in background thread: ${Thread.currentThread().name}")
}
```

#### 🔹 How It Works:
- `Executors.newSingleThreadExecutor()` creates an **Executor with a single background thread**.
- `execute {}` submits a **runnable task** to the executor.
- The task runs **asynchronously** on a separate thread.

### ✅ Example: Running Multiple Background Tasks
```kotlin
val executor = Executors.newFixedThreadPool(3)

repeat(5) { taskNumber ->
    executor.execute {
        println("Task $taskNumber running on ${Thread.currentThread().name}")
    }
}
```

#### 🔹 Expected Output (Thread Names May Vary)
```
Task 0 running on pool-1-thread-1
Task 1 running on pool-1-thread-2
Task 2 running on pool-1-thread-3
Task 3 running on pool-1-thread-1
Task 4 running on pool-1-thread-2
```

#### 🔹 Explanation:
- A **pool of 3 threads** is created.
- The first **3 tasks** run in parallel on **separate threads**.
- Remaining tasks reuse the available threads.

### ✅ Example: Scheduled Executor (Task Runs Every 2 Seconds)
```kotlin
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

val scheduler = Executors.newScheduledThreadPool(1)

scheduler.scheduleAtFixedRate({
    println("Task running at: ${System.currentTimeMillis()}")
}, 0, 2, TimeUnit.SECONDS)
```

#### 🔹 Explanation:
- The task **runs every 2 seconds**.
- `scheduleAtFixedRate()` ensures the **same interval between executions**.

### ❌ When NOT to Use Executors?
1. **Long-running background tasks** → Use **WorkManager**.
2. **Simple one-time tasks** → Use **Kotlin Coroutines** instead.
3. **UI Updates** → Executors run in the background, so you need to **use `Handler` or `MainThread` to update UI**.

### 🔹 Conclusion
✔ **Executors are better than manually managing threads.**  
✔ **Use thread pools for better performance.**  
✔ **Choose the right Executor type for your use case.**  
✔ **Prefer Kotlin Coroutines for modern async programming.**


