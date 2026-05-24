package com.example.cafeteriaapp.data.remote

import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Retrofit

object RetrofitClient {
    private const val BASE_URL = "http://10.255.244.236:8000/"

    val apiService: CafeteriaApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CafeteriaApiService::class.java)
    }
}