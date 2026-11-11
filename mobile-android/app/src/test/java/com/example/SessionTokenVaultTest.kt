package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.EncryptedSessionTokenVault
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class SessionTokenVaultTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val prefs = context.getSharedPreferences("scs360_session_token_v1", Context.MODE_PRIVATE)
    private val testKey = SecretKeySpec(ByteArray(32) { it.toByte() }, "AES")
    @Before fun clear() { prefs.edit().clear().commit() }
    @Test fun roundTripPersistsOnlyRandomizedAuthenticatedCiphertext() {
        val vault = EncryptedSessionTokenVault(context) { testKey }
        vault.write("synthetic-session-secret")
        val first = prefs.getString("ciphertext", null)!!
        assertFalse(first.contains("synthetic-session-secret"))
        assertEquals("synthetic-session-secret", EncryptedSessionTokenVault(context) { testKey }.read())
        vault.write("synthetic-session-secret")
        assertNotEquals(first, prefs.getString("ciphertext", null))
        vault.clear()
        assertNull(vault.read())
    }
    @Test fun wrongKeyAndModifiedCiphertextNeverRestoreASession() {
        val vault = EncryptedSessionTokenVault(context) { testKey }
        vault.write("synthetic-session-secret")
        assertNull(EncryptedSessionTokenVault(context) { SecretKeySpec(ByteArray(32) { 99 }, "AES") }.read())
        val bytes = android.util.Base64.decode(prefs.getString("ciphertext", null), android.util.Base64.NO_WRAP)
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
        prefs.edit().putString("ciphertext", android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)).commit()
        assertNull(vault.read())
    }
}
