package coroutine

import kotlinx.coroutines.*

fun main() = runBlocking {
    println("Main coroutine starts")

    supervisorScope {
        launch {
            delay(1000)
            println("Coroutine 1 executed")
        }

        launch {
            delay(1500)
            throw Exception("Coroutine 2 failed")
        }

        launch {
            delay(2000)
            println("Coroutine 3 executed")
        }
    }

    println("Main coroutine ends")
}


/***
Main coroutine starts
Coroutine 1 executed
Exception in thread "main @coroutine#3" java.lang.Exception: Coroutine 2 failed
	at FileKt$main$1$1$2.invokeSuspend(File.kt:15)
	at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
	at kotlinx.coroutines.DispatchedTaskKt.resume(DispatchedTask.kt:235)
	at kotlinx.coroutines.DispatchedTaskKt.dispatch(DispatchedTask.kt:168)
	at kotlinx.coroutines.CancellableContinuationImpl.dispatchResume(CancellableContinuationImpl.kt:474)
	at kotlinx.coroutines.CancellableContinuationImpl.resumeImpl(CancellableContinuationImpl.kt:508)
	at kotlinx.coroutines.CancellableContinuationImpl.resumeImpl$default(CancellableContinuationImpl.kt:497)
	at kotlinx.coroutines.CancellableContinuationImpl.resumeUndispatched(CancellableContinuationImpl.kt:595)
	at kotlinx.coroutines.EventLoopImplBase$DelayedResumeTask.run(EventLoop.common.kt:493)
	at kotlinx.coroutines.EventLoopImplBase.processNextEvent(EventLoop.common.kt:280)
	at kotlinx.coroutines.BlockingCoroutine.joinBlocking(Builders.kt:85)
	at kotlinx.coroutines.BuildersKt__BuildersKt.runBlocking(Builders.kt:59)
	at kotlinx.coroutines.BuildersKt.runBlocking(Unknown Source)
	at kotlinx.coroutines.BuildersKt__BuildersKt.runBlocking$default(Builders.kt:38)
	at kotlinx.coroutines.BuildersKt.runBlocking$default(Unknown Source)
	at FileKt.main(File.kt:4)
	at FileKt.main(File.kt)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(Unknown Source)
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(Unknown Source)
	at java.base/java.lang.reflect.Method.invoke(Unknown Source)
	at executors.JavaRunnerExecutor$Companion.main(JavaRunnerExecutor.kt:27)
	at executors.JavaRunnerExecutor.main(JavaRunnerExecutor.kt)
	Suppressed: kotlinx.coroutines.internal.DiagnosticCoroutineContextException: [CoroutineId(3), "coroutine#3":StandaloneCoroutine{Cancelling}@6b143ee9, BlockingEventLoop@1936f0f5]
Coroutine 3 executed
Main coroutine ends
***/
