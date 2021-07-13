package de.deftk.openww.android.filter

import de.deftk.openww.android.R
import de.deftk.openww.api.model.feature.notes.INote

class NoteFilter : Filter<INote>(NoteOrder.ByDateCreatedDesc) {

    val titleCriteria = addCriteria<String>(R.string.title, null) { element, value ->
        value ?: return@addCriteria true
        element.title.contains(value, true)
    }

    val descriptionCriteria = addCriteria<String>(R.string.description, null) { element, value ->
        value ?: return@addCriteria true
        element.text.contains(value, true)
    }

    val smartSearchCriteria = addCriteria<String>(R.string.smart_search, null) { element, value ->
        value ?: return@addCriteria true
        titleCriteria.matches(element, value) || descriptionCriteria.matches(element, value)
    }

}