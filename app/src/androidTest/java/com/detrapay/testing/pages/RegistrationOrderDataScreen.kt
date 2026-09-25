package com.detrapay.testing.pages

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText

object RegistrationOrderDataScreen {

    fun assertDisplayed() {
        onView(withText("Dados do pedido")).check(matches(isDisplayed()))
        onView(withText("Revisar pedido")).check(matches(isDisplayed()))
    }
}
