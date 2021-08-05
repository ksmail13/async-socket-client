package io.github.ksmail13.exception

sealed class TimeoutException(msg: String, cause: Throwable? = null): RuntimeException(msg, cause) {
    override fun getStackTrace(): Array<StackTraceElement> {
        return if (cause == null) {
            super.getStackTrace()
        } else {
            arrayOf()
        }
    }
}

class ConnectionTimeoutException(msg: String = "Connection timeout", cause: Throwable? = null): TimeoutException(msg, cause)
class ReadTimeoutException(msg: String = "Read timeout", cause: Throwable? = null): TimeoutException(msg, cause)
class WriteTimeoutException(msg: String = "Write timeout", cause: Throwable? = null): TimeoutException(msg, cause)
