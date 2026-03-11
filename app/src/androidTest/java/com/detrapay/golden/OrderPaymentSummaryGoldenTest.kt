package com.detrapay.golden

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class OrderPaymentSummaryGoldenTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun orderPaymentSummary_matchesReference() {
        GoldenBitmapAssert.assertMatches(
            "order_payment_summary.png",
            PaymentGoldenFixtures.renderOrderSummary(),
        )
    }
}
