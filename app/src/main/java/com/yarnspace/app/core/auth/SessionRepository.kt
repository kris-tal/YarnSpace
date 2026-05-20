package com.yarnspace.app.core.auth

import android.content.Context

/* Needs further refactor */
class SessionRepository(context: Context) {

	private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

	fun saveAccessToken(token: String) {
		prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
	}

	fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

	fun isLoggedIn(): Boolean = !getAccessToken().isNullOrBlank()

	fun clearSession() {
		prefs.edit().remove(KEY_ACCESS_TOKEN).apply()
	}

	companion object {
		private const val PREFS_NAME = "session"
		private const val KEY_ACCESS_TOKEN = "access_token"
	}
}


