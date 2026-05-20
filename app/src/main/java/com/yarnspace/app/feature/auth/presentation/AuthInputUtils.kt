package com.yarnspace.app.feature.auth.presentation

import com.google.android.material.textfield.TextInputEditText

fun TextInputEditText.trimmedText(): String = text?.toString()?.trim().orEmpty()

fun TextInputEditText.rawText(): String = text?.toString().orEmpty()


