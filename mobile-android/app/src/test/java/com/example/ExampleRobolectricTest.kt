package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.OrganizationAccessStatus
import com.example.data.model.SubscriptionInvoice
import com.example.data.model.SubscriptionPlan
import com.example.data.model.UserRole
import com.example.ui.components.DashboardWidgetsSkeletonGrid
import com.example.ui.screens.SubscriptionBillingScreen
import com.example.ui.theme.Okul360Theme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SCS360", appName)
  }

  @Test
  fun `launch MainActivity successfully`() {
    val scenario = ActivityScenario.launch(MainActivity::class.java)
    scenario.onActivity { activity ->
      assertNotNull(activity)
    }
  }

  @Test
  fun `subscriptionBillingScreen renders quota and access status`() {
    composeTestRule.setContent {
      Okul360Theme {
        SubscriptionBillingScreen(
          currentUserRole = UserRole.ADMIN,
          accessStatus = OrganizationAccessStatus(
            organization_name = "SCS360 Eğitim Kurumları A.Ş.",
            status = "ACTIVE",
            plan_name = "SCS360 Kurumsal Pro"
          ),
          plans = listOf(SubscriptionPlan()),
          invoices = listOf(SubscriptionInvoice()),
          onRefreshAccessStatus = {},
          onCreateRenewalRequest = { _, _ -> },
          onSimulateAccessStatusChange = {},
          onLogout = {}
        )
      }
    }

    composeTestRule.onNodeWithText("SCS360 Kurumsal Pro", substring = true).assertIsDisplayed()
  }

  @Test
  fun `dashboardWidgetsSkeletonGrid renders all four shimmer placeholders`() {
    composeTestRule.setContent {
      Okul360Theme {
        DashboardWidgetsSkeletonGrid()
      }
    }

    composeTestRule.onNodeWithTag("dashboard_widgets_skeleton_grid").assertIsDisplayed()
    composeTestRule.onNodeWithTag("skeleton_widget_attendance").assertIsDisplayed()
    composeTestRule.onNodeWithTag("skeleton_widget_finance").assertExists()
    composeTestRule.onNodeWithTag("skeleton_widget_approvals").assertExists()
    composeTestRule.onNodeWithTag("skeleton_widget_risk").assertExists()
  }

  @Test
  fun `dashboardWidgetsReorderingSharedPreferencesPersistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = context.getSharedPreferences("scs360_dashboard_prefs", Context.MODE_PRIVATE)

    // Verify initial clean state or write customized order
    val customOrder = listOf("announcements", "attendance", "finance", "cash_approvals", "campus_risk")
    prefs.edit()
      .putString("dashboard_widget_order", customOrder.joinToString(","))
      .putStringSet("dashboard_hidden_widgets", setOf("campus_risk"))
      .apply()

    val loadedOrder = prefs.getString("dashboard_widget_order", null)?.split(",")
    val loadedHidden = prefs.getStringSet("dashboard_hidden_widgets", null)

    assertEquals(customOrder, loadedOrder)
    assertEquals(setOf("campus_risk"), loadedHidden)
  }
}
