@file:Suppress("MatchingDeclarationName") // routes live next to the graph

package com.example.starter.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.starter.R
import com.example.starter.core.ui.CenteredText
import kotlinx.serialization.Serializable

// Type-safe routes (Navigation 2.8+). Add one @Serializable object/class per screen.
@Serializable data object HomeDestination

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = HomeDestination) {
        composable<HomeDestination> {
            Scaffold { padding ->
                Box(Modifier.padding(padding).fillMaxSize()) {
                    CenteredText(stringResource(R.string.home_placeholder))
                }
            }
        }
    }
}
