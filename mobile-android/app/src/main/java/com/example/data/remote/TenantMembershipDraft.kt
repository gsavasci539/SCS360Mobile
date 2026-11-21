package com.example.data.remote

data class TenantMembershipDraft(
    val planId: String,
    val periodStart: String,
    val periodEnd: String,
    val adminStatus: String,
    val billingOwnerId: String?,
)

data class TenantBillingUser(val id: String, val name: String, val email: String)
