package coroutine

import kotlinx.coroutines.*

fun main() = runBlocking {
    println("Main coroutine starts")

    coroutineScope {
        launch {
            delay(500)
            println("Coroutine 1 executed")
        }

        launch {
            delay(1000)
            throw Exception("Coroutine 2 failed")
        }

        launch {
            delay(1500)
            throw Exception("Coroutine 3 executing")
        }
    }

    println("Main coroutine ends")
}


/****
Main coroutine starts
Coroutine 1 executed
Exception in thread "main" java.lang.Exception: Coroutine 2 failed
 at FileKt$main$1$1$2.invokeSuspend (File.kt:15) 
 at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith (ContinuationImpl.kt:33) 
 at kotlinx.coroutines.DispatchedTaskKt.resume (DispatchedTask.kt:235) 
***/
