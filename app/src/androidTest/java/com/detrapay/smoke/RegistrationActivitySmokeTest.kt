package com.detrapay.smoke

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.detrapay.testing.BaseUiTest
import com.detrapay.ui.registration.RegistrationActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class RegistrationActivitySmokeTest : BaseUiTest() {

    @get:Rule(order = 1)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 2)
    val composeRule = createAndroidComposeRule<RegistrationActivity>()

    @Before
    fun inject() {
        hiltRule.inject()
    }

    @Test
    fun launchRegistration_displaysFirstStepShell() {
        composeRule.onNodeWithText("Dados do pedido").assertIsDisplayed()
    }

    @Test
    fun launchRegistration_allowsAccessToPrimaryAction() {
        composeRule.onNodeWithText("Revisar pedido").assertIsDisplayed()
    }
}
