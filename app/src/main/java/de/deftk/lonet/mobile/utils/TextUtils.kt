package de.deftk.lonet.mobile.utils

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Toast
import de.deftk.lonet.api.model.Feature
import de.deftk.lonet.mobile.activities.StartActivity
import de.deftk.lonet.mobile.fragments.FileStorageFragment

object TextUtils {

    fun parseHtml(text: String?): Spanned {
        val html = text
            ?.replace("\n", "<br>")
            ?.replace("\t", "&nbsp; &nbsp; &nbsp;")
            ?: ""

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
        else Html.fromHtml(html)
    }

    // structure {{<function>|<group>|<file>|<display text>}}
    fun parseInternalReferences(spanned: Spanned): Spanned {
        var builder = SpannableStringBuilder(spanned)
        var startIndex: Int
        var endIndex = 0
        while (true) {
            startIndex = builder.indexOf("{{", endIndex)
            endIndex = builder.indexOf("}}", startIndex + 2)
            if (startIndex == -1 || endIndex == -1) break
            val params = mutableListOf<String>()

            var paramStartIndex = startIndex + 2
            var paramEndIndex: Int
            for (i in 0..3) {
                paramEndIndex = builder.indexOf("|", paramStartIndex)
                if (paramEndIndex == -1 || paramEndIndex > endIndex) paramEndIndex = endIndex
                params.add(builder.substring(paramStartIndex, paramEndIndex).trim())
                paramStartIndex = paramEndIndex + 1
            }
            endIndex += 2
            if (params.size == 4) {
                builder = (builder.replace(startIndex, endIndex, params[3]))
                val type = InternalReferenceType.getById(params[0])
                builder.setSpan(InternalReferenceSpan(type, params[1], params[2], params[3]), startIndex, startIndex + params[3].length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                endIndex = startIndex + params[3].length
            }

        }
        return builder
    }


    class InternalReferenceSpan(private val type: InternalReferenceType, private val group: String, private val extra: String, private val displayText: String): ClickableSpan() {

        override fun onClick(widget: View) {
            when (type) {
                InternalReferenceType.FILE_STORAGE -> {
                    //TODO file revisions
                    val intent = Intent(widget.context, StartActivity::class.java)
                    intent.putExtra(StartActivity.EXTRA_FOCUS_FEATURE, Feature.FILES)
                    val args = Bundle()
                    args.putString(FileStorageFragment.ARGUMENT_GROUP, group)
                    args.putString(FileStorageFragment.ARGUMENT_FILE_ID, extra)
                    intent.putExtra(StartActivity.EXTRA_FOCUS_FEATURE_ARGUMENTS, args)
                    widget.context.startActivity(intent)
                }
                else -> {
                    Toast.makeText(widget.context, "Don't know how to handle reference type \"$type\"", Toast.LENGTH_LONG).show()
                }
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