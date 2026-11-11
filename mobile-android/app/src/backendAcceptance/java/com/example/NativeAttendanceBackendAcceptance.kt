package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Explicit opt-in source set, run only by the temporary real-backend fixture. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class NativeAttendanceBackendAcceptance {
    private fun env(key: String) = System.getenv(key) ?: error("Temporary backend fixture required: $key")

    @Test fun realBackendPaginationVersionedEditingLostReplyReplayAndPermissions() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val base = ApiClientProvider.activeBaseUrl; val token = ApiClientProvider.interceptor.authToken
        val workspace = ApiClientProvider.interceptor.workspaceOrganizationId
        try {
            ApiClientProvider.createService(env("NATIVE_TEST_API_URL"))
            ApiClientProvider.interceptor.setSession(env("NATIVE_TEST_ROOT_TOKEN"), workspace = env("NATIVE_TEST_ORG"))
            val editor = NativeAttendanceEditor(context, env("NATIVE_TEST_ROOT_ID"), env("NATIVE_TEST_CLASS"), env("NATIVE_TEST_DAY"))
            val oldEditor = NativeAttendanceEditor(context, env("NATIVE_TEST_ROOT_ID"), env("NATIVE_TEST_CLASS"), env("NATIVE_TEST_DAY"))
            val rows = editor.load(); val stale = oldEditor.load()
            assertEquals(102, rows.size)
            assertEquals(1, rows.count { it.status == "UNRECORDED" })
            assertTrue(rows.all { it.temperature_celsius == null && it.parent_notified == null })
            val original = rows.first { it.student_id == env("NATIVE_TEST_FIRST") }
            assertEquals("Original note", original.note)
            val draft = rows.map { when (it.student_id) {
                env("NATIVE_TEST_FIRST") -> it.copy(status = "LATE", note = "Native düzenleme Ω")
                env("NATIVE_TEST_NEW") -> it.copy(status = "EXCUSED", note = "Native yeni not")
                else -> it
            } }
            try { editor.save(draft); fail("Fixture must drop the first committed response") } catch (_: java.io.IOException) { }
            assertTrue(editor.hasPending)
            val replay = editor.save(draft)
            assertEquals(1, replay.getInt("created")); assertEquals(1, replay.getInt("updated")); assertEquals(100, replay.getInt("unchanged"))
            assertFalse(editor.hasPending)
            try { oldEditor.save(stale.map { if (it.student_id == original.student_id) it.copy(status = "PRESENT") else it }); fail("Stale editor must fail") }
            catch (failure: AttendanceHttpException) { assertEquals(409, failure.code) }
            assertFalse(oldEditor.ready)
            val fresh = oldEditor.load()
            assertEquals("Native düzenleme Ω", fresh.first { it.student_id == original.student_id }.note)
            assertNotEquals(original.version, fresh.first { it.student_id == original.student_id }.version)

            val profile = NativeAuthentication(ApiClientProvider.createService(ApiClientProvider.activeBaseUrl)).restore(env("NATIVE_TEST_TEACHER_TOKEN"))
            assertEquals(env("NATIVE_TEST_TEACHER_ID"), profile.id)
            assertTrue("attendance.read" in profile.permissions)
            assertFalse("attendance.write" in profile.permissions)
            val readOnly = NativeAttendanceEditor(context, env("NATIVE_TEST_TEACHER_ID"), env("NATIVE_TEST_CLASS"), env("NATIVE_TEST_DAY"))
            val visible = readOnly.load()
            try { readOnly.save(visible.map { if (it.student_id == original.student_id) it.copy(status = "PRESENT") else it }); fail("Read-only teacher must not write") }
            catch (failure: AttendanceHttpException) { assertEquals(403, failure.code) }

            ApiClientProvider.interceptor.setSession(env("NATIVE_TEST_ROOT_TOKEN"), workspace = env("NATIVE_TEST_OTHER_ORG"))
            val foreign = NativeAttendanceEditor(context, env("NATIVE_TEST_ROOT_ID"), env("NATIVE_TEST_CLASS"), env("NATIVE_TEST_DAY"))
            try { foreign.load(); fail("Another institution must not read this class") }
            catch (failure: AttendanceHttpException) { assertEquals(404, failure.code) }
            assertFalse(foreign.ready)
        } finally { ApiClientProvider.createService(base); ApiClientProvider.interceptor.setSession(token, workspace = workspace) }
    }
}
