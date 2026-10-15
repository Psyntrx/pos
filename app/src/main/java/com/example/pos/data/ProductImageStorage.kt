package com.example.pos.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.util.UUID

object ProductImageStorage {
    private const val MAX_IMAGE_DIMENSION = 720
    private const val FILE_PREFIX = "product-"

    fun save(context: Context, source: Uri): String? = runCatching {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (bounds.outWidth / sampleSize > MAX_IMAGE_DIMENSION * 2 ||
            bounds.outHeight / sampleSize > MAX_IMAGE_DIMENSION * 2
        ) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = resolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        persistBitmap(context, decoded).also { decoded.recycle() }
    }.getOrNull()

    fun save(context: Context, bitmap: Bitmap): String? =
        runCatching { persistBitmap(context, bitmap) }.getOrNull()

    private fun persistBitmap(context: Context, decoded: Bitmap): String? {
        val side = minOf(decoded.width, decoded.height)
        if (side <= 0) return null
        val left = (decoded.width - side) / 2
        val top = (decoded.height - side) / 2
        val square = Bitmap.createBitmap(decoded, left, top, side, side)
        val scaled = if (side > MAX_IMAGE_DIMENSION) {
            Bitmap.createScaledBitmap(square, MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION, true)
        } else {
            square
        }

        val directory = File(context.filesDir, "product-images").apply { mkdirs() }
        val file = File(directory, "$FILE_PREFIX${UUID.randomUUID()}.jpg")
        try {
            file.outputStream().use { output ->
                if (!scaled.compress(Bitmap.CompressFormat.JPEG, 88, output)) {
                    throw IllegalStateException("Could not encode product photo")
                }
            }
            return Uri.fromFile(file).toString()
        } finally {
            if (scaled !== square) scaled.recycle()
            if (square !== decoded) square.recycle()
        }
    }

    fun deleteIfOwned(context: Context, imageUri: String?) {
        if (imageUri.isNullOrBlank()) return
        runCatching {
            val target = File(Uri.parse(imageUri).path.orEmpty()).canonicalFile
            val imageDirectory = File(context.filesDir, "product-images").canonicalFile
            if (target.parentFile == imageDirectory && target.name.startsWith(FILE_PREFIX) && target.extension == "jpg") {
                target.delete()
            }
        }
    }
}
