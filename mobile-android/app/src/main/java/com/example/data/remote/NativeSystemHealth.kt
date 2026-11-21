package com.example.data.remote

import com.example.data.model.SystemHealth
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.util.concurrent.TimeUnit

internal class NativeSystemHealth(private val api: Okul360ApiService) {
    suspend fun load(): SystemHealth {
        val start = System.nanoTime()
        val response = try {
            api.getHealth()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return SystemHealth(apiVerified = false)
        }
        if (response.status != "healthy") return SystemHealth(apiVerified = false)
        val latency = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)
        val ready = try {
            val state = api.getReady()
            true.takeIf { state.status == "ready" && state.database == "connected" }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            false.takeIf { (failure as? HttpException)?.code() == 503 }
        }
        return SystemHealth(apiVerified = true, databaseReady = ready, latencyMs = latency)
    }
}
