package com.detrapay.smoke

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.detrapay.R
import com.detrapay.testing.BaseUiTest
import com.detrapay.testing.pages.RegistrationOrderDataScreen
import com.detrapay.testing.waitForView
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
    val scenarioRule = ActivityScenarioRule(RegistrationActivity::class.java)

    @Before
    fun inject() {
        hiltRule.inject()
    }

    @Test
    fun launchRegistration_displaysFirstStepShell() {
        onView(isRoot()).perform(waitForView(withId(R.id.registrationOrderDataNextBtn)))
        RegistrationOrderDataScreen.assertDisplayed()
    }

    @Test
    fun launchRegistration_allowsAccessToPrimaryAction() {
        onView(isRoot()).perform(waitForView(withId(R.id.registrationOrderDataNextBtn)))
        onView(withId(R.id.registrationOrderDataNextBtn)).check(matches(isDisplayed()))
    }
}
