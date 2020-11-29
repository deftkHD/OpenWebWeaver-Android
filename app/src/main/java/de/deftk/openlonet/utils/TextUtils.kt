package de.deftk.openlonet.utils

import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Toast
import de.deftk.lonet.api.model.Feature
import de.deftk.openlonet.R
import de.deftk.openlonet.activities.StartActivity
import de.deftk.openlonet.fragments.FileStorageGroupFragment

object TextUtils {

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
    fun parseInternalReferences(spanned: Spanned): Spanned {
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
                if (params.size == 4) {
                    builder = (builder.replace(startIndex, endIndex, params[3]))
                    val type = InternalReferenceType.getById(params[0])
                    builder.setSpan(
                        InternalReferenceSpan(type, params[1], params[2]),
                        startIndex,
                        startIndex + params[3].length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    endIndex = startIndex + params[3].length
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
                            if (previousChar == null || previousChar == ' ') {
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

    class InternalReferenceSpan(private val type: InternalReferenceType, private val group: String, private val extra: String) : ClickableSpan() {
        override fun onClick(widget: View) {
            when (type) {
                InternalReferenceType.FILE_STORAGE -> {
                    //TODO file revisions
                    val intent = Intent(widget.context, StartActivity::class.java)
                    intent.putExtra(StartActivity.EXTRA_FOCUS_FEATURE, Feature.FILES)
                    val args = Bundle()
                    args.putString(FileStorageGroupFragment.ARGUMENT_GROUP, group)
                    args.putString(FileStorageGroupFragment.ARGUMENT_FILE_ID, extra)
                    intent.putExtra(StartActivity.EXTRA_FOCUS_FEATURE_ARGUMENTS, args)
                    widget.context.startActivity(intent)
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
                Pair(2, android.R.color.holo_green_light),
                Pair(3, android.R.color.holo_red_light),
                Pair(4, android.R.color.holo_blue_dark),
                Pair(5, android.R.color.holo_orange_dark),
                Pair(6, android.R.color.holo_green_dark),
                Pair(7, android.R.color.holo_red_dark),
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
        UNKNOWN("");

        companion object {
            fun getById(id: String): InternalReferenceType {
                return values().firstOrNull { it.id == id } ?: UNKNOWN
            }
        }
    }

}