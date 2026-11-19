package com.example.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException

internal fun currentDailyReportDate(): String = LocalDate.now(ZoneId.of("Europe/Istanbul")).toString()

internal fun parseDailyReportDate(value: String): LocalDate? = try {
    LocalDate.parse(value).takeIf { it.year in 1..9999 && it.toString() == value }
} catch (_: DateTimeParseException) {
    null
}

@Composable
internal fun DailyReportDateField(value: String, valid: Boolean, enabled: Boolean,
    testTag: String = "daily_report_date", onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 10) onChange(it) },
        modifier = Modifier.fillMaxWidth().testTag(testTag),
        enabled = enabled,
        label = { Text("Karne tarihi (YYYY-AA-GG)") },
        isError = !valid,
        supportingText = if (valid) null else ({ Text("Geçerli bir karne tarihi girin.") }),
        singleLine = true,
    )
}
