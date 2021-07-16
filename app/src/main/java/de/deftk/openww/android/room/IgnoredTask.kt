package de.deftk.openww.android.room

import androidx.room.Entity
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.feature.tasks.ITask

@Entity(primaryKeys = ["account", "id", "scope"])
data class IgnoredTask(
    val account: String,
    val id: String,
    val scope: String
) {

    companion object {
        fun from(account: String, task: ITask, scope: IOperatingScope): IgnoredTask {
            return IgnoredTask(account, task.id, scope.login)
        }
    }

}