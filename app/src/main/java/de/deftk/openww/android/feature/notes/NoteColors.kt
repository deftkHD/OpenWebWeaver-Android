package de.deftk.openww.android.feature.notes

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import de.deftk.openww.android.R
import de.deftk.openww.api.model.feature.notes.NoteColor

enum class NoteColors(val apiColor: NoteColor, @ColorRes val androidColor: Int, @StringRes val text: Int) {

    BLUE(NoteColor.BLUE, android.R.color.holo_blue_light, R.string.blue),
    GREEN(NoteColor.GREEN, android.R.color.holo_green_light, R.string.green),
    RED(NoteColor.RED, android.R.color.holo_red_light, R.string.red),
    YELLOW(NoteColor.YELLOW, android.R.color.holo_orange_light, R.string.yellow),
    WHITE(NoteColor.WHITE, android.R.color.white, R.string.white);

    companion object {
        @JvmStatic
        fun getByApiColor(color: NoteColor?): NoteColors? {
            if (color == null) return null
            return values().firstOrNull { it.apiColor == color }
        }

        @JvmStatic
        fun getByApiColorOrDefault(color: NoteColor?): NoteColors {
            return getByApiColor(color) ?: BLUE
        }
    }
}