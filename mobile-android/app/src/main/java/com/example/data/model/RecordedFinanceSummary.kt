package com.example.data.model

import java.math.BigDecimal

/** Server ledger totals are lifetime values, never a monthly forecast or expense estimate. */
data class RecordedFinanceSummary(
    val asOf: String,
    val branchId: String?,
    val totalDebt: BigDecimal?,
    val totalPaid: BigDecimal?,
    val outstandingDebt: BigDecimal?,
    val overdueAmount: BigDecimal?,
    val collectedTotal: BigDecimal?,
    val collectionRate: BigDecimal?,
    val debtCount: Int,
    val paymentCount: Int,
    val requiresReview: Boolean,
    val historicalMonthly: List<RecordedCollectionMonth>
)

data class RecordedCollectionMonth(val month: String, val amount: BigDecimal?, val paymentCount: Int)
