package com.example.starter.core.paging

import com.example.starter.core.common.AppResult
import com.example.starter.core.ui.UiText
import com.example.starter.core.ui.toUserMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Bottom-of-list state the UI renders (spinner / retry row / "end"). */
data class PageStatus(val isLoadingMore: Boolean = false, val loadMoreError: UiText? = null, val endReached: Boolean = false) {
    val canLoadMore: Boolean get() = !isLoadingMore && loadMoreError == null && !endReached
}

/**
 * Reusable "load more" state machine, so paging policy lives in one place instead of in every ViewModel.
 *
 * Rules:
 *  - one request at a time (calling [loadNext] while loading does nothing)
 *  - never requests past the end
 *  - no automatic retry after an error: the user calls [retry]
 *
 * Page 1 is usually loaded by the caller (first load, refresh, new search). Afterwards call
 * [reset] with the page to continue from.
 *
 * @param loadPage loads one page and returns `true` when it was the last one.
 */
class Paginator(
    private val scope: CoroutineScope,
    private val loadPage: suspend (page: Int) -> AppResult<Boolean>,
    private val onStatusChange: (PageStatus) -> Unit
) {
    var status: PageStatus = PageStatus()
        private set

    private var nextPage = 1
    private var job: Job? = null

    fun loadNext() {
        if (job?.isActive == true || !status.canLoadMore) return
        val page = nextPage
        job = scope.launch {
            update(status.copy(isLoadingMore = true))
            when (val result = loadPage(page)) {
                is AppResult.Success -> {
                    nextPage = page + 1
                    update(PageStatus(endReached = result.data))
                }
                is AppResult.Failure -> update(PageStatus(loadMoreError = result.throwable.toUserMessage()))
            }
        }
    }

    fun retry() {
        if (status.loadMoreError == null) return
        update(status.copy(loadMoreError = null))
        loadNext()
    }

    /** Cancels any in-flight load and continues from [nextPage] next time. */
    fun reset(nextPage: Int = 1, endReached: Boolean = false) {
        job?.cancel()
        job = null
        this.nextPage = nextPage
        update(PageStatus(endReached = endReached))
    }

    private fun update(newStatus: PageStatus) {
        status = newStatus
        onStatusChange(newStatus)
    }
}
