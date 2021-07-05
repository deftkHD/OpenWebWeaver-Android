package de.deftk.openww.android.filter

import androidx.annotation.StringRes
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.IScope
import de.deftk.openww.api.model.feature.tasks.ITask

sealed class Order<T>(@StringRes val nameRes: Int) {

    abstract fun sort(items: List<T>): List<T>

    class None<T> : Order<T>(0) {
        override fun sort(items: List<T>): List<T> {
            return items
        }
    }

}

sealed class ScopedOrder<T, K : IScope>(@StringRes nameRes: Int): Order<Pair<T, K>>(nameRes) {

    class ByScopeNameAsc<T, K : IScope> : ScopedOrder<T, K>(0) {
        override fun sort(items: List<Pair<T, K>>): List<Pair<T, K>> {
            return items.sortedBy { it.second.name }
        }
    }

    class ByScopeNameDesc<T, K : IScope> : ScopedOrder<T, K>(0) {
        override fun sort(items: List<Pair<T, K>>): List<Pair<T, K>> {
            return items.sortedByDescending { it.second.name }
        }
    }

}

sealed class TaskOrder(@StringRes nameRes: Int) : ScopedOrder<ITask, IOperatingScope>(nameRes) {

    object ByTitleAsc : TaskOrder(0) {
        override fun sort(items: List<Pair<ITask, IOperatingScope>>): List<Pair<ITask, IOperatingScope>> {
            return items.sortedBy { it.first.title }
        }
    }

    object ByTitleDesc : TaskOrder(0) {
        override fun sort(items: List<Pair<ITask, IOperatingScope>>): List<Pair<ITask, IOperatingScope>> {
            return items.sortedByDescending { it.first.title }
        }
    }

    object ByStartDateAsc : TaskOrder(0) {
        override fun sort(items: List<Pair<ITask, IOperatingScope>>): List<Pair<ITask, IOperatingScope>> {
            return items.sortedBy { it.first.startDate?.time ?: -1 }
        }
    }

    object ByStartDateDesc : TaskOrder(0) {
        override fun sort(items: List<Pair<ITask, IOperatingScope>>): List<Pair<ITask, IOperatingScope>> {
            return items.sortedByDescending { it.first.startDate?.time ?: -1 }
        }
    }

    object ByDueDateAsc : TaskOrder(0) {
        override fun sort(items: List<Pair<ITask, IOperatingScope>>): List<Pair<ITask, IOperatingScope>> {
            return items.sortedBy { it.first.dueDate?.time ?: -1 }
        }
    }

    object ByDueDateDesc : TaskOrder(0) {
        override fun sort(items: List<Pair<ITask, IOperatingScope>>): List<Pair<ITask, IOperatingScope>> {
            return items.sortedByDescending { it.first.dueDate?.time ?: -1 }
        }
    }

    object ByCompletedAsc : TaskOrder(0) {
        override fun sort(items: List<Pair<ITask, IOperatingScope>>): List<Pair<ITask, IOperatingScope>> {
            return items.sortedBy { it.first.completed }
        }
    }

    object ByCompletedDesc : TaskOrder(0) {
        override fun sort(items: List<Pair<ITask, IOperatingScope>>): List<Pair<ITask, IOperatingScope>> {
            return items.sortedByDescending { it.first.completed }
        }
    }

}