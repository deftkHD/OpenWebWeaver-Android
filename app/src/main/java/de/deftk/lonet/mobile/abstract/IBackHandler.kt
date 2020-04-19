package de.deftk.lonet.mobile.abstract

interface IBackHandler {

    fun onBackPressed(): Boolean // return true if back press is handled by implementing class

}