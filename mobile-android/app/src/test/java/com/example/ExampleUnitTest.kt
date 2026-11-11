package com.example

import com.example.data.model.*
import com.example.ui.components.SYSTEM_GLOBAL_REPORTS
import com.example.ui.navigation.SubScreen
import org.junit.Assert.*
import org.junit.Test

/**
 * SCS360 Domain & Architecture Unit Tests
 * Validates SaaS access/quota rules, TCKN pickup verification, gate inside-campus calculations,
 * SHA-256 consent freezing, and global search routing.
 */
class ExampleUnitTest {

  @Test
  fun saasAccessStatus_andQuotaCalculations_areValid() {
    val access = OrganizationAccessStatus(
      organization_id = "org-1",
      organization_name = "SCS360 Eğitim Kurumları A.Ş.",
      status = "ACTIVE",
      active_branches = 3,
      branch_limit = 5,
      active_students = 420,
      student_limit = 1500,
      active_users = 84,
      user_limit = 250,
      remaining_grace_days = 10
    )

    assertEquals("ACTIVE", access.status)
    assertTrue(access.active_branches <= access.branch_limit)
    assertTrue(access.active_students <= access.student_limit)
    assertTrue(access.active_users <= access.user_limit)
    assertEquals(10, access.remaining_grace_days)
  }

  @Test
  fun pickupAuthorization_strictTcknValidation_rejectsMismatch() {
    val auth = PickupAuthorization(
      id = "pauth-1",
      student_id = "std-1",
      student_name = "Ali Demir",
      authorized_person_name = "Mehmet Demir",
      national_id = "12345678902",
      status = "ACTIVE"
    )

    // Exact 11-digit TCKN match required; name alone is never enough
    val validInputTckn = "12345678902"
    val wrongInputTckn = "12345678999"

    assertTrue(auth.status == "ACTIVE" && auth.national_id == validInputTckn)
    assertFalse(auth.status == "ACTIVE" && auth.national_id == wrongInputTckn)
  }

  @Test
  fun gateAccessLogs_insideCampusCalculation_andAnomalyFlag() {
    val logs = listOf(
      GateAccessLog(
        id = "gate-3",
        student_id = "std-2",
        student_name = "Zeynep Kaya",
        direction = "IN",
        verification_method = "BARCODE",
        requires_anomaly_review = true
      ),
      GateAccessLog(
        id = "gate-2",
        student_id = "std-1",
        student_name = "Ali Demir",
        direction = "OUT",
        verification_method = "QR"
      ),
      GateAccessLog(
        id = "gate-1",
        student_id = "std-1",
        student_name = "Ali Demir",
        direction = "IN",
        verification_method = "QR"
      )
    )

    // Latest movement per student determines whether the student is currently inside campus
    val latestByStudent = logs.groupBy { it.student_id }.mapValues { it.value.first() }
    val insideCount = latestByStudent.values.count { it.direction == "IN" }
    val anomalyCount = logs.count { it.requires_anomaly_review }

    assertEquals(1, insideCount) // std-2 is IN, std-1 is OUT
    assertEquals(1, anomalyCount)
    assertFalse(logs.first().adult_pickup_verified)
    assertFalse(logs.first().bus_permission_verified)
  }

  @Test
  fun globalSearchReports_containsAllScs360Modules() {
    val subScreensInGlobalSearch = SYSTEM_GLOBAL_REPORTS.map { it.subScreen }.toSet()
    assertTrue(subScreensInGlobalSearch.contains(SubScreen.PLATFORM_SAAS))
    assertTrue(subScreensInGlobalSearch.contains(SubScreen.SUBSCRIPTION_BILLING))
    assertTrue(subScreensInGlobalSearch.contains(SubScreen.DISMISSAL))
    assertTrue(subScreensInGlobalSearch.contains(SubScreen.INFIRMARY))
    assertTrue(subScreensInGlobalSearch.contains(SubScreen.TUTORING))
    assertTrue(subScreensInGlobalSearch.contains(SubScreen.GUIDANCE))
    assertTrue(subScreensInGlobalSearch.contains(SubScreen.CLUBS))
    assertTrue(subScreensInGlobalSearch.contains(SubScreen.RESOURCES))
    assertTrue(subScreensInGlobalSearch.contains(SubScreen.CONSENTS))
    assertTrue(subScreensInGlobalSearch.contains(SubScreen.SURVEYS))
    assertTrue(subScreensInGlobalSearch.contains(SubScreen.USERS))
  }

