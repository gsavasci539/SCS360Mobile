package com.example.data.remote

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID

internal data class SchoolMerchantProvider(val id: String, val name: String, val credentialFields: List<String>)

internal data class SchoolMerchantStatus(
    val canManage: Boolean,
    val available: Boolean,
    val accountId: String? = null,
    val revision: String? = null,
    val currentProvider: String? = null,
    val environment: String? = null,
    val enabled: Boolean = false,
    val configured: Boolean = false,
    val callbackConfigured: Boolean = false,
    val portalConfigured: Boolean = false,
    val encryptionConfigured: Boolean = false,
    val productionCompatible: Boolean = false,
    val providers: List<SchoolMerchantProvider> = emptyList(),
    val savedCredentials: Set<String> = emptySet(),
    val bankPosNote: String = "",
)

internal class SchoolMerchantHttpException(val code: Int, message: String) : Exception(message)

internal data class SchoolMerchantRequest(
    val requestId: String,
    val expectedRevision: String?,
    val previousProvider: String?,
    val provider: String,
    val environment: String,
    val enabled: Boolean,
    val replaceAccount: Boolean,
    val credentials: Map<String, String>,
) {
    override fun toString() = "Kurum ödeme hesabı kayıt isteği"
}

internal fun SchoolMerchantStatus.newRequest(
    provider: SchoolMerchantProvider,
    environment: String,
    enabled: Boolean,
    replaceAccount: Boolean,
    fields: Map<String, String>,
): SchoolMerchantRequest {
    check(canManage)
    require(environment in setOf("SANDBOX", "LIVE")) { "Ödeme ortamını seçin." }
    require(accountId == null || replaceAccount || (provider.id == currentProvider && environment == this.environment)) {
        "Sağlayıcı veya ortam değişikliği için yeni hesap kullanımını onaylayın."
    }
    val credentials = fields.filterKeys(provider.credentialFields::contains).mapValues { it.value.trim() }.filterValues(String::isNotEmpty)
    require(credentials.values.all { it.length <= 2048 }) { "Anahtar uzunluğu en fazla 2048 karakter olmalıdır." }
    require(configured && !replaceAccount || provider.credentialFields.all(credentials::containsKey)) {
        "Seçili sağlayıcının bütün üye işyeri anahtarlarını girin."
    }
    return SchoolMerchantRequest(UUID.randomUUID().toString(), revision, currentProvider, provider.id, environment, enabled, replaceAccount, credentials)
}

internal class NativeSchoolMerchantClient(organizationId: String) {
    private val scope = ApiClientProvider.interceptor.captureScope()
    private val url = (ApiClientProvider.activeBaseUrl + "payments/merchant").toHttpUrl().newBuilder()
        .addQueryParameter("organization_id", organizationId).build()
    private val http = ApiClientProvider.createOkHttpClient(unwrapApiData = false)

    private suspend fun call(payload: JSONObject? = null): SchoolMerchantStatus = scope.io {
        val request = Request.Builder().url(url).header("Cache-Control", "no-store")
            .tag(NativeRequestScope::class.java, scope).apply {
                if (payload != null) put(payload.toString().toRequestBody("application/json".toMediaType()))
            }.build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val error = when (response.code) {
                    401, 403 -> "Ödeme hesabı erişimi reddedildi."
                    409 -> "Ödeme ayarı değişmiş. Güncel durumu yeniden yükleyin."
                    422 -> "Ödeme hesabı alanları doğrulanamadı. Sağlayıcı, ortam ve anahtarları kontrol edin."
                    503 -> "Sunucu yapılandırması ödeme hesabını kaydetmeye uygun değil."
                    else -> "Ödeme hesabı yanıtı alınamadı (HTTP ${response.code})."
                }
                throw SchoolMerchantHttpException(response.code, error)
            }
            val envelope = JSONObject(requireNotNull(response.body).string())
            check(envelope.getBoolean("success")) { "Ödeme ayarı doğrulanamadı." }
            decode(envelope.getJSONObject("data"))
        }
    }

    private fun decode(row: JSONObject): SchoolMerchantStatus {
        fun nullableString(key: String) = row.get(key).takeUnless { it == JSONObject.NULL } as String?
        val available = row.get("available") as Boolean
        if (!(row.get("can_manage") as Boolean)) return SchoolMerchantStatus(canManage = false, available = available)
        val catalog = row.getJSONArray("providers")
        val choices = (0 until catalog.length()).map { index ->
            val item = catalog.getJSONObject(index)
            val keys = item.getJSONArray("credential_fields")
            SchoolMerchantProvider(item.getString("id"), item.getString("name"),
                (0 until keys.length()).map { keys.get(it) as String })
        }
        val provider = row.getString("provider")
        val selected = choices.single { it.id == provider }
        val flags = row.getJSONObject("credential_status")
        val accountId = nullableString("account_id")?.also { UUID.fromString(it) }
        val revision = nullableString("revision")
        check((accountId == null && revision == null) || (accountId != null && revision?.matches(Regex("[0-9a-f]{64}")) == true))
        val environment = nullableString("environment")
        check(environment == null || environment in setOf("SANDBOX", "LIVE"))
        return SchoolMerchantStatus(
            canManage = true, available = available, accountId = accountId, revision = revision,
            currentProvider = provider, environment = environment, enabled = row.get("enabled") as Boolean,
            configured = row.get("configured") as Boolean, callbackConfigured = row.get("callback_configured") as Boolean,
            portalConfigured = row.get("portal_configured") as Boolean, encryptionConfigured = row.get("encryption_configured") as Boolean,
            productionCompatible = row.get("production_compatible") as Boolean, providers = choices,
            savedCredentials = selected.credentialFields.filter { flags.opt(it) == true }.toSet(), bankPosNote = row.getString("bank_pos_note"),
        )
    }

    suspend fun status(): SchoolMerchantStatus = call()

    suspend fun save(request: SchoolMerchantRequest): SchoolMerchantStatus {
        val body = JSONObject().put("request_id", request.requestId)
            .put("expected_revision", request.expectedRevision ?: JSONObject.NULL).put("environment", request.environment)
            .put("enabled", request.enabled).put("replace_account", request.replaceAccount)
        if (request.provider == "iyzico" && request.previousProvider in listOf(null, "iyzico")) {
            request.credentials.forEach { (field, value) -> body.put(field, value) }
        } else {
            body.put("provider", request.provider).put("credentials", JSONObject(request.credentials))
        }
        val acknowledged = call(body)
        check(acknowledged.canManage && acknowledged.currentProvider == request.provider && acknowledged.environment == request.environment &&
            acknowledged.enabled == request.enabled && acknowledged.accountId != null && acknowledged.revision != null) {
            "Ödeme ayarı yanıtı doğrulanamadı."
        }
        return status().also { exact ->
            check(exact.canManage && exact.accountId == acknowledged.accountId && exact.revision == acknowledged.revision &&
                exact.currentProvider == request.provider && exact.environment == request.environment && exact.enabled == request.enabled) {
                "Kaydedilen ödeme hesabı doğrulanamadı."
            }
        }
    }
}
