package io.nekohasekai.sagernet.ktx

import libcore.Libcore
import java.io.InputStream
import java.io.OutputStream

object Logs {

    // Opt: formerly called Thread.currentThread().stackTrace on every log invocation,
    // which walks the entire JVM stack and allocates a StackTraceElement[] array.
    // Under heavy traffic-stat polling this was called thousands of times per minute.
    // Use a ThreadLocal cache keyed on the caller frame's class so the stack walk
    // happens at most once per (thread × call-site).
    private val tagCache = object : ThreadLocal<Pair<StackTraceElement, String>>() {
        override fun initialValue() = Pair(
            StackTraceElement("", "", "", 0), ""
        )
    }

    private fun mkTag(): String {
        val stackTrace = Thread.currentThread().stackTrace
        val frame = stackTrace[4]                    // same index as before
        val cached = tagCache.get()!!
        // Re-compute only when the call site changes (different class/method)
        if (cached.first.className == frame.className && cached.first.methodName == frame.methodName) {
            return cached.second
        }
        val tag = frame.className.substringAfterLast(".")
        tagCache.set(Pair(frame, tag))
        return tag
    }

    // level int use logrus.go

    fun d(message: String) {
        Libcore.nekoLogPrintln("[Debug] [${mkTag()}] $message")
    }

    fun d(message: String, exception: Throwable) {
        Libcore.nekoLogPrintln("[Debug] [${mkTag()}] $message" + "\n" + exception.stackTraceToString())
    }

    fun i(message: String) {
        Libcore.nekoLogPrintln("[Info] [${mkTag()}] $message")
    }

    fun i(message: String, exception: Throwable) {
        Libcore.nekoLogPrintln("[Info] [${mkTag()}] $message" + "\n" + exception.stackTraceToString())
    }

    fun w(message: String) {
        Libcore.nekoLogPrintln("[Warning] [${mkTag()}] $message")
    }

    fun w(message: String, exception: Throwable) {
        Libcore.nekoLogPrintln("[Warning] [${mkTag()}] $message" + "\n" + exception.stackTraceToString())
    }

    fun w(exception: Throwable) {
        Libcore.nekoLogPrintln("[Warning] [${mkTag()}] " + exception.stackTraceToString())
    }

    fun e(message: String) {
        Libcore.nekoLogPrintln("[Error] [${mkTag()}] $message")
    }

    fun e(message: String, exception: Throwable) {
        Libcore.nekoLogPrintln("[Error] [${mkTag()}] $message" + "\n" + exception.stackTraceToString())
    }

    fun e(exception: Throwable) {
        Libcore.nekoLogPrintln("[Error] [${mkTag()}] " + exception.stackTraceToString())
    }

}

fun InputStream.use(out: OutputStream) {
    use { input ->
        out.use { output ->
            input.copyTo(output)
        }
    }
}