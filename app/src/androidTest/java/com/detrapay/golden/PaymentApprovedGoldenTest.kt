package com.detrapay.golden

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class PaymentApprovedGoldenTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun paymentApproved_matchesReference() {
        GoldenBitmapAssert.assertMatches(
            "payment_approved.png",
            PaymentGoldenFixtures.renderPaymentApproved(),
        )
    }
}
