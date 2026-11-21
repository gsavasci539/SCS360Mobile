package com.example.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

internal enum class InstitutionIntegration(val label: String, val fields: Map<String, String> = emptyMap()) {
    SMTP("SMTP", mapOf("host" to "SMTP sunucusu", "port" to "SMTP portu", "username" to "SMTP kullanıcı adı")),
    NETGSM("SMS · NETGSM"),
    TWILIO("SMS · TWILIO"),
    META_WHATSAPP("WhatsApp", mapOf("phone_id" to "WhatsApp telefon ID")),
}

internal data class IntegrationSettingsRecord(
    val id: String,
    val active: Boolean,
    val reviewRequired: Boolean,
    val publicFields: Map<String, Any>,
    val blockedFields: Set<String>,
)

internal class NativeIntegrationSettings(private val organizationId: String) {
    private val scope = ApiClientProvider.interceptor.captureScope()
    private val url = (ApiClientProvider.activeBaseUrl + "settings/integrations").toHttpUrl()
    private val http = ApiClientProvider.createOkHttpClient(unwrapApiData = false)

    private suspend fun call(payload: JSONObject? = null): Any = scope.io {
        val target = if (payload == null) url.newBuilder().addQueryParameter("organization_id", organizationId).build() else url
        val request = Request.Builder().url(target).tag(NativeRequestScope::class.java, scope).apply {
            if (payload != null) put(payload.toString().toRequestBody("application/json".toMediaType()))
        }.build()
        http.newCall(request).execute().use { response ->
            val message = requireNotNull(response.body).string()
            if (!response.isSuccessful) {
                val detail = runCatching { JSONObject(message).getString("message") }.getOrNull()
                throw IllegalStateException(detail ?: "Bağlantı kaydı alınamadı (HTTP ${response.code}).")
            }
            val envelope = JSONObject(message)
            check(envelope.getBoolean("success")) { "Bağlantı ayarları doğrulanamadı." }
            envelope.get("data")
        }
    }

    suspend fun load(): Map<InstitutionIntegration, IntegrationSettingsRecord> {
        val rows = call() as JSONArray
        val seen = mutableSetOf<String>()
        return buildMap {
            repeat(rows.length()) { index ->
                val row = rows.getJSONObject(index)
                val provider = row.getString("provider")
                check(row.getString("organization_id") == organizationId && seen.add(provider)) { "Kurum bağlantıları doğrulanamadı." }
                val kind = InstitutionIntegration.entries.firstOrNull { it.name == provider } ?: return@repeat
                val review = row.optBoolean("requires_review")
                val config = if (review) null else row.getJSONObject("config")
                val allowed = kind.fields.keys.mapNotNull { key ->
                    val value = config?.opt(key)
                    if (value is String || value is Number) key to value else null
                }.toMap()
                val blocked = kind.fields.keys.filter { config?.has(it) == true && it !in allowed }.toSet()
                val id = row.getString("id")
                check(id.isNotBlank())
                val active = row.get("is_active") as Boolean
                put(kind, IntegrationSettingsRecord(id, active, review, allowed, blocked))
            }
        }
    }

    suspend fun save(
        provider: InstitutionIntegration, previous: IntegrationSettingsRecord?, patch: Map<String, String>,
    ): Map<InstitutionIntegration, IntegrationSettingsRecord> {
        require(patch.isNotEmpty() && patch.keys.all { it in provider.fields && it !in previous?.blockedFields.orEmpty() })
        require(previous?.reviewRequired != true)
        val changes = JSONObject()
        val expected = JSONObject()
        patch.forEach { (key, value) ->
            changes.put(key, value)
            expected.put(key, previous?.publicFields?.get(key) ?: JSONObject.NULL)
        }
        val acknowledgement = call(JSONObject().put("organization_id", organizationId).put("provider", provider.name)
            .put("config", changes).put("expected_config", expected)) as JSONObject
        val id = acknowledgement.getString("id")
        check(id.isNotBlank() && acknowledgement.getString("organization_id") == organizationId &&
            acknowledgement.getString("provider") == provider.name) { "Bağlantı kaydı doğrulanamadı." }
        return load().also { records ->
            val persisted = records[provider]
            check(persisted != null && persisted.id == id && !persisted.reviewRequired &&
                patch.all { (field, value) -> persisted.publicFields[field] == value }) { "Kaydedilen bağlantı doğrulanamadı." }
        }
    }
}
