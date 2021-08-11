package de.deftk.openww.android.utils

import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.format.DateUtils
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Toast
import androidx.navigation.NavController
import de.deftk.openww.android.R
import java.text.DateFormat
import java.util.*

object TextUtils {

    fun parseShortDate(date: Date): String {
        return if (DateUtils.isToday(date.time)) {
            DateFormat.getTimeInstance(DateFormat.SHORT).format(date)
        } else {
            DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT).format(date)
        }
    }

    fun parseHtml(text: String?): Spanned {
        val html = text
            ?.replace("\n", "<br>")
            ?.replace("\t", "&nbsp; &nbsp; &nbsp;")
            ?: ""

        @Suppress("DEPRECATION")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
        else Html.fromHtml(html)
    }

    // structure {{<function>|<group>|<detail>|<display text>}}
    // alternative structure {{<email>}}
    fun parseInternalReferences(spanned: Spanned, currentScope: String?, navController: NavController?): Spanned {
        var builder = SpannableStringBuilder(spanned)
        var startIndex: Int
        var endIndex = 0
        while (true) {
            try {
                startIndex = builder.indexOf("{{", endIndex)
                endIndex = builder.indexOf("}}", startIndex + 2)
                if (startIndex == -1 || endIndex == -1)
                    break
                val params = mutableListOf<String>()

                var paramStartIndex = startIndex + 2
                var paramEndIndex: Int
                for (i in 0..3) {
                    paramEndIndex = builder.indexOf("|", paramStartIndex)
                    if (paramEndIndex == -1 || paramEndIndex > endIndex)
                        paramEndIndex = endIndex
                    params.add(builder.substring(paramStartIndex, paramEndIndex).trim())
                    paramStartIndex = paramEndIndex + 1
                    if (paramStartIndex >= endIndex)
                        break
                }
                endIndex += 2
                if (params.size == 4 || params.size == 3) {
                    builder = (builder.replace(startIndex, endIndex, params[params.size - 1]))
                    val type = InternalReferenceType.getById(params[0])
                    val scope = if (params.size == 3) currentScope else params[1]
                    builder.setSpan(
                        InternalReferenceSpan(type, scope, params[params.size - 2], params[params.size - 1], navController),
                        startIndex,
                        startIndex + params[params.size - 1].length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    endIndex = startIndex + params[params.size - 1].length
                } else if (params.size == 1) {
                    val reference = params[0]
                    val name = if (reference.contains("@")) {
                        reference.substring(0, reference.indexOf("@"))
                    } else null
                    val display = name ?: reference
                    builder = builder.replace(startIndex, endIndex, display)
                    builder.setSpan(
                        MailAddressSpan(reference),
                        startIndex,
                        startIndex + display.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    endIndex = startIndex + display.length
                }
            } catch (ignored: Exception) {
            } // don't do anything if failed to parse reference; if possible try to continue read the other references
        }
        return builder
    }

    /**
     * Parses multiple quotes per line
     */
    fun parseMultipleQuotes(spanned: Spanned): Spanned {
        return if (spanned.contains("\n>")) {
            var builder = SpannableStringBuilder(spanned)
            try {
                var globalIndex = 0
                builder.split("\n").forEach { line ->
                    var level = 0
                    val startIndex = globalIndex
                    var charIndex = 0
                    // walk through line
                    while (globalIndex - startIndex < line.length) {
                        if (builder[globalIndex] == '>') {
                            val previousChar = line.getOrNull(charIndex - 1)
                            if (previousChar == null || previousChar == ' ' || previousChar == '>') {
                                builder.setSpan(MultiQuoteSpan(level), globalIndex, globalIndex + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                                builder = builder.replace(globalIndex, globalIndex + 1, " ")
                                level++
                            }
                        }
                        globalIndex++
                        charIndex++
                    }
                    globalIndex++
                }
            } catch (e: Exception) {
            } // don't do anything if failed to parse reference

            builder
        } else {
            spanned
        }
    }


    class InternalReferenceSpan(private val type: InternalReferenceType, private val scope: String?, private val extra: String, private val name: String, private val navController: NavController?) : ClickableSpan() {
        override fun onClick(widget: View) {
            when (type) {
                InternalReferenceType.FILE_STORAGE -> {
                    if (scope == null) {
                        Toast.makeText(widget.context, R.string.error_invalid_internal_reference, Toast.LENGTH_LONG).show()
                        return
                    }

                    val lastSlashIndex = extra.lastIndexOf('/')
                    val fileId = extra.substring(lastSlashIndex, extra.length) //TODO file revisions
                    val folderId = extra.substring(0, lastSlashIndex)
                    val path = mutableListOf<String>()
                    val pathBuilder = StringBuilder("/")
                    val splitted = extra.split('/')
                    splitted.subList(1, splitted.size - 1).forEach { pathSegment ->
                        pathBuilder.append(pathSegment)
                        path.add(pathBuilder.toString())
                        pathBuilder.append("/")
                    }
                    if (path.isNotEmpty())
                        path.removeLast()

                    if (path.isNotEmpty() || folderId.isNotBlank()) {
                        Toast.makeText(widget.context, R.string.not_implemented, Toast.LENGTH_LONG).show()
                        return
                    }

                    val args = Bundle()
                    args.putString("operatorId", scope)
                    args.putString("title", name) //TODO better title (use parent contained inside extra or scope)
                    args.putString("highlightFileId", fileId)
                    args.putString("folderId", folderId.ifBlank { "/" }) //TODO the folder id which is passed uses parentNames, not parentIds
                    args.putBoolean("pasteMode", false)
                    navController?.navigate(R.id.filesFragment, args)
                }
                InternalReferenceType.LEANING_PLAN -> {
                    Toast.makeText(
                        widget.context,
                        R.string.learning_plan_not_support,
                        Toast.LENGTH_LONG
                    ).show()
                }
                else -> {
                    Toast.makeText(
                        widget.context,
                        widget.context.getString(R.string.unknown_reference_type).format(type),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    class MailAddressSpan(private val mail: String) : ClickableSpan() {
        override fun onClick(widget: View) {
            Toast.makeText(widget.context, mail, Toast.LENGTH_LONG).show()
        }
    }

    class MultiQuoteSpan(private val level: Int): ClickableSpan() {

        companion object {
            private val levelColorMap = mapOf(
                Pair(0, android.R.color.holo_blue_light),
                Pair(1, android.R.color.holo_orange_light),
                Pair(2, android.R.color.holo_red_light),
                Pair(3, android.R.color.holo_green_light),
                Pair(4, android.R.color.holo_blue_dark),
                Pair(5, android.R.color.holo_orange_dark),
                Pair(6, android.R.color.holo_red_dark),
                Pair(7, android.R.color.holo_green_dark)
            )
        }

        override fun onClick(widget: View) {}

        override fun updateDrawState(ds: TextPaint) {
            val colorCode = levelColorMap.getOrElse(level) { android.R.color.holo_blue_light }
            ds.bgColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Resources.getSystem().getColor(colorCode, null)
            } else {
                @Suppress("DEPRECATION")
                Resources.getSystem().getColor(colorCode)
            }
        }
    }

    enum class InternalReferenceType(val id: String) {
        FILE_STORAGE("files"),
        LEANING_PLAN("learning_plan"),
        UNKNOWN("");

        companion object {
            fun getById(id: String): InternalReferenceType {
                return values().firstOrNull { it.id == id } ?: UNKNOWN
            }
        }
    }

}