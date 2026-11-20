package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ParentAttendanceMonth(
    val student_id: String,
    val year: Int,
    val month: Int,
    val calendar: Map<String, ParentAttendanceDay>,
    val summary: ParentAttendanceSummary,
)

@JsonClass(generateAdapter = true)
data class ParentAttendanceDay(
    val date: String,
    val status: String?,
    val check_in_time: String?,
    val check_out_time: String?,
    val note: String?,
)

@JsonClass(generateAdapter = true)
data class ParentAttendanceSummary(val present: Int, val absent: Int, val late: Int, val excused: Int, val total: Int)
