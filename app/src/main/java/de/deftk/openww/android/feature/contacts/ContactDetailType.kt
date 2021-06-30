package de.deftk.openww.android.feature.contacts

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import de.deftk.openww.android.R

enum class ContactDetailType(@DrawableRes val drawable: Int, @StringRes val description: Int) {

    NICK_NAME(R.drawable.ic_person_24, R.string.nick_name),
    FULL_NAME(R.drawable.ic_person_24, R.string.full_name),
    FIRST_NAME(R.drawable.ic_person_24, R.string.first_name),
    MIDDLE_NAME(R.drawable.ic_person_24, R.string.middle_name),
    LAST_NAME(R.drawable.ic_person_24, R.string.last_name),
    BIRTHDAY(R.drawable.ic_cake_24, R.string.birthday),

    EMAIL_ADDRESS(R.drawable.ic_email_24, R.string.email_address),
    EMAIL_ADDRESS_2(R.drawable.ic_email_24, R.string.email_address_2),
    EMAIL_ADDRESS_3(R.drawable.ic_email_24, R.string.email_address_3),

    MOBILE_PHONE(R.drawable.ic_smartphone_24, R.string.mobile_phone),
    GENDER(0, R.string.gender),
    HOBBIES(R.drawable.ic_local_activity_24, R.string.hobbys),

    HOME_CITY(0, R.string.home_city),
    HOME_COORDS(0, R.string.home_coordinates),
    HOME_COUNTRY(R.drawable.ic_language_24, R.string.home_country),
    HOME_PHONE(R.drawable.ic_local_phone_24, R.string.home_phone),
    HOME_FAX(0, R.string.home_fax),
    HOME_POSTAL_CODE(0, R.string.postal_code),
    HOME_STATE(0, R.string.home_state),
    HOME_STREET(0, R.string.home_street),
    HOME_STREET_2(0, R.string.home_street_2),
    HOME_STREET_3(0, R.string.home_street_3),

    TITLE(0, R.string.title),
    JOB_TITLE(0, R.string.job_title),
    JOB_TITLE_2(0, R.string.job_title_2),
    SUFFIX(0, R.string.suffix),

    COMPANY(0, R.string.company),
    COMPANY_TYPE(0, R.string.company_type),

    BUSINESS_CITY(0, R.string.business_city),
    BUSINESS_COORDS(0, R.string.business_coordinates),
    BUSINESS_COUNTRY(R.drawable.ic_language_24, R.string.business_country),
    BUSINESS_FAX(0, R.string.business_fax),
    BUSINESS_PHONE(R.drawable.ic_local_phone_24, R.string.business_phone),
    BUSINESS_POSTAL_CODE(0, R.string.business_postal_code),
    BUSINESS_STATE(0, R.string.business_state),
    BUSINESS_STREET(0, R.string.business_street),
    BUSINESS_STREET_2(0, R.string.business_street_2),
    BUSINESS_STREET_3(0, R.string.business_street_3),

    SUBJECTS(0, R.string.subjects),
    UID(0, R.string.uid),
    WEBSITE(R.drawable.ic_language_24, R.string.website),
    NOTES(R.drawable.ic_notes_24, R.string.notes),

    CATEGORIES(0, R.string.categories),


}