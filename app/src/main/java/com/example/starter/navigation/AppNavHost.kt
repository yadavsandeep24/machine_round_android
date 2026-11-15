@file:Suppress("MatchingDeclarationName") // routes live next to the graph

package com.example.starter.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.starter.feature.posts.ui.detail.PostDetailRoute
import com.example.starter.feature.posts.ui.list.PostListRoute
import kotlinx.serialization.Serializable

// Type-safe routes (Navigation 2.8+). Add one @Serializable object/class per screen.
@Serializable data object PostListDestination

@Serializable data class PostDetailDestination(val id: Int)

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = PostListDestination) {
        composable<PostListDestination> {
            PostListRoute(onOpenDetail = { id -> navController.navigate(PostDetailDestination(id)) })
        }
        composable<PostDetailDestination> {
            PostDetailRoute(onBack = { navController.popBackStack() })
        }
    }
}
