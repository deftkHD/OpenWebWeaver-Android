package de.deftk.lonet.mobile.utils

import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

object UnitUtil {

    fun getFormattedSize(size: Long): String {
        if (size == 0L) return "0B"
        val units = arrayOf("B", "kB", "MB", "GB", "T")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(size / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
    }
}