package com.example.data.remote

import com.example.data.model.User

internal class NativeAccountClient(private val api: Okul360ApiService) {
    suspend fun updateProfile(
        accountId: String, firstName: String, lastName: String, email: String, phone: String, currentPassword: String?
    ): User {
        val fields = buildMap {
            put("first_name", firstName.trim())
            put("last_name", lastName.trim())
            put("email", email.trim())
            put("phone", phone.trim())
            currentPassword?.let { put("current_password", it) }
        }
        val response = api.updateOwnProfile(fields)
        val canonicalEmail = response["email"]
        check(response["id"] == accountId && !canonicalEmail.isNullOrBlank()) { "Profil yanıtı doğrulanamadı." }
        return api.getCurrentUser().also { saved ->
            check(saved.id == accountId && saved.email == canonicalEmail &&
                saved.first_name == fields["first_name"] && saved.last_name == fields["last_name"] &&
                saved.phone.orEmpty() == fields["phone"]) { "Kaydedilen profil doğrulanamadı." }
        }
    }

    suspend fun changePassword(accountId: String, currentPassword: String, newPassword: String) {
        val response = api.changePassword(mapOf("current_password" to currentPassword, "new_password" to newPassword))
        check(response["id"] == accountId) { "Şifre değişikliği doğrulanamadı." }
    }
}
