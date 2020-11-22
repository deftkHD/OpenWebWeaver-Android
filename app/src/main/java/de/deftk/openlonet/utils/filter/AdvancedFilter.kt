package de.deftk.openlonet.utils.filter

import de.deftk.lonet.api.model.abstract.AbstractOperator
import de.deftk.lonet.api.model.abstract.IManageable
import de.deftk.lonet.api.model.feature.mailbox.EmailAddress

fun EmailAddress.filterApplies(constraint: String): Boolean {
    return name.contains(constraint, true) || (constraint.contains("@") && address.contains(constraint, true))
}

fun IManageable.filterApplies(constraint: String): Boolean {
    return getName().contains(constraint, true) || (constraint.contains("@") && getLogin().contains(constraint, true))
}

fun AbstractOperator.filterApplies(constraint: String): Boolean {
    return getName().contains(constraint, true)
}

fun String?.filterApplies(constraint: String): Boolean {
    return this != null && this.contains(constraint, true)
}