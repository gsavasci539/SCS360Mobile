package com.example.data.remote

import kotlinx.coroutines.CancellationException
import java.io.IOException

/** The interceptor uses an IOException so OkHttp delivers normal onFailure. */
class NativeContextChangedException : IOException("Oturum veya kurum çalışma alanı değişti")

class NativeRequestScope internal constructor(
    private val owner: AuthAndTenantInterceptor,
    private val identity: Any,
    private val base: String
) {
    fun current() = owner.owns(identity) && base == ApiClientProvider.activeBaseUrl
    fun ensureCurrent() {
        if (!current()) throw CancellationException("Eski oturum veya kurum yanıtı kullanılmadı")
    }
}
