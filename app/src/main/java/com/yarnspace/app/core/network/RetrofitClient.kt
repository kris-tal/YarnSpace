package com.yarnspace.app.core.network

import android.content.Context
import com.yarnspace.app.core.auth.TokenManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Simple singleton Retrofit provider.
 *
 * Note: base URL is currently hardcoded for emulator networking.
 * We'll likely move this to BuildConfig / local.properties later.
 */
object RetrofitClient {
	private const val BASE_URL = "http://10.0.2.2:8000/" // emulator -> host

	@Volatile
	private var apiService: ApiService? = null

	fun getInstance(context: Context): ApiService {
		val existing = apiService
		if (existing != null) return existing

		synchronized(this) {
			val again = apiService
			if (again != null) return again

			val client = OkHttpClient.Builder()
				.addInterceptor { chain ->
					val requestBuilder = chain.request().newBuilder()
					TokenManager.getToken(context.applicationContext)?.let { token ->
						requestBuilder.addHeader("Authorization", "Bearer $token")
					}
					chain.proceed(requestBuilder.build())
				}
				.build()

			val retrofit = Retrofit.Builder()
				.baseUrl(BASE_URL)
				.client(client)
				.addConverterFactory(GsonConverterFactory.create())
				.build()

			val created = retrofit.create(ApiService::class.java)
			apiService = created
			return created
		}
	}
}


