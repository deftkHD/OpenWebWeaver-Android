package de.deftk.openww.android.room

import androidx.room.Entity
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.feature.tasks.ITask

@Entity(primaryKeys = ["id", "scope"])
data class IgnoredTask(
    val id: String,
    val scope: String
) {

    companion object {
        fun from(task: ITask, scope: IOperatingScope): IgnoredTask {
            return IgnoredTask(task.id, scope.login)
        }
    }

}