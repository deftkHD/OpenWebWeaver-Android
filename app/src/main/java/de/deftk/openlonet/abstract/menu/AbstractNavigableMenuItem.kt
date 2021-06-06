package de.deftk.openlonet.abstract.menu

@Deprecated("remove")
abstract class AbstractNavigableMenuItem(name: Int, group: Int, icon: Int): AbstractClickableMenuItem(name, group, icon), IMenuNavigable