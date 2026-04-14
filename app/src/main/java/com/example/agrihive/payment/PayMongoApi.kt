package com.example.agrihive.payment

import com.example.agrihive.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// ── PayMongo REST models ──────────────────────────────────────────────────────

data class PayMongoLinkRequest(val data: LinkData) {
    data class LinkData(val attributes: Attributes)
    data class Attributes(
        val amount: Int,          // in centavos
        val description: String,
        val currency: String = "PHP",
        val remarks: String = ""
    )
}

data class PayMongoLinkResponse(val data: LinkData?) {
    data class LinkData(val id: String, val attributes: Attributes)
    data class Attributes(
        val checkout_url: String,
        val reference_number: String,
        val status: String
    )
}

data class PayMongoSourceRequest(val data: SourceData) {
    data class SourceData(val attributes: Attributes)
    data class Attributes(
        val amount: Int,
        val currency: String = "PHP",
        val type: String,         // "gcash" or "paymaya"
        val redirect: Redirect,
        val billing: Billing? = null
    )
    data class Redirect(val success: String, val failed: String)
    data class Billing(
        val name: String,
        val email: String,
        val phone: String = "",
        val address: Address = Address()
    )
    data class Address(
        val line1: String = "N/A",
        val city: String = "Cebu City",
        val state: String = "Cebu",
        val country: String = "PH",
        val postal_code: String = "6000"
    )
}

data class PayMongoSourceResponse(val data: SourceData?) {
    data class SourceData(val id: String, val attributes: Attributes)
    data class Attributes(
        val redirect: Redirect,
        val status: String,
        val amount: Int,
        val type: String
    )
    data class Redirect(val checkout_url: String, val success: String, val failed: String)
}

// ── Retrofit interface ────────────────────────────────────────────────────────

interface PayMongoApi {

    @POST("v1/links")
    suspend fun createLink(@Body body: PayMongoLinkRequest): Response<PayMongoLinkResponse>

    @POST("v1/sources")
    suspend fun createSource(@Body body: PayMongoSourceRequest): Response<PayMongoSourceResponse>

    companion object {
        // Key is read from local.properties via BuildConfig — never hardcoded in source
        private val SECRET_KEY get() = BuildConfig.PAYMONGO_SECRET_KEY

        private const val BASE_URL = "https://api.paymongo.com/"

        fun create(): PayMongoApi {
            val credentials = okhttp3.Credentials.basic(SECRET_KEY, "")
            val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val req = chain.request().newBuilder()
                        .addHeader("Authorization", credentials)
                        .addHeader("Content-Type", "application/json")
                        .build()
                    chain.proceed(req)
                }
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PayMongoApi::class.java)
        }
    }
}
