package com.example.learnlog.ui.base

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.learnlog.MainActivity
import com.example.learnlog.R
import com.example.learnlog.auth.AuthManager
import com.example.learnlog.data.preferences.UserPreferences
import com.example.learnlog.ui.common.ProfileAvatar
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Base for the five tab screens. Wires the shared top bar (menu button, title, optional
 * action, avatar). Subclasses must be @AndroidEntryPoint so the fields below are injected.
 */
abstract class BaseFragment : Fragment {

    constructor() : super()
    constructor(@LayoutRes contentLayoutId: Int) : super(contentLayoutId)

    @Inject lateinit var authManager: AuthManager
    @Inject lateinit var userPreferences: UserPreferences

    protected fun setupTopBar(
        root: View,
        title: CharSequence,
        subtitle: CharSequence? = null,
        @DrawableRes actionIcon: Int? = null,
        actionDescription: CharSequence? = null,
        onAction: ((View) -> Unit)? = null
    ) {
        root.findViewById<TextView>(R.id.page_title)?.text = title
        root.findViewById<TextView>(R.id.page_subtitle)?.apply {
            text = subtitle
            isVisible = !subtitle.isNullOrBlank()
        }
        root.findViewById<MaterialButton>(R.id.hamburger_button)?.setOnClickListener {
            (activity as? MainActivity)?.openDrawer()
        }
        root.findViewById<MaterialButton>(R.id.top_bar_action)?.apply {
            if (actionIcon != null && onAction != null) {
                isVisible = true
                setIconResource(actionIcon)
                contentDescription = actionDescription
                setOnClickListener(onAction)
            } else {
                isVisible = false
            }
        }
        root.findViewById<View>(R.id.top_bar_avatar)?.setOnClickListener {
            (activity as? MainActivity)?.openSettings(MainActivity.SECTION_ACCOUNT)
        }
        bindAvatar(root)
    }

    protected fun setTopBarSubtitle(root: View, subtitle: CharSequence?) {
        root.findViewById<TextView>(R.id.page_subtitle)?.apply {
            text = subtitle
            isVisible = !subtitle.isNullOrBlank()
        }
    }

    private fun bindAvatar(root: View) {
        val initials = root.findViewById<TextView>(R.id.top_bar_initials) ?: return
        val photo = root.findViewById<ImageView>(R.id.top_bar_photo) ?: return
        val user = authManager.currentUser
        val name = user?.displayName?.takeIf { it.isNotBlank() } ?: getString(R.string.guest_name)
        viewLifecycleOwner.lifecycleScope.launch {
            userPreferences.avatarUri.collect { local ->
                ProfileAvatar.bind(initials, photo, name, local ?: user?.photoUrl?.toString())
            }
        }
    }

    protected val firstName: String
        get() = authManager.currentUser?.displayName?.trim()?.split(" ")?.firstOrNull()
            ?.takeIf { it.isNotBlank() } ?: getString(R.string.guest_name)
}
