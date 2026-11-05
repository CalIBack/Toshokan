package com.example.animeapp.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // true  = datos de prueba locales (FakeJikanApi), sin internet
    // false = API real de Jikan
    private const val USE_FAKE_DATA = true // ← cambiar a false cuando Jikan vuelva a responder

    private const val BASE_URL = "https://api.jikan.moe/v4/"

    private val realApi: JikanApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL) // debe terminar en "/"
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(JikanApi::class.java)
    }

    val api: JikanApi
        get() = if (USE_FAKE_DATA) FakeJikanApi else realApi
}
