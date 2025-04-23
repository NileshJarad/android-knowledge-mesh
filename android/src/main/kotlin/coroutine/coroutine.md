# Coroutine


Coroutines are a powerful feature in modern programming languages that allow for asynchronous and concurrent programming

>A coroutine is an instance of suspendable computation. It is conceptually similar to a thread, in the sense that it takes a block of code to run that works concurrently with the rest of the code. However, a coroutine is not bound to any particular thread. It may suspend its execution in one thread and resume in another one.

Coroutines are a lightweight alternative to threads.


## Suspended function
>Suspended function is function that could be started, paused, and resume. One of the most important points to remember about the suspend function is that they are only allowed to be called from a coroutine or another suspend function
 
When a coroutine is suspended, that thread is free for other coroutines. The continuation of the coroutine doesn't have to be on the same thread. Here we conclude that we can simultaneously run many coroutines with a small number of threads.


## Coroutine Builder

### launch

- It is typically used when you want to fire off a coroutine and don't need to wait for its result immediately.
- Launches a new coroutine without blocking the current thread and returns a reference to the coroutine as a `Job`.
    ```kotlin
    import kotlinx.coroutines.*
    
    fun main() {
        println("Start")
        
        // Launch a coroutine
        val job = GlobalScope.launch {
            delay(1000)
            println("Coroutine executed")
        }
        
        // Do some other work while the coroutine is running
        println("Do some other work")
        
        // Wait for the coroutine to finish
        runBlocking {
            job.join()
        }
        
        println("End")
    }
    
    ```

### async

- This coroutine builder is used when you want to perform a computation asynchronously and obtain a result.
- The `async` builder returns a `Deferred` object that represents the result of the computation
    ```kotlin
    import kotlinx.coroutines.*
    
    fun main() = runBlocking {
        println("Start")
    
        // Start two coroutines asynchronously
        val deferred1 = async {
            delay(1000)
            "Result from coroutine 1"
        }
        val deferred2 = async {
            delay(2000)
            "Result from coroutine 2"
        }
    
        // Do some other work while the coroutines are running
        println("Do some other work")
    
        // Wait for the coroutines to complete and retrieve the results
        val result1 = deferred1.await()
        val result2 = deferred2.await()
    
        println("Results: $result1, $result2")
        println("End")
    }
    
    ```

### runBlocking

- This coroutine builder is used to create a new coroutine and block the current thread until the coroutine completes.
- It is typically used in top-level code, such as the main function, to bridge non-coroutine code with coroutine-based
  code.
    ```kotlin
    import kotlinx.coroutines.*
    
    fun main() = runBlocking {
        println("Start")
    
        // Start a coroutine using runBlocking
        runBlocking {
            delay(1000)
            println("Coroutine executed")
        }
    
        println("End")
    }
    
    ```

---

## Coroutine Dispatchers

- The coroutine context includes a coroutine dispatcher that determines what thread or threads the corresponding
  coroutine uses for its execution.
- The coroutine dispatcher can confine coroutine execution to a specific thread, dispatch it to a thread pool, or let it
  run unconfined.
- All coroutine builders like `launch` and `async` accept an optional `CoroutineContext` parameter that can be used to
  explicitly specify the dispatcher for the new coroutine and other context elements.
- [Dispatchers Example Code](DispatchersExample.kt)

### Dispatchers.Default

- This is default dispatcher is used when no other dispatcher is explicitly specified in the scope.
- It used shared background pool of threads

### newSingleThreadContext

- Create a thread for the coroutine to run
- Dedicated thread is very expensive resource.
- This should be closed using `close`  when no longer in use or should be keep at top level throughout the application

### Dispatchers.Unconfined

- A coroutine dispatcher that is not confined to any specific thread.
- It starts a coroutine in the caller thread, but only until the first suspension point.
- The unconfined dispatcher is appropriate for coroutines which neither consume CPU time nor update any shared data (
  like UI) confined to a specific thread.
- It executes the initial continuation of a coroutine in the current call-frame and lets the coroutine resume in
  whatever thread that is used by the corresponding suspending function, without mandating any specific threading
  policy.
    - [Nested coroutines launched in this dispatcher form an event-loop to avoid stack overflows](UnConfinedEventLoop.kt)
        - Event loop semantics is a purely internal concept and has no guarantees on the order of execution except that
          all queued coroutines will be executed on the current thread in the lexical scope of the outermost unconfined
          coroutine.
          ```kotlin
            fun main() = runBlocking { 
                withContext(Dispatchers.Unconfined) { 
                    println(1)
                    launch(Dispatchers.Unconfined) {
                        println(2)
                     }
                     println(3)
                }
                println("Done")
            }
          ```
        - Can print both "1 2 3" and "1 3 2".
        - This is an implementation detail that can be changed. However, it is guaranteed that **Done** will only be
          printed once the code in both `withContext` and `launch` completes.
    - [Unconfined example](UnConfinedDispatcher.kt)

---

## Coroutine Scope

- It defines a context in which coroutines are executed and provides a way to manage the lifecycle of coroutines.
- Coroutine scope is typically used to launch coroutines and ensure their proper cancellation.
- CoroutineScope is an interface with only one property `coroutineContext`
- Every **coroutine builder** is extension of **CoroutineScope**
- **CoroutineScope** has two main builders `launch` and `async`
- To create a coroutine scope, you can use the `coroutineScope` or `supervisorScope` function
- [Coroutine Scope example](CoroutineScopeExample.kt)

### coroutineScope

- Function creates a new child scope, and the cancellation of this scope will propagate to all child coroutines

### supervisorScope

- function creates a new child scope but provides a supervisor job, allowing child coroutines to fail independently
  without canceling the whole scope.

**Difference table**

|                       | `coroutineScope`                                                                   | `supervisorScope`                                                                      |
|-----------------------|------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------|
| Error Propagation     | Propagates exceptions to its parent scope                                          | Does not propagate exceptions to its parent scope                                      |
| Cancellation          | Cancels all child coroutines on cancellation                                       | Does not cancel other child coroutines on cancellation                                 |
| Completion            | Waits for all child coroutines to complete                                         | Does not wait for all child coroutines to complete                                     |
| Parent Responsibility | Parent is responsible for handling exceptions and cancellation of child coroutines | Parent is not responsible for handling exceptions and cancellation of child coroutines |
| Code Example          | [Example code](CoroutineScopeInBuildExample.kt)                                    | [Example Code](SupervisorScopeExample.kt)                                              |


---- 

1. What are Kotlin Coroutines, and why are they used?