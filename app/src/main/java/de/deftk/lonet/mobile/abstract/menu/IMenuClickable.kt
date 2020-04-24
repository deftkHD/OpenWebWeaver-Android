package de.deftk.lonet.mobile.abstract.menu

import androidx.appcompat.app.AppCompatActivity

interface IMenuClickable : IMenuItem {

    fun onClick(activity: AppCompatActivity)

}