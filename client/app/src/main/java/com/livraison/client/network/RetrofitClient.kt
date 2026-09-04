package com.livraison.client.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * ⚠️ Adapte BASE_URL selon où tu testes :
 * - Émulateur Android : "http://10.0.2.2:8000/" (10.0.2.2 pointe vers le
 *   localhost de la machine hôte depuis l'émulateur)
 * - Téléphone physique : l'adresse IP locale de ton ordinateur sur le
 *   même réseau Wi-Fi (ex. "http://192.168.1.42:8000/"), backend lancé
 *   avec `uvicorn app.main:app --host 0.0.0.0`
 * - Backend déployé (ex. URL publique GitHub Codespaces) : son URL HTTPS,
 *   en gardant le "/" final
 */
object RetrofitClient {
            var baseUrl: String = "https://scaling-goldfish-wrw75w4pxvp6f9vpp-8000.app.github.dev/"
        private set

    private var authToken: String? = null

    fun setBaseUrl(url: String) {
        baseUrl = if (url.endsWith("/")) url else "$url/"
        rebuild()
    }

    fun setToken(token: String?) {
        authToken = token
    }

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val request = if (authToken != null) {
            original.newBuilder()
                .addHeader("Authorization", "Bearer $authToken")
                .build()
        } else {
            original
        }
        chain.proceed(request)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private var _service: ApiService = build()

    val service: ApiService
        get() = _service

    private fun build(): ApiService {
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private fun rebuild() {
        _service = build()
    }
}
