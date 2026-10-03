package com.example.learnlog.ui.planner

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.learnlog.R
import com.example.learnlog.data.entity.TaskEntity
import com.example.learnlog.databinding.BottomSheetRescheduleTaskBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneOffset
import org.threeten.bp.temporal.TemporalAdjusters

/** Quick reschedule options for a task; keeps the task's time of day unless a new one is picked. */
class RescheduleTaskBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetRescheduleTaskBinding? = null
    private val binding get() = _binding!!

    private var onReschedule: ((LocalDate, LocalTime) -> Unit)? = null
    private var task: TaskEntity? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetRescheduleTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // The callback can't survive re-creation (rotation); close rather than do nothing on tap
        if (onReschedule == null) {
            dismissAllowingStateLoss(); return
        }
        @Suppress("DEPRECATION")
        task = arguments?.getParcelable(ARG_TASK)
        val today = LocalDate.now()
        binding.btnToday.setOnClickListener { pick(today) }
        binding.btnTomorrow.setOnClickListener { pick(today.plusDays(1)) }
        binding.btnThisWeekend.setOnClickListener {
            pick(if (today.dayOfWeek == DayOfWeek.SATURDAY) today.plusDays(7) else today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY)))
        }
        binding.btnNextWeek.setOnClickListener { pick(today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))) }
        binding.btnPickDateTime.setOnClickListener { showDateTimePicker() }
    }

    private val currentTime: LocalTime get() = task?.dueAt?.toLocalTime() ?: LocalTime.of(23, 59)

    private fun pick(date: LocalDate, time: LocalTime = currentTime) {
        onReschedule?.invoke(date, time)
        dismiss()
    }

    private fun showDateTimePicker() {
        val start = (task?.dueAt?.toLocalDate() ?: LocalDate.now()).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.pick_date)
            .setSelection(start)
            .build()
        datePicker.addOnPositiveButtonClickListener { millis ->
            val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(if (DateFormat.is24HourFormat(requireContext())) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
                .setHour(currentTime.hour)
                .setMinute(currentTime.minute)
                .setTitleText(R.string.pick_time)
                .build()
            timePicker.addOnPositiveButtonClickListener { pick(date, LocalTime.of(timePicker.hour, timePicker.minute)) }
            timePicker.show(childFragmentManager, "time")
        }
        datePicker.show(childFragmentManager, "date")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TASK = "task"

        fun newInstance(task: TaskEntity, onReschedule: (LocalDate, LocalTime) -> Unit) =
            RescheduleTaskBottomSheet().apply {
                arguments = Bundle().apply { putParcelable(ARG_TASK, task) }
                this.onReschedule = onReschedule
            }
    }
}
