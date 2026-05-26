package com.yarnspace.app.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageUploadUtils {

    private const val DEFAULT_MAX_RAW_BYTES: Int = 10 * 1024 * 1024

    private fun readBytesWithLimit(context: Context, uri: Uri, maxBytes: Int): ByteArray? {
        val resolver = context.contentResolver
        resolver.openInputStream(uri)?.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                total += read
                if (total > maxBytes) return null
                out.write(buffer, 0, read)
            }
            return out.toByteArray()
        }
        return null
    }

    fun compressToJpeg(
        context: Context,
        uri: Uri,
        maxDimensionPx: Int,
        quality: Int,
    ): ByteArray? {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
        val srcW = bounds.outWidth
        val srcH = bounds.outHeight
        if (srcW <= 0 || srcH <= 0) return null

        val largest = max(srcW, srcH)
        var inSampleSize = 1
        while (largest / inSampleSize > maxDimensionPx * 2) {
            inSampleSize *= 2
        }

        val opts = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            this.inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null

        val w = decoded.width
        val h = decoded.height
        val scale = maxDimensionPx.toFloat() / max(w, h).toFloat()
        val finalBitmap = if (scale < 1f) {
            val newW = (w * scale).toInt().coerceAtLeast(1)
            val newH = (h * scale).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(decoded, newW, newH, true).also {
                if (it != decoded) decoded.recycle()
            }
        } else {
            decoded
        }

        return ByteArrayOutputStream().use { out ->
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(0, 100), out)
            if (!finalBitmap.isRecycled) finalBitmap.recycle()
            out.toByteArray()
        }
    }

    fun createJpegPart(
        context: Context,
        uri: Uri,
        formFieldName: String,
        fileName: String,
        maxDimensionPx: Int,
        quality: Int,
    ): MultipartBody.Part? {
        val bytes = compressToJpeg(context, uri, maxDimensionPx, quality)
        if (bytes != null) {
            val body = bytes.toRequestBody("image/jpeg".toMediaType())
            return MultipartBody.Part.createFormData(formFieldName, fileName, body)
        }

        val mime = context.contentResolver.getType(uri)
        val allowed = setOf("image/jpeg", "image/jpg", "image/png", "image/webp")
        if (mime == null || mime !in allowed) return null

        val raw = readBytesWithLimit(context, uri, DEFAULT_MAX_RAW_BYTES) ?: return null
        val rawBody = raw.toRequestBody(mime.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(formFieldName, fileName, rawBody)
    }
}


