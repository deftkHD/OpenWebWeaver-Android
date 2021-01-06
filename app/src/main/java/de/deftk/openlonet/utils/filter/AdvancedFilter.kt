package de.deftk.openlonet.utils.filter

import de.deftk.lonet.api.implementation.OperatingScope
import de.deftk.lonet.api.model.IScope
import de.deftk.lonet.api.model.feature.mailbox.EmailAddress

fun EmailAddress.filterApplies(constraint: String): Boolean {
    return name.contains(constraint, true) || (constraint.contains("@") && address.contains(constraint, true))
}

fun IScope.filterApplies(constraint: String): Boolean {
    return name.contains(constraint, true) || (constraint.contains("@") && login.contains(constraint, true))
}

fun OperatingScope.filterApplies(constraint: String): Boolean {
    return name.contains(constraint, true)
}

fun String?.filterApplies(constraint: String): Boolean {
    return this != null && this.contains(constraint, true)
}