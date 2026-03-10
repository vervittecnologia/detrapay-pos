package com.detrapay.testing

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Before
import org.junit.Rule

abstract class BaseUiTest {

    @get:Rule(order = 0)
    val screenshotOnFailureRule = ScreenshotOnFailureRule()

    protected val device: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun prepareDevice() {
        if (!device.isScreenOn) {
            device.wakeUp()
        }
        device.waitForIdle()
    }
}
