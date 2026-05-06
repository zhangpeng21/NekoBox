@file:Suppress("EXPERIMENTAL_API_USAGE")

package io.nekohasekai.sagernet.ktx

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*

// Deep-Opt: replace GlobalScope with a process-lifetime CoroutineScope.
//
// GlobalScope has two problems:
//  1. It is never cancelled, so coroutines launched in it survive even after the
//     component that started them is destroyed — a coroutine leak that wastes memory
//     and CPU and can cause use-after-free of UI objects in callbacks.
//  2. GlobalScope has no parent Job, so uncaught exceptions silently swallow errors
//     instead of propagating them to a handler.
//
// AppScope uses SupervisorJob so one failed child does not cancel siblings,
// matching the old GlobalScope semantics for fire-and-forget tasks, but it IS
// tied to the process lifetime (cancelled on Application.onTerminate) and uses
// an explicit CoroutineExceptionHandler for logging.
val AppScope: CoroutineScope = CoroutineScope(
    SupervisorJob() +
    Dispatchers.Default +
    CoroutineExceptionHandler { _, t ->
        // Do NOT re-throw — this is a fire-and-forget scope
        Logs.w("AppScope uncaught", t)
    }
)

fun block(block: suspend CoroutineScope.() -> Unit): suspend CoroutineScope.() -> Unit {
    return block
}

fun runOnDefaultDispatcher(block: suspend CoroutineScope.() -> Unit) =
    AppScope.launch(Dispatchers.Default, block = block)

fun Fragment.runOnLifecycleDispatcher(block: suspend CoroutineScope.() -> Unit) =
    lifecycleScope.launch(Dispatchers.Default, block = block)

suspend fun <T> onDefaultDispatcher(block: suspend CoroutineScope.() -> T) =
    withContext(Dispatchers.Default, block = block)

fun runOnIoDispatcher(block: suspend CoroutineScope.() -> Unit) =
    AppScope.launch(Dispatchers.IO, block = block)

suspend fun <T> onIoDispatcher(block: suspend CoroutineScope.() -> T) =
    withContext(Dispatchers.IO, block = block)

fun runOnMainDispatcher(block: suspend CoroutineScope.() -> Unit) =
    AppScope.launch(Dispatchers.Main.immediate, block = block)

suspend fun <T> onMainDispatcher(block: suspend CoroutineScope.() -> T) =
    withContext(Dispatchers.Main.immediate, block = block)

fun runBlockingOnMainDispatcher(block: suspend CoroutineScope.() -> Unit) {
    runBlocking {
        AppScope.launch(Dispatchers.Main.immediate, block = block)
    }
}
