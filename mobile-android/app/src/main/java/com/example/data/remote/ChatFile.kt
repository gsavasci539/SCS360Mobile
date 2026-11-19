package com.example.data.remote

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.Locale

private const val MAX_CHAT_FILE_BYTES = 10 * 1024 * 1024
private val attachmentTypes = mapOf(
    "pdf" to "application/pdf", "png" to "image/png", "jpg" to "image/jpeg", "jpeg" to "image/jpeg",
)

internal fun chatFileType(name: String): String? =
    attachmentTypes[name.substringAfterLast('.', "").lowercase(Locale.ROOT)]

internal fun chatFileName(name: String): String =
    name.replace('\\', '/').substringAfterLast('/').trim().also {
        require(it.isNotEmpty() && it.length <= 255 && it.none { char -> char.code < 32 } && chatFileType(it) != null)
    }

internal fun InputStream.readChatFile(): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = read(buffer)
        if (count == -1) break
        require(output.size() + count <= MAX_CHAT_FILE_BYTES) { "Dosya 10 MB sınırını aşıyor" }
        output.write(buffer, 0, count)
    }
    return output.toByteArray().also { require(it.isNotEmpty()) }
}

internal fun chatFileDigest(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(Locale.ROOT, it) }

class ChatFile(name: String, val bytes: ByteArray) {
    val name = chatFileName(name)
    val type = requireNotNull(chatFileType(this.name))
    val digest = chatFileDigest(bytes)

    init {
        require(bytes.size in 1..MAX_CHAT_FILE_BYTES)
    }
}
