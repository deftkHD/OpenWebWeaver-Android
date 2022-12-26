package de.deftk.openww.android.feature.devtools

import java.util.Date

data class ExceptionReport(val id: Int, val source: ExceptionSource, val date: Date, val stackTrace: List<StackTraceElement>, val exception: Throwable?, val message: String?) {

    fun getTitle(): String {
        if (exception != null) {
            return exception::class.java.name
        }
        if (message != null) {
            return message
        }
        return "<unknown exception>"
    }

    fun getDetail(): String {
        return message ?: exception?.localizedMessage ?: exception?.message ?: exception?.stackTraceToString() ?: exception.toString()
    }

}