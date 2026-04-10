package com.yarnspace.app

import com.google.android.material.textfield.TextInputEditText

fun TextInputEditText.trimmedText(): String = text?.toString()?.trim().orEmpty()

fun TextInputEditText.rawText(): String = text?.toString().orEmpty()

