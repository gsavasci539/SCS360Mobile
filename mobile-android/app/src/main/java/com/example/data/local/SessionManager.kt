package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.model.UserRoleInfo
import com.example.data.model.SessionRolePolicy
import org.json.JSONArray
import org.json.JSONObject

class SessionManager(context: Context, private val vault: SessionTokenVault = EncryptedSessionTokenVault(context)) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "scs360_user_session"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_EMAIL = "email"
        private const val KEY_ROLE = "role"
        private const val KEY_PHONE = "phone"
        private const val KEY_TENANT_ID = "tenant_id"
        private const val KEY_INSTITUTION_NAME = "institution_name"
        private const val KEY_BRANCH_ID = "branch_id"
        private const val KEY_BRANCH_NAME = "branch_name"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_PROFILE_VERIFIED = "profile_verified_v2"
        private const val KEY_SUPER_ADMIN = "is_super_admin"
        private const val KEY_ROLES = "roles_v2"
        private const val KEY_PERMISSIONS = "permissions_v2"
        private const val KEY_API_BASE = "profile_api_base_v2"
        private const val KEY_FCM_TOKEN = "fcm_device_token"
    }

    fun saveFcmToken(token: String) {
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    fun getFcmToken(): String? {
        return prefs.getString(KEY_FCM_TOKEN, null)
    }

    fun saveSession(user: User, token: String? = null) {
        if (token != null) vault.write(token)
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, user.id)
            putString(KEY_FULL_NAME, user.resolvedName)
            putString(KEY_EMAIL, user.email)
            putString(KEY_ROLE, user.resolvedRole)
            putBoolean(KEY_PROFILE_VERIFIED, true)
            putString(KEY_API_BASE, com.example.data.remote.ApiClientProvider.activeBaseUrl)
            putBoolean(KEY_SUPER_ADMIN, user.is_super_admin == true)
            putString(KEY_ROLES, JSONArray().apply { user.roles.forEach { put(JSONObject().put("slug", it.slug).put("is_active", it.is_active)) } }.toString())
            putString(KEY_PERMISSIONS, JSONArray(user.permissions).toString())
            putString(KEY_PHONE, user.phone)
            putString(KEY_TENANT_ID, user.tenant_id)
            putString(KEY_INSTITUTION_NAME, user.institution_name)
            putString(KEY_BRANCH_ID, user.branch_id)
            putString(KEY_BRANCH_NAME, user.branch_name)
            remove(KEY_TOKEN)
            apply()
        }
    }

    fun updateRole(newRole: String) {
        check(getUserRole()?.name == newRole.uppercase(java.util.Locale.ROOT)) { "Rol sunucu profilinden belirlenir" }
    }

    fun updateBranch(branchId: String, branchName: String) {
        prefs.edit().apply {
            putString(KEY_BRANCH_ID, branchId)
            putString(KEY_BRANCH_NAME, branchName)
            apply()
        }
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) && getUserRole() != null && !getToken().isNullOrBlank()
    }

    fun getUser(): User? {
        if (!prefs.getBoolean(KEY_IS_LOGGED_IN, false) || !prefs.getBoolean(KEY_PROFILE_VERIFIED, false)) return null
        if (prefs.getString(KEY_API_BASE, null) != com.example.data.remote.ApiClientProvider.activeBaseUrl) return null
        val id = prefs.getString(KEY_USER_ID, null)?.takeIf { it.isNotBlank() } ?: return null
        return try { User(
            id = id,
            full_name = prefs.getString(KEY_FULL_NAME, "Kullanıcı") ?: "Kullanıcı",
            email = prefs.getString(KEY_EMAIL, "") ?: "",
            role = prefs.getString(KEY_ROLE, "") ?: "",
            is_super_admin = prefs.getBoolean(KEY_SUPER_ADMIN, false),
            roles = JSONArray(prefs.getString(KEY_ROLES, "[]")).let { rows -> (0 until rows.length()).map { i -> rows.getJSONObject(i).let { UserRoleInfo(it.getString("slug"), it.getBoolean("is_active")) } } },
            permissions = JSONArray(prefs.getString(KEY_PERMISSIONS, "[]")).let { rows -> (0 until rows.length()).map { rows.getString(it) } },
            phone = prefs.getString(KEY_PHONE, null),
            tenant_id = prefs.getString(KEY_TENANT_ID, null),
            institution_name = prefs.getString(KEY_INSTITUTION_NAME, "SCS360 Eğitim Kurumları") ?: "SCS360 Eğitim Kurumları",
            branch_id = prefs.getString(KEY_BRANCH_ID, "all") ?: "all",
            branch_name = prefs.getString(KEY_BRANCH_NAME, "Tüm Şubeler") ?: "Tüm Şubeler"
        ) } catch (_: Exception) { null }
    }

    fun getUserRole(): UserRole? = getUser()?.let(SessionRolePolicy::primary)
    fun getToken(): String? = if (getUser() != null) vault.read() else null

    fun clearSession() {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_USER_ID)
            remove(KEY_FULL_NAME)
            remove(KEY_EMAIL)
            remove(KEY_ROLE)
            remove(KEY_PROFILE_VERIFIED)
            remove(KEY_SUPER_ADMIN)
            remove(KEY_ROLES)
            remove(KEY_PERMISSIONS)
            remove(KEY_API_BASE)
            remove(KEY_PHONE)
            remove(KEY_TENANT_ID)
            remove(KEY_INSTITUTION_NAME)
            remove(KEY_BRANCH_ID)
            remove(KEY_BRANCH_NAME)
            remove(KEY_TOKEN)
            apply()
        }
        vault.clear()
    }
}
