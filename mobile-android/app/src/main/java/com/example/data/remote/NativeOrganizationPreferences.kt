package com.example.data.remote

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.Locale

internal val noticePreferenceLabels = mapOf(
    "receipt_email_enabled" to "Ödeme sonrası makbuz e-postası",
    "overdue_reminders_enabled" to "Geciken borç hatırlatmaları",
    "failed_notification_alerts_enabled" to "Başarısız gönderim uyarıları",
)

internal data class OrganizationPreferences(
    val values: Map<String, Any?>,
    val maxSizeMb: Int,
    val extensions: List<String>,
)

internal class NativeOrganizationPreferences(private val organizationId: String) {
    private val requestScope = ApiClientProvider.interceptor.captureScope()
    private val endpoint = ApiClientProvider.activeBaseUrl + "settings/preferences"
    private val http = ApiClientProvider.createOkHttpClient(unwrapApiData = false)

    private suspend fun call(payload: JSONObject? = null): OrganizationPreferences = requestScope.io {
        val request = Request.Builder().url(endpoint).tag(NativeRequestScope::class.java, requestScope).apply {
            if (payload != null) put(payload.toString().toRequestBody("application/json".toMediaType()))
        }.build()
        http.newCall(request).execute().use { response ->
            val text = requireNotNull(response.body).string()
            if (!response.isSuccessful) {
                val message = runCatching { JSONObject(text).getString("message") }.getOrNull()
                throw IllegalStateException(message ?: "Kurum tercihleri alınamadı (HTTP ${response.code}).")
            }
            val result = JSONObject(text)
            check(result.getBoolean("success")) { "Kurum tercihleri doğrulanamadı." }
            parse(result.getJSONObject("data"))
        }
    }

    private fun parse(data: JSONObject): OrganizationPreferences {
        check(data.getString("organization_id") == organizationId) { "Seçili kurum değişti." }
        val current = data.getJSONObject("values")
        val policy = data.getJSONObject("upload_policy")
        val supported = policy.getJSONArray("allowed_extensions").let { values ->
            (0 until values.length()).map { values.get(it) as String }
        }
        val max = policy.get("max_size_mb") as Int
        val actual = current.get("max_upload_size_mb") as Int
        check(actual in 1..max) { "Belge yükleme sınırı doğrulanamadı." }
        val values = buildMap {
            put("school_name", current.get("school_name") as String)
            put("academic_year", current.get("academic_year").takeUnless { it == JSONObject.NULL } as String?)
            put("default_currency", (current.get("default_currency") as String).also { check(it in listOf("TRY", "EUR", "USD")) })
            put("max_upload_size_mb", actual)
            put("allowed_file_types", current.get("allowed_file_types") as String)
            noticePreferenceLabels.keys.forEach { key ->
                put(key, current.get(key).takeUnless { it == JSONObject.NULL } as Boolean?)
            }
        }
        return OrganizationPreferences(values, max, supported)
    }

    suspend fun load(): OrganizationPreferences = call()

    suspend fun save(previous: OrganizationPreferences, edits: Map<String, Any?>): OrganizationPreferences {
        val changes = edits.filter { (key, value) -> previous.values[key] != value }
        if (changes.isEmpty()) return previous
        val expected = changes.keys.associateWith { previous.values[it] }
        fun json(values: Map<String, Any?>) = JSONObject().apply {
            values.forEach { (key, value) -> put(key, value ?: JSONObject.NULL) }
        }
        call(JSONObject().put("organization_id", organizationId)
            .put("changes", json(changes)).put("expected", json(expected)))
        return load().also { persisted ->
            check(changes.all { (key, value) -> persisted.values[key] == value }) { "Kaydedilen tercihler doğrulanamadı." }
        }
    }
}

internal fun OrganizationPreferences.normalizeExtensions(text: String): String {
    val entered = text.split(',').map { it.trim().lowercase(Locale.ROOT) }.filter(String::isNotBlank).toSet()
    require(entered.all(extensions::contains)) { "Yalnız sunucunun izin verdiği uzantıları girin." }
    return extensions.filter(entered::contains).joinToString(",")
}
