package de.deftk.openww.android

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.text.format.Formatter
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import androidx.navigation.NavController
import de.deftk.openww.android.activities.MainActivity
import de.deftk.openww.android.feature.forum.ForumPostIcons
import de.deftk.openww.android.utils.ContactUtil
import de.deftk.openww.android.utils.CustomTabTransformationMethod
import de.deftk.openww.android.utils.TextUtils
import de.deftk.openww.android.utils.UIUtil
import de.deftk.openww.android.viewmodel.UserViewModel
import de.deftk.openww.api.model.IScope
import de.deftk.openww.api.model.IUser
import de.deftk.openww.api.model.RemoteScope
import de.deftk.openww.api.model.feature.Quota
import de.deftk.openww.api.model.feature.contacts.IContact
import de.deftk.openww.api.model.feature.filestorage.FileType
import de.deftk.openww.api.model.feature.filestorage.IRemoteFile
import de.deftk.openww.api.model.feature.forum.IForumPost
import de.deftk.openww.api.model.feature.mailbox.IEmail
import de.deftk.openww.api.model.feature.tasks.ITask
import java.util.*

object BindingAdapters {

    @JvmStatic
    @BindingAdapter("backgroundResource")
    fun backgroundResource(view: View, @ColorRes color: Int) {
        view.setBackgroundResource(color)
    }

    @JvmStatic
    @BindingAdapter("fileSize")
    fun fileSize(view: TextView, file: IRemoteFile) {
        if (file.type == FileType.FILE) {
            view.text = Formatter.formatFileSize(view.context, file.size)
        } else if (file.type == FileType.FOLDER) {
            view.text = view.context.getString(R.string.directory)
        }
    }

    @JvmStatic
    @BindingAdapter("byteSize")
    fun byteSize(view: TextView, size: Long) {
        view.text = Formatter.formatFileSize(view.context, size)
    }

    @JvmStatic
    @BindingAdapter("filePreview")
    fun filePreview(view: ImageView, file: IRemoteFile) {
        if (file.type == FileType.FILE) {
            view.setImageResource(R.drawable.ic_file_32)
        } else if (file.type == FileType.FOLDER) {
            view.setImageResource(R.drawable.ic_folder_32)
        }
    }

    @JvmStatic
    @BindingAdapter("quotaProgress")
    fun quotaProgress(view: ProgressBar, quota: Quota) {
        view.progress = ((quota.usage.toFloat() / quota.limit) * 100).toInt()
    }

    @JvmStatic
    @BindingAdapter("quotaText")
    fun quotaText(view: TextView, quota: Quota) {
        view.text = String.format(view.context.getString(R.string.quota), Formatter.formatFileSize(view.context, quota.free), Formatter.formatFileSize(view.context, quota.limit))
    }

    @JvmStatic
    @BindingAdapter("forumPostIcon")
    fun forumPostIcon(view: ImageView, post: IForumPost) {
        view.setImageResource(ForumPostIcons.getByTypeOrDefault(post.icon).resource)
    }

    @JvmStatic
    @BindingAdapter("mailAuthor")
    fun mailAuthor(view: TextView, email: IEmail) {
        var author = email.from?.joinToString(", ") { it.name }
        if (author == null) {
            val context = view.context
            author = if (context is MainActivity) {
                val userViewModel by context.viewModels<UserViewModel>()
                userViewModel.apiContext.value?.user?.name ?: "UNKNOWN"
            } else {
                "UNKNOWN"
            }
        }
        view.text = author
    }

    @JvmStatic
    @BindingAdapter("dateText")
    fun dateText(view: TextView, date: Date) {
        view.text = TextUtils.parseShortDate(date)
    }

    @JvmStatic
    @BindingAdapter("bold")
    fun bold(view: TextView, bold: Boolean) {
        if (bold) {
            view.setTypeface(null, Typeface.BOLD)
        } else {
            view.setTypeface(null, Typeface.NORMAL)
        }
    }

    @JvmStatic
    @BindingAdapter("italic")
    fun italic(view: TextView, bold: Boolean) {
        if (bold) {
            view.setTypeface(null, Typeface.ITALIC)
        } else {
            view.setTypeface(null, Typeface.NORMAL)
        }
    }

