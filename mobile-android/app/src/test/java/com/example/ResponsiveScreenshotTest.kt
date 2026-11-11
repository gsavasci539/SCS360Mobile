package com.example

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.*
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w360dp-h780dp-mdpi")
class ResponsiveScreenshotTest {
    @get:Rule val compose = createComposeRule()
    private val state = Okul360UiState(currentUser = User(id = "synthetic-user", full_name = "Örnek kullanıcı", role = "ADMIN"),
        students = List(6) { i -> Student(id = "synthetic-$i", first_name = "Uzun isimli örnek", last_name = "Öğrenci $i",
            parent_name = "Örnek veli adı soyadı", class_name = "Uzun isimli örnek sınıf", student_number = "12345678$i") })
    @Composable private fun Students() {
        Okul360Theme { AppContentWrapper {
            StudentsScreen(state, onSearchChange = {}, onFilterClassChange = {}, onSelectStudent = {}, onOpenAddStudent = {},
                onUpdateStudent = {}, onDeleteStudent = {})
        } }
    }
    @Test fun phoneCards() {
        compose.setContent { Students() }
        compose.onNodeWithTag("students_list").performScrollToNode(hasTestTag("table_view_toggle"))
        compose.onRoot().captureRoboImage("../../output/native-responsive-cards-20261111.png")
    }
    @Test fun phoneTable() {
        compose.setContent { Students() }
        compose.onNodeWithTag("students_list").performScrollToNode(hasTestTag("table_view_toggle"))
        compose.onNodeWithTag("table_view_toggle").performClick()
        compose.onRoot().captureRoboImage("../../output/native-responsive-table-20261111.png")
    }
    @OptIn(ExperimentalTestApi::class)
    @Test fun phoneLargeTextPlatform() {
        compose.setContent { DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
            Okul360Theme { AppContentWrapper {
                PlatformSaasScreen(UserRole.SUPERADMIN, plans = listOf(SubscriptionPlan(id = "synthetic-plan", name = "Örnek paket")),
                    tenants = emptyList(), invoices = emptyList(), onCreatePlan = {}, onCreateTenantWithOwner = { null },
                    onUpdateTenantSubscription = { _, _, _, _ -> null }, onToggleTenantActive = { _, _ -> }, onVerifyInvoicePayment = { _, _, _, _, _ -> })
            } }
        } }
        compose.onRoot().captureRoboImage("../../output/native-responsive-root-font2-20261111.png")
    }

    @OptIn(ExperimentalTestApi::class)
    @Config(qualifiers = "w780dp-h360dp-mdpi")
    @Test fun landscapeLargeTextPlatform() {
        compose.setContent { DeviceConfigurationOverride(DeviceConfigurationOverride.FontScale(2f)) {
            Okul360Theme { AppContentWrapper {
                PlatformSaasScreen(UserRole.SUPERADMIN, plans = emptyList(), tenants = emptyList(), invoices = emptyList(),
                    onCreatePlan = {}, onCreateTenantWithOwner = { null }, onUpdateTenantSubscription = { _, _, _, _ -> null },
                    onToggleTenantActive = { _, _ -> }, onVerifyInvoicePayment = { _, _, _, _, _ -> })
            } }
        } }
        compose.onNodeWithTag("platform_list_0").performScrollToNode(hasTestTag("create_tenant_btn"))
        compose.onNodeWithTag("create_tenant_btn").assertIsDisplayed()
        compose.onRoot().captureRoboImage("../../output/native-responsive-root-landscape-font2-20261111.png")
    }
}
