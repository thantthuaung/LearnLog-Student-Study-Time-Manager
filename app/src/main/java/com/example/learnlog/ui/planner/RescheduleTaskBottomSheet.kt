package com.example.learnlog.ui.planner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.learnlog.data.entity.TaskEntity
import com.example.learnlog.databinding.BottomSheetRescheduleTaskBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId

class RescheduleTaskBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetRescheduleTaskBinding? = null
    private val binding get() = _binding!!

    private var task: TaskEntity? = null
    private var onReschedule: ((LocalDate, LocalTime) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetRescheduleTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        task = arguments?.getParcelable(ARG_TASK)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnToday.setOnClickListener {
            rescheduleToDate(LocalDate.now())
        }

        binding.btnTomorrow.setOnClickListener {
            rescheduleToDate(LocalDate.now().plusDays(1))
        }

        binding.btnThisWeekend.setOnClickListener {
            val today = LocalDate.now()
            val daysUntilSaturday = (6 - today.dayOfWeek.value) % 7
            val saturday = if (daysUntilSaturday == 0) {
                today.plusDays(7)
            } else {
                today.plusDays(daysUntilSaturday.toLong())
            }
            rescheduleToDate(saturday)
        }

        binding.btnNextWeek.setOnClickListener {
            rescheduleToDate(LocalDate.now().plusWeeks(1))
        }

        binding.btnPickDateTime.setOnClickListener {
            showDateTimePicker()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun rescheduleToDate(date: LocalDate) {
        val currentTime = task?.dueAt?.toLocalTime() ?: LocalTime.of(23, 59)
        onReschedule?.invoke(date, currentTime)
        dismiss()
    }

    private fun showDateTimePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val selectedDate = Instant.ofEpochMilli(selection)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            val currentTime = task?.dueAt?.toLocalTime() ?: LocalTime.of(23, 59)
            val timePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(currentTime.hour)
                .setMinute(currentTime.minute)
                .setTitleText("Select Time")
                .build()

            timePicker.addOnPositiveButtonClickListener {
                val selectedTime = LocalTime.of(timePicker.hour, timePicker.minute)
                onReschedule?.invoke(selectedDate, selectedTime)
                dismiss()
            }

            timePicker.show(childFragmentManager, "timePicker")
        }

        datePicker.show(childFragmentManager, "datePicker")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TASK = "task"

        fun newInstance(
            task: TaskEntity,
            onReschedule: (LocalDate, LocalTime) -> Unit
        ): RescheduleTaskBottomSheet {
            return RescheduleTaskBottomSheet().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_TASK, task)
                }
                this.onReschedule = onReschedule
            }
        }
    }
}

