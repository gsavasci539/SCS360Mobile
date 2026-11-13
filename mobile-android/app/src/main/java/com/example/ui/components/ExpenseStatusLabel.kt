package com.example.ui.components

import java.util.Locale

/** Only server-provided bookkeeping status is displayed; no payment is inferred. */
fun expenseStatusLabel(status:String?):String? = status?.takeIf { it.isNotBlank() }?.let {
    when(it.lowercase(Locale.ROOT)) {
        "pending" -> "Bekliyor"
        "completed" -> "Tamamlandı"
        "approved" -> "Onaylandı"
        "cancelled", "canceled" -> "İptal edildi"
        "paid" -> "Ödendi"
        else -> it
    }
}
