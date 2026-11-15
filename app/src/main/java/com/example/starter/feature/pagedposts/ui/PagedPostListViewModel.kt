package com.example.starter.feature.pagedposts.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.starter.core.model.Post
import com.example.starter.feature.pagedposts.domain.ObservePagedPostsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * MVVM + Paging 3. Loading/error/end state lives in LazyPagingItems.loadState,
 * so the ViewModel stays tiny. cachedIn keeps loaded pages across rotation.
 */
@HiltViewModel
class PagedPostListViewModel @Inject constructor(observePagedPosts: ObservePagedPostsUseCase) : ViewModel() {
    val posts: Flow<PagingData<Post>> = observePagedPosts().cachedIn(viewModelScope)
}
