package com.yarnspace.app.core.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
	@ApplicationContext private val context: Context
) {

	private val PREF_NAME = "auth_prefs"
	private val TOKEN_KEY = "auth_token"
	private val USERNAME_KEY = "auth_username"

	private val prefs: EncryptedSharedPreferences by lazy {
		val masterKey = MasterKey.Builder(context)
			.setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
			.build()

		@Suppress("DEPRECATION")
		EncryptedSharedPreferences.create(
			context,
			PREF_NAME,
			masterKey,
			EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
			EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
		) as EncryptedSharedPreferences
	}

	fun saveToken(token: String) {
		prefs.edit().putString(TOKEN_KEY, token).apply()
	}

	fun getToken(): String? {
		return prefs.getString(TOKEN_KEY, null)
	}

	fun saveUsername(username: String) {
		prefs.edit().putString(USERNAME_KEY, username).apply()
	}

	fun getUsername(): String? {
		return prefs.getString(USERNAME_KEY, null)
	}

	fun clearToken() {
		prefs.edit().remove(TOKEN_KEY).remove(USERNAME_KEY).apply()
	}
}