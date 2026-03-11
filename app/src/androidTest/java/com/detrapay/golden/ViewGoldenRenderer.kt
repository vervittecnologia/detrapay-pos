package com.detrapay.golden

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import com.detrapay.R

object ViewGoldenRenderer {

    fun themedInflater(context: Context): LayoutInflater {
        val themedContext = ContextThemeWrapper(context, R.style.Theme_Detrapay)
        return LayoutInflater.from(themedContext)
    }

    fun render(view: View, width: Int, height: Int): Bitmap {
        view.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, width, height)
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = Canvas(bitmap)
            view.draw(canvas)
        }
    }
}
