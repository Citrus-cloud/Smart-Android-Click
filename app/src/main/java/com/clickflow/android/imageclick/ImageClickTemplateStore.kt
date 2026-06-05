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
            if (p.size != 8) return@mapNotNull null
            ImageClickTemplate(
                id = p[0],
                name = p[1],
                filePath = p[2],
                width = p[3].toIntOrNull() ?: 1,
                height = p[4].toIntOrNull() ?: 1,
                threshold = p[5].toFloatOrNull() ?: 0.82f,
                tapX = p[6].toFloatOrNull() ?: 0.5f,
                tapY = p[7].toFloatOrNull() ?: 0.5f,
            )
        }.filter { File(it.filePath).exists() }
    }

    fun saveTemplates(context: Context, templates: List<ImageClickTemplate>) {
        val raw = templates.joinToString(";") { t ->
            val line = listOf(t.id, t.name, t.filePath, t.width, t.height, t.threshold, t.tapX, t.tapY).joinToString("|")
            Base64.encodeToString(line.toByteArray(), Base64.NO_WRAP)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_TEMPLATES, raw).apply()
    }
}