    @JvmStatic
    @BindingAdapter("memberOnlineImage")
    fun memberOnlineImage(view: ImageView, scope: IScope) {
        if (scope is RemoteScope) {
            if (scope.online == true) {
                view.setImageResource(R.drawable.ic_person_accent_24)
            } else {
                view.setImageResource(R.drawable.ic_person_24)
            }
        } else if (scope is IUser) {
            view.setImageResource(R.drawable.ic_person_accent_24)
        } else {
            view.setImageResource(R.drawable.ic_person_24)
        }
    }

    @JvmStatic
    @BindingAdapter("memberOnlineText")
    fun memberOnlineText(view: TextView, scope: IScope) {
        if (scope is RemoteScope) {
            if (scope.online == true) {
                view.setText(R.string.online)
            } else {
                view.setText(R.string.offline)
            }
        } else if (scope is IUser) {
            view.setText(R.string.online)
        } else {
            view.isVisible = false
        }
    }

    @JvmStatic
    @BindingAdapter("strikeThroughTask")
    fun strikeThrough(view: TextView, task: ITask) {
        if (task.dueDate != null && Date().compareTo(task.dueDate) > -1) {
            view.paintFlags = view.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            view.paintFlags = view.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
    }

    @JvmStatic
    @BindingAdapter("taskDueDate")
    fun taskDueDate(view: TextView, task: ITask) {
        view.text = if (task.dueDate != null) UIUtil.getTaskDue(task) else view.context.getString(R.string.not_set)
    }

    @JvmStatic
    @BindingAdapter("taskCompleted")
    fun taskCompleted(view: ImageView, task: ITask) {
        view.setBackgroundResource(if (task.completed) R.drawable.ic_check_green_32 else 0)
    }

    @JvmStatic
    @BindingAdapter("android:layout_marginStart")
    fun layoutMarginStart(view: View, margin: Int) {
        val params = view.layoutParams as ViewGroup.MarginLayoutParams
        params.setMargins(margin, params.topMargin, params.rightMargin, params.bottomMargin)
        view.layoutParams = params
    }

    @JvmStatic
    @BindingAdapter("android:layout_marginEnd")
    fun layoutMarginEnd(view: View, margin: Int) {
        val params = view.layoutParams as ViewGroup.MarginLayoutParams
        params.setMargins(params.leftMargin, params.topMargin, margin, params.bottomMargin)
        view.layoutParams = params
    }

    @JvmStatic
    @BindingAdapter("formattedText", "currentScope", "navController", requireAll = true)
    fun setFormattedText(view: TextView, text: String?, currentScope: String, navController: NavController?) {
        if (text != null) {
            view.text = TextUtils.parseMultipleQuotes(TextUtils.parseInternalReferences(TextUtils.parseHtml(text), currentScope, navController))
            view.movementMethod = LinkMovementMethod.getInstance()
            view.transformationMethod = CustomTabTransformationMethod(view.autoLinkMask)
        } else {
            view.text = ""
        }
    }

    @JvmStatic
    @BindingAdapter("android:isVisible")
    fun setIsVisible(view: View, visible: Boolean) {
        view.isVisible = visible
    }

    @JvmStatic
    @BindingAdapter("contactName")
    fun setContactName(view: TextView, contact: IContact) {
        view.text = ContactUtil.getContactName(contact)
    }

    @JvmStatic
    @BindingAdapter("contactDescription")
    fun setContactDescription(view: TextView, contact: IContact) {
        val text = contact.emailAddress
            ?: contact.email2Address
            ?: contact.email3Address
            ?: contact.homePhone
            ?: contact.mobilePhone
            ?: contact.businessPhone
            ?: contact.homeFax
            ?: contact.businessFax
        if (text != null) {
            view.text = text
            view.isVisible = true
        } else {
            view.isVisible = false
        }
    }

    @JvmStatic
    @BindingAdapter("android:srcRes")
    fun setSrcRes(view: ImageView, @DrawableRes res: Int) {
        view.setImageResource(res)
    }

    @JvmStatic
    @BindingAdapter("lowerTextPriority")
    fun setLowerTextPriority(view: TextView, state: Boolean) {
        italic(view, state)

        val a = view.context.theme.obtainStyledAttributes(R.style.AppTheme, intArrayOf(android.R.attr.textColorPrimary, android.R.attr.textColorSecondary))
        val index = if (state) 1 else 0
        val color = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            view.context.resources.getColor(a.getResourceId(index, 0), view.context.theme)
        } else {
            view.context.resources.getColor(a.getResourceId(index, 0))
        }
        view.setTextColor(color)

    }

}