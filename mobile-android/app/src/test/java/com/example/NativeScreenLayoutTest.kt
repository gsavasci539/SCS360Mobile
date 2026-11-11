package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NativeScreenLayoutTest {
    @get:Rule val compose = createComposeRule()
    private val students = List(3) { i -> Student(id = "synthetic-$i", first_name = "Uzun ad içeren örnek öğrenci", last_name = "Örnek soyadı",
        parent_name = "Uzun veli adı ve soyadı", class_name = "Uzun sınıf adı", student_number = "12345678$i") }
    private val state = Okul360UiState(currentUser = User(id = "synthetic-user", full_name = "Örnek kullanıcı", role = "ADMIN"), students = students)

    @OptIn(ExperimentalTestApi::class)
    @Test fun platformCreationRemainsReachableAtLargeFontInPortraitAndLandscape() {
        var size by mutableStateOf(DpSize(360.dp, 780.dp))
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(size) then DeviceConfigurationOverride.FontScale(2f)) {
                Okul360Theme { AppContentWrapper { key(size) {
                    PlatformSaasScreen(UserRole.SUPERADMIN, plans = emptyList(), tenants = emptyList(), invoices = emptyList(),
                        onCreatePlan = {}, onCreateTenantWithOwner = { null }, onUpdateTenantSubscription = { _, _, _, _ -> null },
                        onToggleTenantActive = { _, _ -> }, onVerifyInvoicePayment = { _, _, _, _, _ -> })
                } } }
            }
        }
        for (window in listOf(DpSize(360.dp, 780.dp), DpSize(780.dp, 360.dp), DpSize(320.dp, 600.dp))) {
            compose.runOnIdle { size = window }
            compose.onNodeWithTag("platform_list_0").performScrollToNode(hasTestTag("create_tenant_btn"))
            compose.onNodeWithTag("create_tenant_btn").assertIsDisplayed().assertWidthIsAtLeast(48.dp)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun actualStudentAndPlatformScreensKeepVisibleActionsInsideTheirWindows() {
        data class Scenario(val width: Int, val height: Int, val font: Float, val platform: Boolean)
        val cases = listOf(320 to 780, 360 to 780, 412 to 915, 780 to 360, 1280 to 800).flatMap { (w, h) ->
            listOf(1f, 2f).flatMap { font -> listOf(false, true).map { Scenario(w, h, font, it) } }
        }
        var current by mutableStateOf(cases.first())
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(current.width.dp, current.height.dp)) then
                DeviceConfigurationOverride.FontScale(current.font)) {
                Okul360Theme { AppContentWrapper { key(current) {
                    if (current.platform) PlatformSaasScreen(currentUserRole = UserRole.SUPERADMIN,
                        plans = listOf(SubscriptionPlan(id = "synthetic-plan", name = "Uzun adı olan örnek paket")),
                        tenants = listOf(PlatformTenantSummary(organization_id = "synthetic-org", organization_name = "Uzun adı olan örnek eğitim kurumu")),
                        invoices = emptyList(), onCreatePlan = {}, onCreateTenantWithOwner = { null },
                        onUpdateTenantSubscription = { _, _, _, _ -> null }, onToggleTenantActive = { _, _ -> },
                        onVerifyInvoicePayment = { _, _, _, _, _ -> })
                    else StudentsScreen(state, onSearchChange = {}, onFilterClassChange = {}, onSelectStudent = {}, onOpenAddStudent = {},
                        onUpdateStudent = {}, onDeleteStudent = {})
                } } }
            }
        }
        for (candidate in cases) {
            compose.runOnIdle { current = candidate }
            compose.waitForIdle()
            val viewport = compose.onRoot().getUnclippedBoundsInRoot()
            compose.onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach { node ->
                val visible = node.boundsInRoot
                if (visible.width <= 0 || visible.height <= 0) return@forEach
                var ancestor = node.parent
                var horizontallyScrollable = false
                while (ancestor != null) {
                    if (ancestor.config.contains(SemanticsProperties.HorizontalScrollAxisRange)) horizontallyScrollable = true
                    ancestor = ancestor.parent
                }
                if (!horizontallyScrollable) {
                    // Coordinates are in pixels here; use the enclosing root's
                    // pixel bounds to include density-forced test windows.
                    val root = compose.onRoot().fetchSemanticsNode()
                    val left = node.positionInRoot.x
                    val right = left + node.size.width
                    assertTrue("Action overflows left at $candidate: ${node.config}", left >= root.positionInRoot.x - 1f)
                    assertTrue("Action overflows right at $candidate: ${node.config}", right <= root.positionInRoot.x + root.size.width + 1f)
                }
            }
            assertTrue((viewport.right - viewport.left).value > 0)
        }
    }
}
