package com.detrapay.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.widget.ImageView
import com.google.android.material.button.MaterialButton
import java.io.File
import java.io.FileOutputStream

object ImageUtils {

    fun saveImage(context: Context, name: String, bytes: ByteArray): String? {
        return try {
            val file = File(context.filesDir, "icons")
            if (!file.exists()) file.mkdirs()
            
            val imageFile = File(file, "$name.png")
            val fos = FileOutputStream(imageFile)
            fos.write(bytes)
            fos.close()
            imageFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun loadImage(context: Context, name: String, imageView: ImageView) {
        try {
            val file = File(context.filesDir, "icons")
            val imageFile = File(file, "$name.png")
            if (imageFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                imageView.setImageBitmap(bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadImage(context: Context, name: String, button: MaterialButton) {
        try {
            val file = File(context.filesDir, "icons")
            val imageFile = File(file, "$name.png")
            if (imageFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                button.icon = BitmapDrawable(context.resources, bitmap)
                button.iconTint = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getImagePath(context: Context, name: String): String? {
        // First check internal icons folder (downloaded)
        val internalFile = File(context.filesDir, "icons")
        val internalImage = File(internalFile, "$name.png")
        if (internalImage.exists()) return internalImage.absolutePath
        
        // Then check if it's in the special local drawable folder if it exists as a fallback
        // Note: res/drawable/card-brand-icons is not a standard Android path but might be used as file path
        // However, usually we should use R.drawable. 
        return null
    }
}
