package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.Okul360Database
import com.example.data.local.StudentEntity
import com.example.data.model.*
import com.example.data.remote.*
import com.example.data.repository.Okul360Repository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class NativeRegistryBackendAcceptance {
    private fun env(name: String) = System.getenv(name) ?: error("Temporary fixture required: $name")
    private suspend fun rejects(code: Int, action: suspend () -> Unit) {
        try { action(); fail("HTTP $code required") } catch (failure: RegistryHttpException) { assertEquals(code, failure.code) }
    }
    @Test fun actualCrudNullablePrivacyScopesPaginationAndNoPrematureRoomWrites() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val base = ApiClientProvider.activeBaseUrl; val token = ApiClientProvider.interceptor.authToken
        val workspace = ApiClientProvider.interceptor.workspaceOrganizationId
        try {
            ApiClientProvider.createService(env("NATIVE_TEST_API_URL"))
            ApiClientProvider.interceptor.setSession(env("NATIVE_TEST_ROOT_TOKEN"), workspace = env("NATIVE_TEST_ORG"))
            context.getSharedPreferences("scs360_api_connection", Context.MODE_PRIVATE).edit().remove("base_url").commit()
            val repo = Okul360Repository(context)
            val dao = Okul360Database.getDatabase(context).studentDao()
            // A preexisting local row survives every successful and rejected remote operation.
            val sentinel = StudentEntity(id = env("NATIVE_TEST_EXISTING"), student_number = "local-synthetic", first_name = "Local", last_name = "Sentinel",
                class_id = "", class_name = "", branch_id = env("NATIVE_TEST_BRANCH"), parent_name = "", parent_phone = "", parent_email = null,
                status = "ACTIVE", allergies_or_diet = null, blood_type = null, avatar_color_hex = 0, tuition_balance = 0.0, today_attendance_status = null)
            dao.insertStudent(sentinel)
            val originalRoom = dao.getAllStudentsList()
            val roster = repo.getStudents(null, null, null)
            assertEquals(102, roster.size)
            assertTrue(roster.all { it.birth_date.isBlank() && it.gender.isBlank() && it.blood_type == null && it.address == null && it.parent_phone.isBlank() })
            assertEquals(originalRoom, dao.getAllStudentsList())
            assertEquals(1, repo.getStudents(null, null, "Native099").size)
            val branches = repo.getBranches()
            assertEquals(2, branches.size)
            assertTrue(branches.all { it.organization_id == env("NATIVE_TEST_ORG") })
            val choices = repo.getClassTeacherOptions(env("NATIVE_TEST_ORG"), env("NATIVE_TEST_BRANCH"))
            assertEquals(env("NATIVE_TEST_TEACHER_ID"), choices.single().id)
            val room = repo.addClass(ClassGroup(name = "Native Registry Class", organization_id = env("NATIVE_TEST_ORG"),
                branch_id = env("NATIVE_TEST_BRANCH"), code = "NATIVE", academic_year = "2026-2027", capacity = 200,
                teacher_id = choices.single().id, description = "Synthetic description"))
            UUID.fromString(room.id)
            assertEquals("NATIVE", room.code); assertEquals("2026-2027", room.academic_year)
            assertEquals(choices.single().id, room.teacher_id)
            val editedClass = repo.updateClass(room.copy(code = "", teacher_id = null, description = null))
            assertEquals("", editedClass.code); assertNull(editedClass.teacher_id); assertNull(editedClass.description)
            val draft = Student(organization_id = env("NATIVE_TEST_ORG"), branch_id = env("NATIVE_TEST_BRANCH"), class_id = room.id,
                first_name = "NativeRegistry", last_name = "Initial", student_number = "NATIVE-REGISTRY-ONLY", health_notes = "Private synthetic note")
            val created = repo.addStudent(draft)
            UUID.fromString(created.id)
            assertTrue(created.birth_date.isBlank()); assertNull(created.blood_type); assertEquals("Private synthetic note", created.health_notes)
            try { repo.deleteClass(room.id); fail("Occupied class must remain available") }
            catch (_: IllegalArgumentException) { }
            rejects(409) { repo.addStudent(draft.copy(first_name = "Duplicate")) }
            rejects(422) { repo.addStudent(draft.copy(student_number = "", branch_id = env("NATIVE_TEST_SAME_ORG_BRANCH"))) }
            assertEquals(originalRoom, dao.getAllStudentsList())

            // A custom role can edit basic fields without reading or overwriting health notes.
            ApiClientProvider.interceptor.setSession(env("NATIVE_TEST_CLERK_TOKEN"))
            val minimalBranches = NativeRegistryClient().studentBranches(env("NATIVE_TEST_ORG"))
            assertTrue(minimalBranches.any { it.id == env("NATIVE_TEST_BRANCH") })
            val minimalClasses = NativeRegistryClient().studentClasses(env("NATIVE_TEST_ORG"), env("NATIVE_TEST_BRANCH"))
            assertTrue(minimalClasses.any { it.id == room.id })
            val writeOnlyCreated = repo.addStudent(Student(organization_id = env("NATIVE_TEST_ORG"), branch_id = env("NATIVE_TEST_BRANCH"),
                first_name = "NativeWriterOnly", last_name = "Synthetic"))
            repo.deleteStudent(writeOnlyCreated.id)
            val visible = repo.getStudents(null, null, "NativeRegistry").single()
            assertNull(visible.health_notes)
            val edited = repo.updateStudent(visible.copy(last_name = "Edited", class_id = ""))
            assertEquals("Edited", edited.last_name); assertEquals("", edited.class_id)
            rejects(403) { repo.updateStudent(edited.copy(health_notes = "Unauthorized replacement")) }
            rejects(404) { repo.deleteStudent(env("NATIVE_TEST_FOREIGN_STUDENT")) }
            assertEquals(originalRoom, dao.getAllStudentsList())
            ApiClientProvider.interceptor.setSession(env("NATIVE_TEST_TEACHER_TOKEN"))
            rejects(403) { repo.addStudent(draft.copy(student_number = "", health_notes = null)) }

            ApiClientProvider.interceptor.setSession(env("NATIVE_TEST_ROOT_TOKEN"), workspace = env("NATIVE_TEST_ORG"))
            val verified = repo.getStudents(null, null, "NativeRegistry").single()
            assertEquals("Private synthetic note", verified.health_notes)
            ApiClientProvider.interceptor.setSession(env("NATIVE_TEST_ROOT_TOKEN"), workspace = env("NATIVE_TEST_OTHER_ORG"))
            rejects(404) { repo.updateClass(editedClass.copy(name = "Foreign edit")) }
            ApiClientProvider.interceptor.setSession(env("NATIVE_TEST_ROOT_TOKEN"), workspace = env("NATIVE_TEST_ORG"))
            repo.deleteStudent(created.id)
            repo.deleteClass(room.id)
            assertTrue(repo.getStudents(null, null, "NativeRegistry").isEmpty())
            assertEquals(originalRoom, dao.getAllStudentsList())
            assertEquals(originalRoom, dao.getAllStudentsList())
        } finally { ApiClientProvider.createService(base); ApiClientProvider.interceptor.setSession(token, workspace = workspace) }
    }
}
