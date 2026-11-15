package com.example.starter.feature.organizations

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.starter.core.paging.PageStatus
import com.example.starter.core.ui.UiText
import com.example.starter.feature.organizations.domain.Organization
import com.example.starter.feature.organizations.ui.list.OrganizationListIntent
import com.example.starter.feature.organizations.ui.list.OrganizationListScreen
import com.example.starter.feature.organizations.ui.list.OrganizationListState
import com.example.starter.feature.organizations.ui.list.TAG_SEARCH
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Functional UI tests of the stateless screen: render a state, interact, assert the intents sent. */
@RunWith(AndroidJUnit4::class)
class OrganizationListScreenTest {

    @get:Rule val compose = createComposeRule()

    private val intents = mutableListOf<OrganizationListIntent>()
    private val loaded = OrganizationListState(
        loadedQuery = "",
        isLoading = false,
        totalResults = 2,
        items = listOf(Organization(1, "Red Cross", "Washington, DC", "P20"), Organization(2, "Food Bank", "Austin, TX", null))
    )

    private fun show(state: OrganizationListState) = compose.setContent { OrganizationListScreen(state) { intents += it } }

    @Test
    fun rowsAndCountAreShown() {
        show(loaded)
        compose.onNodeWithText("Red Cross").assertIsDisplayed()
        compose.onNodeWithText("Washington, DC · NTEE P20").assertIsDisplayed()
        compose.onNodeWithText("2 organizations").assertIsDisplayed()
    }

    @Test
    fun tappingARowSendsItsEin() {
        show(loaded)
        compose.onNodeWithText("Food Bank").performClick()
        assertTrue(OrganizationListIntent.OrganizationClicked(2) in intents)
    }

    @Test
    fun typingSendsQueryChanged() {
        show(loaded)
        compose.onNodeWithTag(TAG_SEARCH).performTextInput("cross")
        assertTrue(OrganizationListIntent.QueryChanged("cross") in intents)
    }

    @Test
    fun emptyResultExplainsTheQuery() {
        show(OrganizationListState(query = "zzz", loadedQuery = "zzz", isLoading = false))
        compose.onNodeWithText("No organizations match \"zzz\"").assertIsDisplayed()
    }

    @Test
    fun loadMoreErrorOffersRetry() {
        show(loaded.copy(paging = PageStatus(loadMoreError = UiText.Dynamic("Network down"))))
        compose.onNodeWithText("Retry").performClick()
        assertTrue(OrganizationListIntent.RetryLoadMore in intents)
    }
}
