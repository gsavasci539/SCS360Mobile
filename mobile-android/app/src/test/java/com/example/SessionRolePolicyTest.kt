package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SessionManager
import com.example.data.local.EncryptedSessionTokenVault
import javax.crypto.spec.SecretKeySpec
import com.example.data.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class SessionRolePolicyTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val prefs = context.getSharedPreferences("scs360_user_session", Context.MODE_PRIVATE)
    private val testKey = SecretKeySpec(ByteArray(32) { it.toByte() }, "AES")
    private fun session() = SessionManager(context, EncryptedSessionTokenVault(context) { testKey })
    private fun save(user: User) = session().saveSession(user, "synthetic-session-token")
    private val teacher = User(id = "synthetic-teacher", email = "teacher@example.test",
        roles = listOf(UserRoleInfo("teacher"), UserRoleInfo("parent")), permissions = listOf("attendance.read", "teacher.view_classes"))
    @Before fun clear() { prefs.edit().clear().commit(); EncryptedSessionTokenVault(context) { testKey }.clear() }

    @Test fun legacyUnverifiedCacheNeverBecomesAnAdministratorAndIsNotDeleted() {
        prefs.edit().putBoolean("is_logged_in", true).putString("user_id", "legacy-id").putString("role", "ADMIN").commit()
        assertFalse(session().isLoggedIn())
        assertNull(session().getUser())
        assertNull(session().getUserRole())
        assertEquals("legacy-id", prefs.getString("user_id", null))
    }
    @Test fun roleMetadataAndPermissionsSurviveRestartWithoutChangingTheActor() {
        save(teacher)
        val restored = session()
        assertTrue(restored.isLoggedIn())
        assertEquals(UserRole.TEACHER, restored.getUserRole())
        assertEquals(teacher.id, restored.getUser()?.id)
        assertEquals(teacher.email, restored.getUser()?.email)
        assertEquals(teacher.roles, restored.getUser()?.roles)
        assertEquals(teacher.permissions, restored.getUser()?.permissions)
        assertTrue(SessionRolePolicy.canSelect(restored.getUser()!!, UserRole.PARENT))
        assertFalse(SessionRolePolicy.canSelect(restored.getUser()!!, UserRole.SUPERADMIN))
        try { restored.updateRole("SUPERADMIN"); fail("Local role promotion must be rejected") } catch (_: IllegalStateException) { }
        assertEquals(UserRole.TEACHER, session().getUserRole())
        assertEquals(teacher.id, session().getUser()?.id)
    }
    @Test fun unknownAndInactiveRolesCannotFallBackToLegacyAdmin() {
        for (user in listOf(User(id = "synthetic"), User(id = "synthetic", role = "ADMIN", roles = listOf(UserRoleInfo("admin", false))),
            User(id = "synthetic", role = "ADMIN", roles = listOf(UserRoleInfo("unknown"))))) {
            save(user)
            assertNull(session().getUserRole())
            assertFalse(session().isLoggedIn())
        }
    }
    @Test fun rootRequiresTheRealSuperadminFlagAndLogoutClearsProfileMetadata() {
        save(User(id = "synthetic-root", role = "SUPERADMIN"))
        assertFalse(session().isLoggedIn())
        save(User(id = "synthetic-root", is_super_admin = true))
        assertTrue(session().isLoggedIn())
        assertEquals(UserRole.SUPERADMIN, session().getUserRole())
        session().clearSession()
        assertFalse(session().isLoggedIn())
        assertNull(session().getUser())
        assertFalse(prefs.contains("roles_v2"))
    }
}
