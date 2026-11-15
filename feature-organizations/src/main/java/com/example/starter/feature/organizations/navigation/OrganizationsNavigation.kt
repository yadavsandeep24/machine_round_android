package com.example.starter.feature.organizations.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.starter.feature.organizations.ui.detail.OrganizationDetailRoute
import com.example.starter.feature.organizations.ui.list.OrganizationListRoute
import kotlinx.serialization.Serializable

@Serializable data object OrganizationListDestination

@Serializable data class OrganizationDetailDestination(val ein: Long)

/** The feature owns its screens and routes; :app only calls this from its NavHost. */
fun NavGraphBuilder.organizationsGraph(navController: NavController) {
    composable<OrganizationListDestination> {
        OrganizationListRoute(onOpenDetail = { ein -> navController.navigate(OrganizationDetailDestination(ein)) })
    }
    composable<OrganizationDetailDestination> {
        OrganizationDetailRoute(onBack = { navController.popBackStack() })
    }
}
