package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.RecordedFinanceSummary
import com.example.ui.components.*
import com.example.ui.theme.Okul360Theme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w320dp-h600dp-mdpi")
class NativeFinanceSummaryUiTest {
    @get:Rule val compose = createComposeRule()
    private var oldFont = 1f
    @Before fun font() { oldFont = RuntimeEnvironment.getFontScale(); RuntimeEnvironment.setFontScale(2f) }
    @After fun restore() { RuntimeEnvironment.setFontScale(oldFont) }
    @Test fun errorHasWorkingRetryWithoutFabricatedAmounts() {
        var clicks = 0
        compose.setContent { Okul360Theme { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            RecordedFinanceSummaryCard(null, false, "Sunucu hatası", { clicks++ })
        } } }
        compose.onNodeWithText("Sunucu hatası").assertIsDisplayed()
        compose.onNodeWithTag("finance_summary_retry").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(1, clicks)
        compose.onAllNodesWithText("0,00", substring = true).assertCountEquals(0)
        compose.onAllNodesWithText("480.000", substring = true).assertCountEquals(0)
    }
    @Test @Config(qualifiers = "w780dp-h360dp-land-mdpi")
    fun unknownAndZeroAreDistinctAtLargeFontAndHistoryIsNotInvented() {
        val value = RecordedFinanceSummary("2026-11-12", null, null, null, null, BigDecimal.ZERO,
            BigDecimal.ZERO, null, 0, 0, true, emptyList())
        compose.setContent { Okul360Theme { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            RecordedFinanceSummaryCard(value, false, null, {})
        } } }
        compose.onAllNodesWithText("Bilinmiyor", substring = false).assertCountEquals(3)
        compose.onNodeWithText("Tamamlanmış tahsilat geçmişi yok.").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("%88", substring = true).assertCountEquals(0)
    }
    @Test fun oldFinanceVisualizationsCannotGenerateSampleMoneyOrStudentMatches() {
        compose.setContent { Okul360Theme { Column(Modifier.verticalScroll(rememberScrollState())) {
            FinancialSummaryCard(); BranchFinancialOverviewChart(); YearlyFinancialTrendRechartsView()
        } } }
        compose.onNodeWithText("Öğrenciye ait doğrulanmış finans özeti henüz yüklenmedi.").assertExists()
        compose.onNodeWithText("Şube bazında gelir ve gider kayıtları henüz yüklenmedi.").assertExists()
        compose.onAllNodesWithText("550.000", substring = true).assertCountEquals(0)
    }
}
