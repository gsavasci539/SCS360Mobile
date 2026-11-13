package com.example

import com.example.data.model.SessionRolePolicy
import com.example.data.model.UserRole
import com.example.data.remote.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class NativeParentParityBackendAcceptance {
    private fun env(name: String) = System.getenv(name) ?: error("Missing isolated fixture input: $name")
    @Test fun realCustomLoginContactAccountStudentAndParentDebtRoundtrip() = runBlocking {
        val old = ApiClientProvider.activeBaseUrl; val interceptor = ApiClientProvider.interceptor; val oldToken = interceptor.authToken
        try {
            val auth = NativeAuthentication(ApiClientProvider.createService(env("NATIVE_TEST_API_URL")))
            val manager = auth.login("native-manager@example.com", "Synthetic-pass-2026!")
            assertEquals(UserRole.CUSTOM, SessionRolePolicy.primary(manager)); assertEquals(false, manager.is_super_admin)
            val managerToken = interceptor.authToken!!
            var api = NativeParentClient(env("NATIVE_TEST_ORGANIZATION"))
            assertEquals(101, api.list("NativeSeed").size)
            val created = api.save(JSONObject().put("first_name", "NativeCreated").put("last_name", "Contact").put("relation_type", "GUARDIAN").put("receive_notifications", true))
            val updated = api.save(JSONObject().put("phone", "05320000001"), created)
            assertEquals("05320000001", api.get(created.id).value("phone"))
            assertTrue(api.accounts(created.id).any { it.id == env("NATIVE_TEST_ACCOUNT") })
            api.linkAccount(created.id, env("NATIVE_TEST_ACCOUNT"))
            api.linkStudent(env("NATIVE_TEST_STUDENT"), created.id, true, true)
            assertTrue(created.id in api.studentParents(env("NATIVE_TEST_STUDENT")))
            assertEquals(env("NATIVE_TEST_ACCOUNT"), api.get(created.id).value("user_id"))
            val parent = auth.login("native-parent@example.com", "Synthetic-pass-2026!")
            assertEquals(UserRole.PARENT, SessionRolePolicy.primary(parent))
            val portal = NativeParentPortalClient(); val child = portal.children().single()
            assertEquals(env("NATIVE_TEST_STUDENT"), child.id)
            val debt = portal.invoices(child).single(); assertEquals(env("NATIVE_TEST_DEBT"), debt.id); assertEquals(100.10, debt.amount, 0.0)
            val day = portal.section(child.id, "today-status") as JSONObject
            assertEquals(child.id, day.getString("student_id")); assertTrue(day.isNull("daily_report"))
            val medicine = portal.submit(child.id, "MEDICINE", JSONObject().put("medicine_name", "Synthetic acceptance request").put("dosage", "Recorded test dose").put("time_to_give", "10:00"))
            assertEquals(child.id, medicine.getString("student_id")); assertEquals("PENDING", medicine.getString("status"))
            val excuse = portal.submit(child.id, "EXCUSE", JSONObject().put("start_date", "2027-01-15").put("end_date", "2027-01-16").put("reason_type", "ILLNESS").put("description", "Synthetic acceptance excuse"))
            assertEquals(child.id, excuse.getString("student_id")); assertEquals("PENDING", excuse.getString("status"))
            val teachers = portal.section(child.id, "teachers") as JSONArray
            assertTrue((0 until teachers.length()).any { teachers.getJSONObject(it).getString("id") == env("NATIVE_TEST_TEACHER") })
            val sent = portal.sendChat(child.id, env("NATIVE_TEST_TEACHER"), "Native acceptance message", parent.id)
            val history = portal.chat(child.id, env("NATIVE_TEST_TEACHER"))
            assertTrue((0 until history.length()).any { history.getJSONObject(it).getString("id") == sent.getString("id") })
            try { NativeParentClient(env("NATIVE_TEST_ORGANIZATION")).list(); fail("Parent staff directory") } catch (failure: RegistryHttpException) { assertEquals(403, failure.code) }
            auth.restore(managerToken); api = NativeParentClient(env("NATIVE_TEST_ORGANIZATION"))
            api.unlinkStudent(child.id, created.id); assertFalse(created.id in api.studentParents(child.id))
            api.linkAccount(created.id, null); assertEquals("", api.get(created.id).value("user_id"))
            api.delete(created.id); assertFalse(api.list("NativeCreated").any { it.id == created.id })
        } finally { ApiClientProvider.createService(old); interceptor.setSession(oldToken) }
    }
}
