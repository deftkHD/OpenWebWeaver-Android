package de.deftk.openww.android.filter

import androidx.annotation.StringRes
import de.deftk.openww.api.model.IGroup
import de.deftk.openww.api.model.IOperatingScope
import de.deftk.openww.api.model.IScope
import de.deftk.openww.api.model.IUser
import de.deftk.openww.api.model.feature.Quota
import de.deftk.openww.api.model.feature.board.IBoardNotification
import de.deftk.openww.api.model.feature.messenger.IQuickMessage
import de.deftk.openww.api.model.feature.notes.INote
import de.deftk.openww.api.model.feature.systemnotification.ISystemNotification
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

sealed class ScopeOrder(@StringRes nameRes: Int): Order<IScope>(nameRes) {

    object ByScopeNameAsc : ScopeOrder(0) {
        override fun sort(items: List<IScope>): List<IScope> {
            return items.sortedBy { it.name }
        }
    }

    object ByScopeNameDesc : ScopeOrder(0) {
        override fun sort(items: List<IScope>): List<IScope> {
            return items.sortedByDescending { it.name }
        }
    }

    object ByScopeTypeAsc : ScopeOrder(0) {
        override fun sort(items: List<IScope>): List<IScope> {
            return items.sortedBy { it.type }
        }
    }

    object ByScopeTypeDesc : ScopeOrder(0) {
        override fun sort(items: List<IScope>): List<IScope> {
            return items.sortedByDescending { it.type }
        }
    }

    object ByOperatorDefault : ScopeOrder(0) {
        override fun sort(items: List<IScope>): List<IScope> {
            return items.sortedWith(compareBy ({ it !is IUser }, { it.name }))
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

    object ByGivenAsc : TaskOrder(0) {
        override fun sort(items: List<Pair<ITask, IOperatingScope>>): List<Pair<ITask, IOperatingScope>> {
            return items.sortedBy { it.first.startDate?.time ?: it.first.created.date.time }
        }
    }

    object ByGivenDesc : TaskOrder(0) {
        override fun sort(items: List<Pair<ITask, IOperatingScope>>): List<Pair<ITask, IOperatingScope>> {
            return items.sortedByDescending { it.first.startDate?.time ?: it.first.created.date.time }
        }
    }

}

sealed class BoardNotificationOrder(@StringRes nameRes: Int) : ScopedOrder<IBoardNotification, IGroup>(nameRes) {

    object ByTitleAsc : BoardNotificationOrder(0) {
        override fun sort(items: List<Pair<IBoardNotification, IGroup>>): List<Pair<IBoardNotification, IGroup>> {
            return items.sortedBy { it.first.title }
        }
    }

    object ByTitleDesc : BoardNotificationOrder(0) {
        override fun sort(items: List<Pair<IBoardNotification, IGroup>>): List<Pair<IBoardNotification, IGroup>> {
            return items.sortedByDescending { it.first.title }
        }
    }

    object ByCreatedAsc : BoardNotificationOrder(0) {
        override fun sort(items: List<Pair<IBoardNotification, IGroup>>): List<Pair<IBoardNotification, IGroup>> {
            return items.sortedBy { it.first.created.date.time }
        }
    }

    object ByCreatedDesc : BoardNotificationOrder(0) {
        override fun sort(items: List<Pair<IBoardNotification, IGroup>>): List<Pair<IBoardNotification, IGroup>> {
            return items.sortedByDescending { it.first.created.date.time }
        }
    }

}

sealed class SystemNotificationOrder(@StringRes nameRes: Int): Order<ISystemNotification>(nameRes) {

    object ByTypeAsc : SystemNotificationOrder(0) {
        override fun sort(items: List<ISystemNotification>): List<ISystemNotification> {
            return items.sortedBy { it.messageType }
        }
    }

    object ByTypeDesc : SystemNotificationOrder(0) {
        override fun sort(items: List<ISystemNotification>): List<ISystemNotification> {
            return items.sortedByDescending { it.messageType }
        }
    }

    object ByDateAsc : SystemNotificationOrder(0) {
        override fun sort(items: List<ISystemNotification>): List<ISystemNotification> {
            return items.sortedBy { it.date.time }
        }
    }

    object ByDateDesc : SystemNotificationOrder(0) {
        override fun sort(items: List<ISystemNotification>): List<ISystemNotification> {
            return items.sortedByDescending { it.date.time }
        }
    }

}

sealed class QuotaOrder(@StringRes nameRes: Int): Order<Pair<IOperatingScope, Quota>>(nameRes) {

    object ByOperatorDefault : QuotaOrder(0) {
        override fun sort(items: List<Pair<IOperatingScope, Quota>>): List<Pair<IOperatingScope, Quota>> {
            return items.sortedWith(compareBy ({ it.first !is IUser }, { it.first.name }))
        }
    }

}

sealed class NoteOrder(@StringRes nameRes: Int): Order<INote>(nameRes) {

    object ByDateCreatedAsc : NoteOrder(0) {
        override fun sort(items: List<INote>): List<INote> {
            return items.sortedBy { it.created.date.time }
        }
    }

    object ByDateCreatedDesc : NoteOrder(0) {
        override fun sort(items: List<INote>): List<INote> {
            return items.sortedByDescending { it.created.date.time }
        }
    }

}

sealed class MessageOrder(@StringRes nameRes: Int): Order<IQuickMessage>(nameRes) {

    object ByDateCreatedAsc : MessageOrder(0) {
        override fun sort(items: List<IQuickMessage>): List<IQuickMessage> {
            return items.sortedBy { it.date.time }
        }
    }

    object ByDateCreatedDesc : MessageOrder(0) {
        override fun sort(items: List<IQuickMessage>): List<IQuickMessage> {
            return items.sortedByDescending { it.date.time }
        }
    }

}