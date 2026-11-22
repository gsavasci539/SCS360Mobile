package com.example.ui.screens

import com.example.data.model.AttendanceStatus
import com.example.data.remote.AttendanceMonthDay

internal fun attendanceMonthCsv(rows: List<AttendanceMonthDay>): String {
    if (rows.isEmpty()) return ""
    val columns = listOf(AttendanceStatus.PRESENT, AttendanceStatus.ABSENT, AttendanceStatus.EXCUSED,
        AttendanceStatus.LATE, AttendanceStatus.HALF_DAY)
    fun cell(value: String): String {
        val safe = if (value.trimStart().firstOrNull()?.let { it in "=+-@" } == true) "'$value" else value
        return "\"" + safe.replace("\"", "\"\"") + "\""
    }
    return buildString {
        append('\uFEFF')
        append(listOf("Tarih", "Sınıf", "Toplam öğrenci", "Geldi", "Gelmedi", "İzinli", "Geç", "Yarım gün").joinToString(",", transform = ::cell))
        rows.forEach { row ->
            append("\r\n")
            val values = listOf(row.day.toString(), row.className.orEmpty(), row.total.toString()) +
                columns.map { row.statusCounts.getValue(it).toString() }
            append(values.joinToString(",", transform = ::cell))
        }
    }
}
