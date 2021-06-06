package de.deftk.openlonet.abstract.menu

import androidx.appcompat.app.AppCompatActivity

@Deprecated("remove")
interface IMenuClickable : IMenuItem {

    fun onClick(activity: AppCompatActivity)

}