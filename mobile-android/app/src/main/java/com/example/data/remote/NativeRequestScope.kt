package com.example.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/** The interceptor uses an IOException so OkHttp delivers normal onFailure. */
class NativeContextChangedException : IOException("Oturum veya kurum çalışma alanı değişti")

class NativeRequestScope internal constructor(
    private val owner: AuthAndTenantInterceptor,
    private val identity: Any,
    private val base: String
) {
    fun current() = owner.owns(identity) && base == ApiClientProvider.activeBaseUrl
    internal fun matches(snapshot: Any, url: String) = identity === snapshot && base == url && current()
    fun ensureNetworkCurrent() {
        if (!current()) throw NativeContextChangedException()
    }
    fun ensureCurrent() {
        if (!current()) throw CancellationException("Eski oturum veya kurum yanıtı kullanılmadı")
    }
    suspend fun <T> io(block: suspend CoroutineScope.() -> T): T {
        try {
            val result = withContext(Dispatchers.IO + Context(this) + binding.asContextElement(this)) {
                ensureCurrent()
                block()
            }
            ensureCurrent()
            return result
        } catch (failure: Exception) {
            ensureCurrent()
            throw failure
        }
    }
    private class Context(val scope: NativeRequestScope) : AbstractCoroutineContextElement(Key) {
        companion object Key : CoroutineContext.Key<Context>
    }
    companion object {
        private val binding = ThreadLocal<NativeRequestScope?>()
        internal fun bound() = binding.get()
        suspend fun operation() = currentCoroutineContext()[Context]?.scope ?: ApiClientProvider.interceptor.captureScope()
    }
}
