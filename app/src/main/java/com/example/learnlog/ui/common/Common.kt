package com.example.learnlog.ui.common

import android.animation.ValueAnimator
import android.content.Context
import android.net.Uri
import android.view.View
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.learnlog.R
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.ChronoUnit
import java.util.Locale

/** Implemented by tab screens that scroll to top when their bottom-nav tab is tapped again. */
interface Reselectable {
    fun onReselected()
}

object Motion {
    /** Material "emphasized decelerate" curve used for entrances. */
    val EMPHASIZED = PathInterpolator(0.05f, 0.7f, 0.1f, 1f)

    /** Fades views in while they rise a little, one after another. */
    fun staggerIn(views: List<View>, startDelay: Long = 0, step: Long = 55) {
        views.forEachIndexed { i, v ->
            v.alpha = 0f
            v.translationY = v.resources.displayMetrics.density * 18
            v.animate().alpha(1f).translationY(0f)
                .setStartDelay(startDelay + i * step)
                .setDuration(420)
                .setInterpolator(EMPHASIZED)
                .start()
        }
    }

    /** Small bounce used to celebrate an action (task completed, goal reached). */
    fun pop(view: View) {
        view.animate().cancel()
        view.scaleX = 0.82f
        view.scaleY = 0.82f
        view.animate().scaleX(1f).scaleY(1f).setDuration(380)
            .setInterpolator(OvershootInterpolator(2.6f)).start()
    }

    /** Counts a number up from its current value, e.g. on stat tiles. */
    fun countTo(view: TextView, target: Int, durationMs: Long = 700, format: (Int) -> String) {
        val from = (view.getTag(R.id.tag_count_value) as? Int) ?: 0
        view.setTag(R.id.tag_count_value, target)
        if (from == target) {
            view.text = format(target); return
        }
        ValueAnimator.ofInt(from, target).apply {
            duration = durationMs
            interpolator = EMPHASIZED
            addUpdateListener { view.text = format(it.animatedValue as Int) }
            start()
        }
    }

    /** Plays the list's layout animation once, the first time data arrives. */
    fun RecyclerView.animateFirstLayout() {
        if (getTag(R.id.tag_animated) == true) return
        setTag(R.id.tag_animated, true)
        scheduleLayoutAnimation()
    }
}

object Format {
    /** "25 min", "1 h 30 min", "2 h". */
    fun minutes(context: Context, minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h == 0 -> context.getString(R.string.minutes_short, m)
            m == 0 -> context.getString(R.string.hours_short, h)
            else -> context.getString(R.string.hours_minutes_short, h, m)
        }
    }

    /** Compact form for tiles and charts: "45m", "1h 30m", "2h". */
    fun compact(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h == 0 -> "${m}m"
            m == 0 -> "${h}h"
            else -> "${h}h ${m}m"
        }
    }

    private val time = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    private val dayDate = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())
    private val dayDateYear = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

    fun time(dt: LocalDateTime): String = dt.format(time)

    /** Friendly due label: "Today · 5:00 PM", "Tomorrow · 9:00 AM", "Mon, 6 Oct · 11:59 PM". */
    fun due(context: Context, due: LocalDateTime?, now: LocalDateTime = LocalDateTime.now()): String {
        if (due == null) return context.getString(R.string.no_due_date)
        val days = ChronoUnit.DAYS.between(now.toLocalDate(), due.toLocalDate())
        val t = due.format(time)
        return when (days) {
            0L -> context.getString(R.string.due_today_at, t)
            1L -> context.getString(R.string.due_tomorrow_at, t)
            -1L -> context.getString(R.string.due_yesterday_at, t)
            else -> {
                val pattern = if (due.year == now.year) dayDate else dayDateYear
                "${due.format(pattern)} · $t"
            }
        }
    }

    /** "2 days", "3 h", "15 min" – how long ago a deadline passed. */
    fun ago(context: Context, from: LocalDateTime, now: LocalDateTime = LocalDateTime.now()): String {
        val minutes = ChronoUnit.MINUTES.between(from, now).coerceAtLeast(1)
        return when {
            minutes < 60 -> context.getString(R.string.minutes_short, minutes.toInt())
            minutes < 60 * 24 -> context.getString(R.string.hours_short, (minutes / 60).toInt())
            else -> context.resources.getQuantityString(R.plurals.days, (minutes / 1440).toInt(), (minutes / 1440).toInt())
        }
    }

    fun greeting(context: Context, now: LocalDateTime = LocalDateTime.now()): String = context.getString(
        when (now.hour) {
            in 5..11 -> R.string.greeting_morning
            in 12..17 -> R.string.greeting_afternoon
            else -> R.string.greeting_evening
        }
    )

    fun initials(name: String): String = name.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifEmpty { "🙂" }
}

object ProfileAvatar {
    /** Shows the photo when one loads, otherwise the person's initials. */
    fun bind(initials: TextView, photo: ImageView, name: String, photoUri: String?) {
        initials.text = Format.initials(name)
        initials.isVisible = true
        if (photoUri.isNullOrBlank()) {
            photo.isVisible = false
            return
        }
        photo.isVisible = true
        Glide.with(photo)
            .load(Uri.parse(photoUri))
            .circleCrop()
            .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<android.graphics.drawable.Drawable>, isFirstResource: Boolean): Boolean {
                    photo.isVisible = false
                    return true
                }

                override fun onResourceReady(resource: android.graphics.drawable.Drawable, model: Any, target: Target<android.graphics.drawable.Drawable>?, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                    initials.isVisible = false
                    return false
                }
            })
            .into(photo)
    }
}
