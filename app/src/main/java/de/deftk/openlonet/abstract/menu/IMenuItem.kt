package de.deftk.openlonet.abstract.menu

@Deprecated("remove")
interface IMenuItem {

    fun getName(): Int
    fun getGroup(): Int
    fun getIcon(): Int

}