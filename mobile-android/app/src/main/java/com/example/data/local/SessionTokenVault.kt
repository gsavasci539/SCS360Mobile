package com.example.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface SessionTokenVault {
    fun read(): String?
    fun write(token: String)
    fun clear()
}

/** Key material stays in AndroidKeyStore; only authenticated ciphertext is persisted. */
class EncryptedSessionTokenVault(
    context: Context,
    private val keyProvider: () -> SecretKey = { androidKey() }
) : SessionTokenVault {
    private val prefs = context.getSharedPreferences("scs360_session_token_v1", Context.MODE_PRIVATE)
    override fun read(): String? = try {
        prefs.getString("ciphertext", null)?.let { encoded ->
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            require(bytes.size >= 29 && bytes[0] == 1.toByte())
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, keyProvider(), GCMParameterSpec(128, bytes.copyOfRange(1, 13)))
            String(cipher.doFinal(bytes.copyOfRange(13, bytes.size)), Charsets.UTF_8).takeIf { it.isNotBlank() }
        }
    } catch (_: Exception) { null }

    override fun write(token: String) {
        require(token.isNotBlank())
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keyProvider())
        check(cipher.iv.size == 12)
        val payload = byteArrayOf(1) + cipher.iv + cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        check(prefs.edit().putString("ciphertext", Base64.encodeToString(payload, Base64.NO_WRAP)).commit()) { "Güvenli oturum saklanamadı" }
    }

    override fun clear() { prefs.edit().remove("ciphertext").commit() }

    companion object {
        private const val ALIAS = "scs360.session.v1"
        @Synchronized private fun androidKey(): SecretKey {
            val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
            return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256).setRandomizedEncryptionRequired(true).build())
            }.generateKey()
        }
    }
}
