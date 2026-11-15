package com.example.starter.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.starter.feature.pagedposts.ui.PagedPostListRoute
import com.example.starter.feature.posts.ui.detail.PostDetailRoute
import com.example.starter.feature.posts.ui.list.PostListRoute
import kotlinx.serialization.Serializable

// Type-safe routes (Navigation 2.8+)
@Serializable data object PostListDestination

@Serializable data class PostDetailDestination(val id: Int)

@Serializable data object PagedPostListDestination

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = PostListDestination) {
        composable<PostListDestination> {
            PostListRoute(
                onOpenDetail = { id -> navController.navigate(PostDetailDestination(id)) },
                onOpenPaging3 = { navController.navigate(PagedPostListDestination) }
            )
        }
        composable<PostDetailDestination> {
            PostDetailRoute(onBack = { navController.popBackStack() })
        }
        composable<PagedPostListDestination> {
            PagedPostListRoute(onBack = { navController.popBackStack() })
        }
    }
}
