package de.deftk.openww.android.feature.contacts

import androidx.annotation.StringRes
import de.deftk.openww.android.R
import de.deftk.openww.api.model.feature.contacts.Gender

enum class GenderTranslation(val gender: Gender, @StringRes val translation: Int) {

    MALE(Gender.MALE, R.string.gender_male),
    FEMALE(Gender.FEMALE, R.string.gender_female),
    OTHER(Gender.OTHER, R.string.gender_other);

    companion object {
        fun getByGender(gender: Gender) : GenderTranslation {
            return values().first { it.gender == gender }
        }
    }

}