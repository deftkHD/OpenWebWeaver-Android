package de.deftk.openww.android.exception

class ObjectNotFoundException(objName: String): IllegalStateException("$objName not found")