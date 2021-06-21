package de.deftk.openww.android.feature.board

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import de.deftk.openww.api.model.feature.board.BoardNotificationColor
import de.deftk.openww.android.R

enum class BoardNotificationColors(val apiColor: BoardNotificationColor, @ColorRes val androidColor: Int, @StringRes val text: Int) {
    BLUE(BoardNotificationColor.BLUE, android.R.color.holo_blue_light, R.string.blue),
    GREEN(BoardNotificationColor.GREEN, android.R.color.holo_green_light, R.string.green),
    RED(BoardNotificationColor.RED, android.R.color.holo_red_light, R.string.red),
    YELLOW(BoardNotificationColor.YELLOW, android.R.color.holo_orange_light, R.string.yellow),
    WHITE(BoardNotificationColor.WHITE, android.R.color.white, R.string.white);

    companion object {
        @JvmStatic
        fun getByApiColor(color: BoardNotificationColor?): BoardNotificationColors? {
            if (color == null) return null
            return values().firstOrNull { it.apiColor == color }
        }

        @JvmStatic
        fun getByApiColorOrDefault(color: BoardNotificationColor?): BoardNotificationColors {
            return getByApiColor(color) ?: BLUE
        }
    }
}