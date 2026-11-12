package com.example.ui.components

import com.example.ui.viewmodel.Okul360UiState

/** Only presentation state is reset; pending ledger request identities remain durable. */
data class FinancePresentationScope(
    val loggedIn: Boolean, val actor: String, val organization: String?, val workspace: String?,
    val branch: String, val role: String, val permissions: Set<String>, val root: Boolean
)

fun financePresentationScope(state: Okul360UiState) = FinancePresentationScope(
    state.isLoggedIn, state.currentUser.id, state.currentUser.organization_id, state.workspaceOrganizationId,
    state.selectedBranchId, state.selectedRole.name, state.currentUser.permissions.toSet(), state.currentUser.is_super_admin == true
)
