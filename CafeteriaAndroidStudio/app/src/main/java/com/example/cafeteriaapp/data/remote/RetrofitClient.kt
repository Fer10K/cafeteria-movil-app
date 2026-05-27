package com.example.cafeteriaapp.data.remote

import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "http//10.255.244.236:8000"


    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }


    private val noCacheInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .cacheControl(CacheControl.FORCE_NETWORK) // Forza a ir a la red, ignora el caché
            .build()
        chain.proceed(request)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val apiService: CafeteriaApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // Le pasamos el cliente sin caché
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CafeteriaApiService::class.java)
    }
}