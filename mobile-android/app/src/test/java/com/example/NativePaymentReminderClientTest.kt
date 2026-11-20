package com.example

import com.example.data.remote.ApiClientProvider
import com.example.data.remote.NativePaymentReminderClient
import com.example.data.remote.PaymentReminderResult
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class NativePaymentReminderClientTest {
    @Test fun smsRequestReportsOnlyVerifiedQueueOrSkip() = runBlocking {
        fun id(suffix: Long) = UUID(90, suffix).toString()
        val invoice = id(1)
        val student = id(2)
        val calls = CopyOnWriteArrayList<JSONObject>()
        val paths = CopyOnWriteArrayList<String>()
        val count = AtomicInteger()
        val oldBase = ApiClientProvider.activeBaseUrl
        val interceptor = ApiClientProvider.interceptor
        val oldToken = interceptor.authToken
        val oldTenant = interceptor.tenantId
        val oldBranch = interceptor.branchId
        val oldWorkspace = interceptor.workspaceOrganizationId
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            paths.add("${exchange.requestMethod} ${exchange.requestURI.path}")
            calls.add(JSONObject(exchange.requestBody.use { it.readBytes().decodeToString() }))
            val attempt = count.getAndIncrement()
            val queued = JSONArray()
            val skipped = JSONArray()
            if (attempt == 1) {
                skipped.put(JSONObject().put("invoice_id", invoice).put("reason", "İletişim adresi bulunamadı"))
            } else {
                queued.put(JSONObject().put("invoice_id", invoice).put("student_id", if (attempt == 0) student else id(3))
                    .put("job_id", id(4)).put("channel", "SMS").put("status", "QUEUED").put("recipients", 2))
            }
            val payload = JSONObject().put("total_sent", 0).put("total_queued", queued.length())
                .put("reminders", queued).put("skipped", skipped)
            val bytes = JSONObject().put("success", true).put("data", payload).toString().toByteArray()
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        server.start()
        try {
            ApiClientProvider.createService("http://127.0.0.1:${server.address.port}/api/")
            interceptor.setSession("isolated-reminder")
            val client = NativePaymentReminderClient()
            assertEquals(PaymentReminderResult.Queued(2), client.queueSms(invoice, student))
            assertEquals(PaymentReminderResult.Skipped("İletişim adresi bulunamadı"), client.queueSms(invoice, student))
            assertTrue(runCatching { client.queueSms(invoice, student) }.exceptionOrNull() is IllegalStateException)
            assertEquals(List(3) { "POST /api/payments/finances/reminders/send" }, paths)
            calls.forEach { body ->
                assertEquals(setOf("invoice_ids", "channel"), body.keys().asSequence().toSet())
                assertEquals(invoice, body.getJSONArray("invoice_ids").getString(0))
                assertEquals("SMS", body.getString("channel"))
            }
        } finally {
            server.stop(0)
            ApiClientProvider.createService(oldBase)
            interceptor.setSession(oldToken, oldTenant, oldBranch, oldWorkspace)
        }
    }
}
