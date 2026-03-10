package com.detrapay.testing

import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import org.hamcrest.Matcher

fun waitForView(viewMatcher: Matcher<View>, timeoutMs: Long = 5_000L): ViewAction {
    return object : ViewAction {
        override fun getConstraints(): Matcher<View> = isRoot()

        override fun getDescription(): String = "wait up to $timeoutMs milliseconds for $viewMatcher"

        override fun perform(uiController: UiController, view: View) {
            val endTime = SystemClock.elapsedRealtime() + timeoutMs
            do {
                if (findMatch(view, viewMatcher)) return
                uiController.loopMainThreadForAtLeast(100)
            } while (SystemClock.elapsedRealtime() < endTime)

            throw PerformException.Builder()
                .withActionDescription(description)
                .withViewDescription(view.toString())
                .build()
        }
    }
}

private fun findMatch(root: View, matcher: Matcher<View>): Boolean {
    if (matcher.matches(root)) return true
    if (root is ViewGroup) {
        for (index in 0 until root.childCount) {
            if (findMatch(root.getChildAt(index), matcher)) return true
        }
    }
    return false
}
