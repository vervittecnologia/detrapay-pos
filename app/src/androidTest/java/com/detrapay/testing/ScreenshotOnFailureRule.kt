package com.detrapay.testing

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenshotOnFailureRule : TestWatcher() {

    override fun failed(e: Throwable?, description: Description) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val device = UiDevice.getInstance(instrumentation)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val directory = File(context.getExternalFilesDir("test-artifacts"), "screenshots")
        directory.mkdirs()

        val safeClassName = description.className.substringAfterLast('.')
        val file = File(directory, "${safeClassName}_${description.methodName}_$timestamp.png")
        device.takeScreenshot(file)
    }
}
