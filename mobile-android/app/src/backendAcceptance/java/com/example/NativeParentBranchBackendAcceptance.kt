package com.example

import com.example.data.model.*
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
class NativeParentBranchBackendAcceptance {
    private fun env(name: String) = System.getenv(name) ?: error("Missing isolated input: $name")
    @Test fun branchLimitedCustomManagerCreatesAndReloadsAnAtomicStudentContact() = runBlocking {
        val base = ApiClientProvider.activeBaseUrl; val interceptor = ApiClientProvider.interceptor; val token = interceptor.authToken
        try {
            val auth = NativeAuthentication(ApiClientProvider.createService(env("NATIVE_TEST_API_URL")))
            val actor = auth.login("native-branch@example.com", "Synthetic-pass-2026!")
            assertEquals(UserRole.CUSTOM, SessionRolePolicy.primary(actor)); assertFalse(actor.is_super_admin == true)
            assertTrue(NativeParentCreationPolicy.requiresStudent(actor, "all")); assertTrue(actor.branch_ids.orEmpty().contains(env("NATIVE_TEST_BRANCH")))
            val api = NativeParentClient(env("NATIVE_TEST_ORGANIZATION"), true)
            val options = NativeRegistryClient().students(null, null, null)
            assertTrue(options.any { it.id == env("NATIVE_TEST_STUDENT") }); assertFalse(options.any { it.id == env("NATIVE_TEST_FOREIGN_STUDENT") })
            assertTrue(api.list("BranchSeed").isEmpty())
            val body = JSONObject().put("first_name", "BranchCreated").put("last_name", "Contact").put("relation_type", "GUARDIAN")
            try { api.save(body); fail("Student required") } catch (_: IllegalArgumentException) { }
            val created = api.save(body, createStudentId = env("NATIVE_TEST_STUDENT"))
            assertEquals(created.id, api.list("BranchCreated").single().id)
            assertTrue(created.id in api.studentParents(env("NATIVE_TEST_STUDENT")))
            val updated = api.save(JSONObject().put("phone", "05320000002"), created)
            assertEquals("05320000002", updated.value("phone"))
            try { api.save(JSONObject().put("first_name", "ForeignRejected").put("last_name", "Contact"), createStudentId = env("NATIVE_TEST_FOREIGN_STUDENT")); fail("Foreign student") }
            catch (failure: RegistryHttpException) { assertTrue(failure.code in listOf(403, 404)) }
            val teacher = auth.login("native-class-teacher@example.com", "Synthetic-pass-2026!")
            assertEquals(UserRole.TEACHER, SessionRolePolicy.primary(teacher))
            assertTrue(teacher.branch_ids.isNullOrEmpty()); assertTrue(NativeParentCreationPolicy.requiresStudent(teacher, "all"))
            val teacherApi = NativeParentClient(env("NATIVE_TEST_ORGANIZATION"), true)
            assertTrue(teacherApi.list("BranchSeed").isEmpty())
            val teacherOptions = NativeRegistryClient().students(null, null, null)
            assertTrue(teacherOptions.any { it.id == env("NATIVE_TEST_STUDENT") }); assertFalse(teacherOptions.any { it.id == env("NATIVE_TEST_OTHER_CLASS_STUDENT") })
            val teacherCreated = teacherApi.save(JSONObject().put("first_name", "TeacherCreated").put("last_name", "Contact"), createStudentId = env("NATIVE_TEST_STUDENT"))
            assertEquals(teacherCreated.id, teacherApi.list("TeacherCreated").single().id)
            try { teacherApi.save(JSONObject().put("first_name", "OtherClassRejected").put("last_name", "Contact"), createStudentId = env("NATIVE_TEST_OTHER_CLASS_STUDENT")); fail("Other class student") }
            catch (failure: RegistryHttpException) { assertTrue(failure.code in listOf(403, 404)) }
        } finally { ApiClientProvider.createService(base); interceptor.setSession(token) }
    }
}
