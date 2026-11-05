package com.example.animeapp.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.squareup.picasso.Picasso

// Portadas con Picasso; si no hay, genera una con iniciales
object CoverLoader {
    private val palette = listOf(
        0xFF6A11CB to 0xFF2575FC,
        0xFFFF4F81 to 0xFF7B2FF7,
        0xFFF7971E to 0xFFFF5858,
        0xFF11998E to 0xFF38EF7D,
        0xFF283C86 to 0xFF45A247,
        0xFFC33764 to 0xFF1D2671
    )

    fun load(imageView: ImageView, url: String?, title: String) {
        val placeholder = placeholderFor(imageView.context, title)
        if (url.isNullOrBlank()) {
            Picasso.get().cancelRequest(imageView)
            imageView.setImageDrawable(placeholder)
            return
        }
        Picasso.get()
            .load(url)
            .placeholder(placeholder)
            .error(placeholder)
            .fit()
            .centerCrop()
            .into(imageView)
    }

    private fun placeholderFor(context: Context, title: String): Drawable {
        val width = 300
        val height = 430
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val (start, end) = palette[Math.floorMod(title.hashCode(), palette.size)]
        val background = Paint().apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                start.toInt(), end.toInt(), Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), background)

        val initials = title.split(" ", ":", "-")
            .filter { it.isNotBlank() && it.first().isLetterOrDigit() }
            .take(2)
            .joinToString("") { it.first().uppercase() }

        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xE6FFFFFF.toInt()
            textSize = 120f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val y = height / 2f - (text.descent() + text.ascent()) / 2f
        canvas.drawText(initials, width / 2f, y, text)

        return BitmapDrawable(context.resources, bitmap)
    }
}
