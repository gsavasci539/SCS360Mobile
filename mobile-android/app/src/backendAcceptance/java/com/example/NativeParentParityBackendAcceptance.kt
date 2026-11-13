package com.example

import com.example.data.model.SessionRolePolicy
import com.example.data.model.UserRole
import com.example.data.remote.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
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
            val manager = auth.login("native-manager@test.invalid", "Synthetic-pass-2026!")
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
            val parent = auth.login("native-parent@test.invalid", "Synthetic-pass-2026!")
            assertEquals(UserRole.PARENT, SessionRolePolicy.primary(parent))
            val portal = NativeParentPortalClient(); val child = portal.children().single()
            assertEquals(env("NATIVE_TEST_STUDENT"), child.id)
            val debt = portal.invoices(child).single(); assertEquals(env("NATIVE_TEST_DEBT"), debt.id); assertEquals(100.10, debt.amount, 0.0)
            try { NativeParentClient(env("NATIVE_TEST_ORGANIZATION")).list(); fail("Parent staff directory") } catch (failure: RegistryHttpException) { assertEquals(403, failure.code) }
            auth.restore(managerToken); api = NativeParentClient(env("NATIVE_TEST_ORGANIZATION"))
            api.unlinkStudent(child.id, created.id); assertFalse(created.id in api.studentParents(child.id))
            api.linkAccount(created.id, null); assertEquals("", api.get(created.id).value("user_id"))
            api.delete(created.id); assertFalse(api.list("NativeCreated").any { it.id == created.id })
        } finally { ApiClientProvider.createService(old); interceptor.setSession(oldToken) }
    }
}
