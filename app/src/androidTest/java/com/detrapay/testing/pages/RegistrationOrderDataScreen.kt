package com.detrapay.testing.pages

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.detrapay.R

object RegistrationOrderDataScreen {

    fun assertDisplayed() {
        onView(withId(R.id.registrationProgress)).check(matches(isDisplayed()))
        onView(withId(R.id.tvProgressLabel)).check(matches(withText("DADOS DO CLIENTE")))
        onView(withId(R.id.registrationOrderDataNextBtn)).check(matches(isDisplayed()))
    }
}
