package com.example.learnlog.ui.settings

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.learnlog.R
import com.example.learnlog.databinding.ItemSettingRowBinding
import com.example.learnlog.databinding.ItemSettingsGroupBinding
import com.google.android.material.color.MaterialColors

/** Builds grouped settings cards with consistent rows, so every section looks the same. */
class SettingsBuilder(private val container: LinearLayout) {

    private val inflater = LayoutInflater.from(container.context)

    fun custom(view: View) = container.addView(view)

    fun group(title: CharSequence?, build: Group.() -> Unit): Group {
        val g = ItemSettingsGroupBinding.inflate(inflater, container, false)
        g.groupTitle.text = title
        g.groupTitle.isVisible = !title.isNullOrBlank()
        container.addView(g.root)
        return Group(g).apply(build)
    }

    inner class Group(val binding: ItemSettingsGroupBinding) {
        private val rows: LinearLayout get() = binding.groupRows

        fun row(
            @DrawableRes icon: Int,
            title: CharSequence,
            subtitle: CharSequence? = null,
            value: CharSequence? = null,
            chevron: Boolean = true,
            destructive: Boolean = false,
            onClick: (() -> Unit)? = null
        ): Row {
            val b = newRow(icon, title, subtitle, destructive)
            b.rowValue.text = value
            b.rowValue.isVisible = value != null
            b.rowChevron.isVisible = chevron && onClick != null
            if (onClick != null) b.row.setOnClickListener { onClick() } else {
                b.row.isClickable = false
                b.row.background = null
            }
            return Row(b)
        }

        fun switch(
            @DrawableRes icon: Int,
            title: CharSequence,
            subtitle: CharSequence? = null,
            onChange: (Boolean) -> Unit
        ): Row {
            val b = newRow(icon, title, subtitle, false)
            b.rowSwitch.isVisible = true
            b.rowSwitch.setOnCheckedChangeListener { button, checked -> if (button.isPressed) onChange(checked) }
            b.row.setOnClickListener { b.rowSwitch.isPressed = true; b.rowSwitch.toggle(); b.rowSwitch.isPressed = false }
            return Row(b)
        }

        private fun newRow(@DrawableRes icon: Int, title: CharSequence, subtitle: CharSequence?, destructive: Boolean): ItemSettingRowBinding {
            if (rows.childCount > 0) rows.addView(divider())
            val b = ItemSettingRowBinding.inflate(inflater, rows, false)
            b.rowIcon.setImageResource(icon)
            b.rowTitle.text = title
            b.rowSubtitle.text = subtitle
            b.rowSubtitle.isVisible = !subtitle.isNullOrBlank()
            if (destructive) {
                val error = ContextCompat.getColor(b.root.context, R.color.ll_error)
                b.rowTitle.setTextColor(error)
                b.rowIcon.imageTintList = ColorStateList.valueOf(error)
                b.rowIconTile.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(b.root.context, R.color.ll_error_container))
            }
            rows.addView(b.root)
            return b
        }

        private fun divider() = View(container.context).apply {
            val density = resources.displayMetrics.density
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (1 * density).toInt()).apply {
                marginStart = (70 * density).toInt()
            }
            setBackgroundColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutlineVariant))
        }
    }

    class Row(val binding: ItemSettingRowBinding) {
        var value: CharSequence?
            get() = binding.rowValue.text
            set(v) {
                binding.rowValue.text = v
                binding.rowValue.isVisible = v != null
            }

        var subtitle: CharSequence?
            get() = binding.rowSubtitle.text
            set(v) {
                binding.rowSubtitle.text = v
                binding.rowSubtitle.isVisible = !v.isNullOrBlank()
            }

        var checked: Boolean
            get() = binding.rowSwitch.isChecked
            set(v) { if (binding.rowSwitch.isChecked != v) binding.rowSwitch.isChecked = v }

        var enabled: Boolean
            get() = binding.row.isEnabled
            set(v) {
                binding.row.isEnabled = v
                binding.rowSwitch.isEnabled = v
                binding.row.alpha = if (v) 1f else 0.45f
            }
    }
}
