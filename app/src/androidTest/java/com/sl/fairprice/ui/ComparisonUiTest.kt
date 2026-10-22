package com.sl.fairprice.ui

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.sl.fairprice.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

/** Actual Android UI interactions, no network, images or test accounts. */
@RunWith(AndroidJUnit4::class)
class ComparisonUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun enter(number: Int, price: String, quantity: String) {
        compose.onNodeWithTag("offer-$number-price").performScrollTo().performTextInput(price)
        compose.onNodeWithTag("offer-$number-quantity").performScrollTo().performTextInput(quantity)
    }

    @Test fun manualComparisonRequiresConfirmationAndRevokesItOnEdit() {
        compose.onNodeWithTag("offer-1-confirm").performScrollTo().assertIsNotEnabled()
        enter(1, "100", "500")
        enter(2, "120", "400")
        compose.onNodeWithTag("comparison-winner").assertDoesNotExist()
        compose.onNodeWithTag("offer-1-confirm").performScrollTo().performClick()
        compose.onNodeWithTag("comparison-winner").assertDoesNotExist()
        compose.onNodeWithTag("offer-2-confirm").performScrollTo().performClick()
        compose.onNodeWithTag("comparison-winner").assertExists().assertTextContains("1", substring = true)
        compose.onNodeWithTag("offer-1-price").performScrollTo().performTextReplacement("300")
        compose.onNodeWithTag("comparison-winner").assertDoesNotExist()
    }

    @Test fun incompatibleDimensionsNeverShowWinner() {
        enter(1, "100", "500")
        enter(2, "150", "500")
        compose.onNodeWithTag("offer-2-measure-MILLILITRE").performScrollTo().performClick()
        compose.onNodeWithTag("offer-1-confirm").performScrollTo().performClick()
        compose.onNodeWithTag("offer-2-confirm").performScrollTo().performClick()
        compose.onNodeWithTag("comparison-incompatible").assertExists()
        compose.onNodeWithTag("comparison-winner").assertDoesNotExist()
    }
}
