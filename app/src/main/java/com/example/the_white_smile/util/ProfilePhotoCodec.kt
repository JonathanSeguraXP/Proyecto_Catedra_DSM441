package com.example.the_white_smile.util

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

/** Encodes small square thumbnails so profiles fit safely in Firestore documents. */
object ProfilePhotoCodec {

    private const val MAX_JPEG_BYTES = 60_000
    private const val MAX_SIDE = 256

    fun encode(resolver: ContentResolver, uri: Uri): String {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "No se pudo leer la imagen" }

        val sample = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val source = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, sample)
        } ?: throw IllegalArgumentException("No se pudo abrir la imagen")

        val side = minOf(source.width, source.height)
        val crop = Bitmap.createBitmap(
            source,
            (source.width - side) / 2,
            (source.height - side) / 2,
            side,
            side
        )
        if (source !== crop) source.recycle()

        var size = MAX_SIDE
        while (size >= 96) {
            val thumbnail = Bitmap.createScaledBitmap(crop, size, size, true)
            for (quality in listOf(76, 64, 52, 40)) {
                val output = ByteArrayOutputStream()
                thumbnail.compress(Bitmap.CompressFormat.JPEG, quality, output)
                val bytes = output.toByteArray()
                if (bytes.size <= MAX_JPEG_BYTES) {
                    if (thumbnail !== crop) thumbnail.recycle()
                    crop.recycle()
                    return Base64.encodeToString(bytes, Base64.NO_WRAP)
                }
            }
            if (thumbnail !== crop) thumbnail.recycle()
            size = (size * 3) / 4
        }

        crop.recycle()
        throw IllegalArgumentException("La imagen no se pudo comprimir lo suficiente")
    }

    fun decode(encoded: String?): Bitmap? = try {
        if (encoded.isNullOrBlank()) null
        else {
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (maxOf(width, height) / sample > 512) sample *= 2
        return sample
    }
}
