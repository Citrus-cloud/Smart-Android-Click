package com.clickflow.android.imageclick

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.File
import java.util.UUID

private const val PREFS_NAME = "clickflow_image_templates"
private const val KEY_TEMPLATES = "templates"

data class ImageClickTemplate(
    val id: String,
    val name: String,
    val filePath: String,
    val width: Int,
    val height: Int,
    val threshold: Float = 0.82f,
    val tapX: Float = 0.5f,
    val tapY: Float = 0.5f,
    val regionLeft: Float = 0f,
    val regionTop: Float = 0f,
    val regionRight: Float = 1f,
    val regionBottom: Float = 1f,
    val continuous: Boolean = false,
    val scaleMin: Float = 0.85f,
    val scaleMax: Float = 1.15f,
)

object ImageClickTemplateStore {
    fun copyUriAsTemplate(context: Context, uri: Uri, number: Int): ImageClickTemplate? {
        return runCatching {
            val dir = File(context.filesDir, "image_templates").apply { mkdirs() }
            val id = UUID.randomUUID().toString()
            val out = File(dir, "$id.png")
            context.contentResolver.openInputStream(uri)?.use { input ->
                out.outputStream().use { output -> input.copyTo(output) }
            } ?: return null

            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(out.absolutePath, opts)
            ImageClickTemplate(
                id = id,
                name = "Шаблон $number",
                filePath = out.absolutePath,
                width = opts.outWidth.coerceAtLeast(1),
                height = opts.outHeight.coerceAtLeast(1),
            )
        }.getOrNull()
    }

    fun loadTemplates(context: Context): List<ImageClickTemplate> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_TEMPLATES, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split(";").mapNotNull { encoded ->
            val line = String(Base64.decode(encoded, Base64.NO_WRAP))
            val p = line.split("|")
            if (p.size < 8) return@mapNotNull null
            ImageClickTemplate(
                id = p[0],
                name = p[1],
                filePath = p[2],
                width = p[3].toIntOrNull() ?: 1,
                height = p[4].toIntOrNull() ?: 1,
                threshold = p[5].toFloatOrNull() ?: 0.82f,
                tapX = p[6].toFloatOrNull() ?: 0.5f,
                tapY = p[7].toFloatOrNull() ?: 0.5f,
                regionLeft = p.getOrNull(8)?.toFloatOrNull()?.coerceIn(0f, 0.95f) ?: 0f,
                regionTop = p.getOrNull(9)?.toFloatOrNull()?.coerceIn(0f, 0.95f) ?: 0f,
                regionRight = p.getOrNull(10)?.toFloatOrNull()?.coerceIn(0.05f, 1f) ?: 1f,
                regionBottom = p.getOrNull(11)?.toFloatOrNull()?.coerceIn(0.05f, 1f) ?: 1f,
                continuous = p.getOrNull(12)?.toBooleanStrictOrNull() ?: false,
                scaleMin = p.getOrNull(13)?.toFloatOrNull()?.coerceIn(0.5f, 2f) ?: 0.85f,
                scaleMax = p.getOrNull(14)?.toFloatOrNull()?.coerceIn(0.5f, 2f) ?: 1.15f,
            ).normalized()
        }.filter { File(it.filePath).exists() }
    }

    fun saveTemplates(context: Context, templates: List<ImageClickTemplate>) {
        val raw = templates.joinToString(";") { t0 ->
            val t = t0.normalized()
            val line = listOf(
                t.id,
                t.name,
                t.filePath,
                t.width,
                t.height,
                t.threshold,
                t.tapX,
                t.tapY,
                t.regionLeft,
                t.regionTop,
                t.regionRight,
                t.regionBottom,
                t.continuous,
                t.scaleMin,
                t.scaleMax,
            ).joinToString("|")
            Base64.encodeToString(line.toByteArray(), Base64.NO_WRAP)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_TEMPLATES, raw).apply()
    }
}

fun ImageClickTemplate.normalizedRegion(): ImageClickTemplate = normalized()

fun ImageClickTemplate.normalized(): ImageClickTemplate {
    val left = regionLeft.coerceIn(0f, 0.95f)
    val top = regionTop.coerceIn(0f, 0.95f)
    val right = regionRight.coerceIn(left + 0.05f, 1f)
    val bottom = regionBottom.coerceIn(top + 0.05f, 1f)
    val minScale = scaleMin.coerceIn(0.5f, 2f)
    val maxScale = scaleMax.coerceIn(minScale, 2f)
    return copy(
        threshold = threshold.coerceIn(0.5f, 0.99f),
        tapX = tapX.coerceIn(0f, 1f),
        tapY = tapY.coerceIn(0f, 1f),
        regionLeft = left,
        regionTop = top,
        regionRight = right,
        regionBottom = bottom,
        scaleMin = minScale,
        scaleMax = maxScale,
    )
}
