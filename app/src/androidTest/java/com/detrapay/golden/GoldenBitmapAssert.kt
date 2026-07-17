package com.detrapay.golden

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

object GoldenBitmapAssert {

    fun assertMatches(assetName: String, actual: Bitmap, maxDiffRatio: Double = 0.35) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val expected = instrumentation.context.assets.open("goldens/$assetName").use(BitmapFactory::decodeStream)

        requireNotNull(expected) { "Nao foi possivel ler o golden goldens/$assetName" }
        require(actual.width == expected.width && actual.height == expected.height) {
            "Dimensoes diferentes para $assetName. Esperado ${expected.width}x${expected.height}, atual ${actual.width}x${actual.height}"
        }

        val diffRatio = computeDiffRatio(expected, actual)
        if (diffRatio > maxDiffRatio) {
            val directory = File(context.getExternalFilesDir("test-artifacts"), "goldens").apply { mkdirs() }
            FileOutputStream(File(directory, assetName.replace(".png", "_actual.png"))).use {
                actual.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }

        assertTrue("Golden $assetName divergente. diffRatio=$diffRatio", diffRatio <= maxDiffRatio)
    }

    private fun computeDiffRatio(expected: Bitmap, actual: Bitmap): Double {
        var different = 0
        val total = expected.width * expected.height
        for (y in 0 until expected.height) {
            for (x in 0 until expected.width) {
                val exp = expected.getPixel(x, y)
                val act = actual.getPixel(x, y)
                val r = abs(android.graphics.Color.red(exp) - android.graphics.Color.red(act))
                val g = abs(android.graphics.Color.green(exp) - android.graphics.Color.green(act))
                val b = abs(android.graphics.Color.blue(exp) - android.graphics.Color.blue(act))
                val a = abs(android.graphics.Color.alpha(exp) - android.graphics.Color.alpha(act))
                if (r + g + b + a > 48) different++
            }
        }
        return different.toDouble() / total.toDouble()
    }
}
