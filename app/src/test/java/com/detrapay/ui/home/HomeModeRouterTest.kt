package com.detrapay.ui.home

import com.detrapay.data.model.SellerAppMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeModeRouterTest {

    @Test
    fun `direct checkout uses its own figma surface`() {
        assertTrue(HomeModeRouter.shouldUseDirectCheckoutSurface(SellerAppMode.DIRECT_CHECKOUT))
        assertFalse(HomeModeRouter.shouldUseSimplifiedSurface(SellerAppMode.DIRECT_CHECKOUT))
    }

    @Test
    fun `direct checkout hides bottom navigation so old order list is not reachable`() {
        assertTrue(HomeModeRouter.shouldHideBottomNavigation(SellerAppMode.DIRECT_CHECKOUT))
    }

    @Test
    fun `complete mode keeps regular home navigation`() {
        assertFalse(HomeModeRouter.shouldUseSimplifiedSurface(SellerAppMode.COMPLETE))
        assertFalse(HomeModeRouter.shouldHideBottomNavigation(SellerAppMode.COMPLETE))
    }
}
