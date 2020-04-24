package de.deftk.lonet.mobile.abstract.menu

abstract class AbstractClickableMenuItem(private val name: Int, private val group: Int, private val icon: Int): IMenuClickable {

    override fun getName(): Int {
        return name
    }

    override fun getGroup(): Int {
        return group
    }

    override fun getIcon(): Int {
        return icon
    }

}