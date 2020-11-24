package de.deftk.openlonet.utils

import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.text.Spannable
import android.text.Spanned
import android.text.method.TransformationMethod
import android.text.style.URLSpan
import android.text.util.Linkify
import android.util.Patterns
import android.view.View
import android.widget.TextView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.preference.PreferenceManager


class CustomTabURLSpan(url: String): URLSpan(url) {

    override fun onClick(widget: View) {
        try {
            if (PreferenceManager.getDefaultSharedPreferences(widget.context).getBoolean("open_link_external", false)) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                widget.context.startActivity(intent)
            } else {
                CustomTabsIntent.Builder().build().launchUrl(widget.context, Uri.parse(url))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            super.onClick(widget)
        }
    }

}

class CustomTabTransformationMethod(private val linkifyOptions: Int): TransformationMethod {

    override fun getTransformation(source: CharSequence?, view: View?): CharSequence? {
        if (view is TextView) {
            Linkify.addLinks(view, linkifyOptions)
            if (view.text == null || view.text !is Spannable)
                return source
            val span = view.text as Spannable

            span.getSpans(0, view.length(), URLSpan::class.java).forEach { oldSpan ->
                val startIndex = span.getSpanStart(oldSpan)
                val lastIndex = span.getSpanEnd(oldSpan)
                val url = oldSpan.url
                if (Patterns.WEB_URL.matcher(url).matches()) {
                    span.setSpan(
                        CustomTabURLSpan(url),
                        startIndex,
                        lastIndex,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    span.removeSpan(oldSpan)
                }
            }
        }
        return source
    }

    override fun onFocusChanged(
        view: View?,
        sourceText: CharSequence?,
        focused: Boolean,
        direction: Int,
        previouslyFocusedRect: Rect?
    ) {
    }
}