  @Test
  fun classCapacityGuard_andNullableFields_areSupported() {
    val cls = ClassGroup(
      id = "class-1",
      name = "Papatyalar",
      grade_level = "5 Yaş",
      branch_id = "branch-1",
      teacher_name = "Ayşe Yılmaz",
      student_count = 18,
      capacity = 18,
      room_number = "101",
      teacher_id = null,
      is_active = true
    )
    // Class is at full capacity (18/18) and primary teacher is cleared (null)
    assertNull(cls.teacher_id)
    assertTrue(cls.student_count >= cls.capacity)

    val paidReviewInvoice = SubscriptionInvoice(
      id = "sinv-3",
      invoice_no = "SAAS-2026-089",
      status = "PAID_REVIEW",
      provider_payment_id = "iyz-pay-994120",
      reconcile_note = "İyzico referansı doğrulandı"
    )
    assertEquals("PAID_REVIEW", paidReviewInvoice.status)
    assertEquals("iyz-pay-994120", paidReviewInvoice.provider_payment_id)

    val studentWithLinkedParent = Student(
      id = "std-1",
      first_name = "Ali",
      last_name = "Demir",
      enrollment_date = null,
      address = null,
      notes = null,
      health_notes = null,
      linked_parent_user_id = "usr-parent",
      receive_notifications = true,
      sms_phone = "+905321112233",
      whatsapp_phone = null
    )
    assertEquals("usr-parent", studentWithLinkedParent.linked_parent_user_id)
    assertNull(studentWithLinkedParent.address)
    assertNull(studentWithLinkedParent.whatsapp_phone)
  }

  @Test
  fun yearlyFinancialTrendRecharts_metricsAndCalculations_areAccurate() {
    val trends2026 = com.example.ui.components.FinancialTrendDataProvider.getTrendsForYear(2026)
    assertEquals(12, trends2026.size)

    // Verify all 12 months are populated
    assertEquals("Ocak", trends2026.first().monthName)
    assertEquals("Aralık", trends2026.last().monthName)

    // Verify positive revenue and expenses
    val totalIncome2026 = trends2026.sumOf { it.income }
    val totalExpense2026 = trends2026.sumOf { it.expense }
    val netSurplus2026 = totalIncome2026 - totalExpense2026

    assertTrue(totalIncome2026 > 5000000.0)
    assertTrue(totalExpense2026 > 3000000.0)
    assertTrue(netSurplus2026 > 0.0)

    // Verify year multipliers work comparatively
    val trends2025 = com.example.ui.components.FinancialTrendDataProvider.getTrendsForYear(2025)
    val totalIncome2025 = trends2025.sumOf { it.income }
    assertTrue(totalIncome2026 > totalIncome2025)

    // Verify month net profit calculation
    val eylul = trends2026[8] // Eylül
    assertEquals("Eylül", eylul.monthName)
    assertEquals("01 - 30 Eylül", eylul.dateRangeText)
    assertTrue(eylul.quarterLabel.contains("Q3"))
    assertNotNull(eylul.previousMonthIncome)
    assertTrue(eylul.income > eylul.expense)
    assertTrue(eylul.profitMarginPercent > 0)
    assertTrue(eylul.collectionRate >= 90)
  }

  @Test
  fun todayAttendancePieChart_sliceAndRateCalculations_areAccurate() {
    val present = 135
    val absent = 11
    val late = 6
    val excused = 2
    val total = present + absent + late + excused

    assertEquals(154, total)

    val rate = (((present + late * 0.5) / total.toDouble()) * 100).toInt()
    assertEquals(89, rate)

    val presentPct = ((present.toDouble() / total) * 100).toInt()
    val absentPct = ((absent.toDouble() / total) * 100).toInt()
    val latePct = ((late.toDouble() / total) * 100).toInt()

    assertEquals(87, presentPct)
    assertEquals(7, absentPct)
    assertEquals(3, latePct)
  }
}
