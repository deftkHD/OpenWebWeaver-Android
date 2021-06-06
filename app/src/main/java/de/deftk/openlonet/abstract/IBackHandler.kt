package de.deftk.openlonet.abstract

@Deprecated("remove")
interface IBackHandler {

    fun onBackPressed(): Boolean // return true if back press is handled by implementing class

}