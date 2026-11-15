package com.example.starter.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.starter.feature.organizations.navigation.OrganizationListDestination
import com.example.starter.feature.organizations.navigation.organizationsGraph

/** The app module only composes feature graphs; it doesn't know any feature's screens. */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = OrganizationListDestination) {
        organizationsGraph(navController)
    }
}
