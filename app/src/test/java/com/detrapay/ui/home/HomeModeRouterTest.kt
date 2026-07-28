package com.detrapay.ui.home

import org.junit.Assert.assertTrue
import org.junit.Test

class HomeModeRouterTest {

    @Test
    fun `direct checkout uses its own figma surface`() {
        assertTrue(HomeModeRouter.shouldUseDirectCheckoutSurface())
    }

    @Test
    fun `direct checkout hides bottom navigation`() {
        assertTrue(HomeModeRouter.shouldHideBottomNavigation())
    }
}
