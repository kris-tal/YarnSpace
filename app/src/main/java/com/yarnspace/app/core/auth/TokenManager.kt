package com.yarnspace.app.core.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object TokenManager {

	private const val PREF_NAME = "auth_prefs"
	private const val TOKEN_KEY = "auth_token"

	private fun getEncryptedSharedPreferences(context: Context): EncryptedSharedPreferences {
		val masterKey = MasterKey.Builder(context)
			.setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
			.build()

		@Suppress("DEPRECATION")
		return EncryptedSharedPreferences.create(
			context,
			PREF_NAME,
			masterKey,
			EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
			EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
		) as EncryptedSharedPreferences
	}

	fun saveToken(context: Context, token: String) {
		val prefs = getEncryptedSharedPreferences(context)
		prefs.edit().putString(TOKEN_KEY, token).apply()
	}

	fun getToken(context: Context): String? {
		val prefs = getEncryptedSharedPreferences(context)
		return prefs.getString(TOKEN_KEY, null)
	}

	fun clearToken(context: Context) {
		val prefs = getEncryptedSharedPreferences(context)
		prefs.edit().remove(TOKEN_KEY).apply()
	}
}


