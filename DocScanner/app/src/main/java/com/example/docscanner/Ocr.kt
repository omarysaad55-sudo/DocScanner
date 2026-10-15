package com.example.docscanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Offline Arabic + English OCR using Tesseract. Language data is downloaded once on first use. */
object Ocr {
    private val langs = listOf("ara", "eng")
    private const val BASE = "https://raw.githubusercontent.com/tesseract-ocr/tessdata_fast/main/"

    /** Downloads ara/eng traineddata the first time (needs internet once, ~20 MB). */
    fun ensureData(ctx: Context) {
        val dir = File(ctx.filesDir, "tessdata").apply { mkdirs() }
        for (l in langs) {
            val target = File(dir, "$l.traineddata")
            if (target.exists() && target.length() > 100_000) continue
            val tmp = File(dir, "$l.traineddata.part")
            val conn = URL(BASE + "$l.traineddata").openConnection() as HttpURLConnection
            conn.connectTimeout = 20_000
            conn.readTimeout = 60_000
            conn.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it) } }
            if (!tmp.renameTo(target)) error("Could not save $l language data")
        }
    }

    fun recognize(ctx: Context, uri: Uri): String {
        ensureData(ctx)
        val bmp = loadBitmap(ctx, uri) ?: error("Could not open image")
        val api = TessBaseAPI()
        try {
            if (!api.init(ctx.filesDir.absolutePath, langs.joinToString("+"))) error("OCR engine failed to start")
            api.setImage(bmp)
            return api.utF8Text.orEmpty().trim()
        } finally {
            api.recycle()
        }
    }

    private fun loadBitmap(ctx: Context, uri: Uri): Bitmap? {
        val cr = ctx.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2600) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val bmp = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
        val orientation = cr.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val deg = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (deg == 0f) return bmp
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(deg) }, true)
    }
}
