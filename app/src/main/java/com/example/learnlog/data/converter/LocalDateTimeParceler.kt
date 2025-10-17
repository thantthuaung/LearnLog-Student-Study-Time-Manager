package com.example.learnlog.data.converter

import android.os.Parcel
import kotlinx.parcelize.Parceler
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

object LocalDateTimeParceler : Parceler<LocalDateTime?> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override fun create(parcel: Parcel): LocalDateTime? {
        return parcel.readString()?.let { LocalDateTime.parse(it, formatter) }
    }

    override fun LocalDateTime?.write(parcel: Parcel, flags: Int) {
        parcel.writeString(this?.format(formatter))
    }
}
