package com.example.data.remote

import com.example.data.model.LoginRequest
import com.example.data.model.SessionRolePolicy
import com.example.data.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Canonical JSON login and a fresh authenticated profile; no local identity fallback. */
class NativeAuthentication(private val api: Okul360ApiService) {
    suspend fun login(email: String, password: String): User {
        val before = ApiClientProvider.interceptor.captureScope()
        val response = try { withContext(Dispatchers.IO) { api.login(LoginRequest(email = email.trim(), password = password)) } }
            catch (failure: Exception) { before.ensureCurrent(); throw failure }
        before.ensureCurrent()
        val token = response.resolvedToken?.takeIf { it.isNotBlank() } ?: error("Giriş sunucuda doğrulanamadı")
        return restore(token)
    }

    suspend fun restore(token: String): User {
        ApiClientProvider.interceptor.setSession(token)
        val scope = ApiClientProvider.interceptor.captureScope()
        return try {
            val user = withContext(Dispatchers.IO) { api.getCurrentUser() }
            scope.ensureCurrent()
            check(user.id.isNotBlank() && SessionRolePolicy.primary(user) != null) { "Hesabınızın desteklenen etkin rolü bulunmuyor" }
            user
        } catch (failure: Exception) {
            scope.ensureCurrent()
            ApiClientProvider.interceptor.setSession(null)
            throw failure
        }
    }
}
