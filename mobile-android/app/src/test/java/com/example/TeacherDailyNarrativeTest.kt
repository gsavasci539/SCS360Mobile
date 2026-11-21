package com.example

import com.example.util.AiAnalyticsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TeacherDailyNarrativeTest {
    @Test fun draftContainsOnlyExplicitObservations() {
        val draft = AiAnalyticsEngine.generateTeacherDailyNarrative(
            " Öğrenci ", mealStatus = "", sleepStatus = "20 dakika", mood = "",
            activities = "", customTeacherNote = "Öğretmen gözlemi",
        )
        assertEquals("Öğrenci", draft.student_name)
        assertEquals("Uyku: 20 dakika\nÖğretmen notu: Öğretmen gözlemi", draft.narrative_text)
        assertTrue(draft.student_id.isEmpty())
        assertTrue(draft.suggested_activities.isEmpty())
        assertTrue(runCatching { AiAnalyticsEngine.generateTeacherDailyNarrative("Öğrenci", "", "", "", "") }.isFailure)
    }
}
