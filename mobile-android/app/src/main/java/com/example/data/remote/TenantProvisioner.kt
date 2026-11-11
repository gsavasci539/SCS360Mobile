package com.example.data.remote

import android.content.Context
import android.util.Patterns
import com.example.data.local.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.MessageDigest
import java.time.OffsetDateTime
import java.util.Locale
import java.util.UUID

data class TenantProvisionDraft(
    val name: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
    val planId: String
) {
    // Never include the owner's password in generated diagnostic strings.
    override fun toString() = "TenantProvisionDraft(redacted)"
    fun body(requestId: String): JSONObject {
        require(name.trim().length in 2..255 && firstName.trim().length in 1..100 && lastName.trim().length in 1..100)
        require(Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() && password.length in 8..4096)
        return JSONObject().put("name", name.trim()).put("owner_first_name", firstName.trim())
            .put("owner_last_name", lastName.trim()).put("owner_email", email.trim().lowercase(Locale.ROOT))
            .put("owner_password", password).put("plan_id", UUID.fromString(planId).toString())
            .put("request_id", UUID.fromString(requestId).toString())
    }
}

/** Persist only an opaque request id; never store the draft, password or token. */
class TenantProvisioner(context: Context) {
    private val api = SaasApi()
    private val base = ApiClientProvider.activeBaseUrl
    private val userId = UUID.fromString(SessionManager(context).getUser()?.id ?: error("Oturum gerekli")).toString()
    private val workspace = ApiClientProvider.interceptor.workspaceOrganizationId
    private val prefs = context.getSharedPreferences("tenant_provision_v1", Context.MODE_PRIVATE)
    init { check(workspace == null) { "Platform çalışma alanını açın" } }
    fun current() = api.current()

    suspend fun create(draft: TenantProvisionDraft): JSONObject = withContext(Dispatchers.IO) {
        check(current()) { "Oturum değişti" }
        // Validate before persisting an id or sending any request. The backend
        // chooses the real plan period; this client never invents price/dates.
        draft.body(UUID(0, 0).toString())
        val scope = MessageDigest.getInstance("SHA-256")
            .digest("$base|$userId|$workspace|${draft.name.trim().lowercase(Locale.ROOT)}".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 255) }
        val requestId = synchronized(nonceLock) {
            prefs.getString(scope, null)?.also { UUID.fromString(it) } ?: UUID.randomUUID().toString().also {
                check(prefs.edit().putString(scope, it).commit()) { "İstek kimliği saklanamadı" }
            }
        }
        // Keep the id after acknowledgement too: late/reopened retries must
        // replay the original transaction, never create a second institution.
        val result = api.request("platform/tenants", "POST", draft.body(requestId)) as JSONObject
        UUID.fromString(result.getString("organization_id"))
        UUID.fromString(result.getString("owner_user_id"))
        check(result.getString("owner_email") == draft.email.trim().lowercase(Locale.ROOT)) { "Sahip sonucu eşleşmiyor" }
        OffsetDateTime.parse(result.getString("period_end"))
        result
    }
    companion object { private val nonceLock = Any() }
}
