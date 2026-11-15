@file:Suppress("MatchingDeclarationName") // routes live next to the graph

package com.example.starter.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.starter.feature.course.ui.CourseRoute
import kotlinx.serialization.Serializable

@Serializable data object CourseDestination

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = CourseDestination) {
        composable<CourseDestination> { CourseRoute() }
    }
}
