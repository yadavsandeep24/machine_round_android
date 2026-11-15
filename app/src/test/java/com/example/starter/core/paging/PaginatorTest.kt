package com.example.starter.core.paging

import com.example.starter.core.common.AppResult
import com.example.starter.core.ui.UiText
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** Paging policy tested on its own: no ViewModel, no repository. */
@OptIn(ExperimentalCoroutinesApi::class)
class PaginatorTest {

    private val requested = mutableListOf<Int>()
    private val statuses = mutableListOf<PageStatus>()

    private fun TestScope.paginator(lastPage: Int = 3, failOn: Set<Int> = emptySet()) = Paginator(
        scope = this,
        loadPage = { page ->
            requested += page
            if (page in failOn) AppResult.Failure(RuntimeException("fail $page")) else AppResult.Success(page >= lastPage)
        },
        onStatusChange = { statuses += it }
    )

    @Test
    fun `loads pages in order and stops at the end`() = runTest {
        val p = paginator(lastPage = 3)
        repeat(5) {
            p.loadNext()
            advanceUntilIdle()
        }
        assertThat(requested).containsExactly(1, 2, 3).inOrder()
        assertThat(p.status.endReached).isTrue()
    }

    @Test
    fun `ignores loadNext while a page is loading`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val p = Paginator(
            scope = this,
            loadPage = { page ->
                requested += page
                gate.await()
                AppResult.Success(false)
            },
            onStatusChange = {}
        )
        p.loadNext()
        advanceUntilIdle()
        assertThat(p.status.isLoadingMore).isTrue()
        repeat(3) { p.loadNext() }
        gate.complete(Unit)
        advanceUntilIdle()
        assertThat(requested).containsExactly(1)
    }

    @Test
    fun `error blocks loadNext until retry`() = runTest {
        val p = paginator(failOn = setOf(2))
        p.loadNext()
        advanceUntilIdle()
        p.loadNext()
        advanceUntilIdle()
        assertThat(p.status.loadMoreError).isEqualTo(UiText.Dynamic("fail 2"))

        p.loadNext()
        advanceUntilIdle()
        assertThat(requested).containsExactly(1, 2).inOrder() // no auto-retry
    }

    @Test
    fun `retry re-requests the failed page`() = runTest {
        var fail = true
        val p = Paginator(
            scope = this,
            loadPage = { page ->
                requested += page
                if (fail) AppResult.Failure(RuntimeException("x")) else AppResult.Success(false)
            },
            onStatusChange = {}
        )
        p.loadNext()
        advanceUntilIdle()
        fail = false
        p.retry()
        advanceUntilIdle()
        assertThat(requested).containsExactly(1, 1).inOrder()
        assertThat(p.status.loadMoreError).isNull()
    }

    @Test
    fun `reset cancels in-flight load and continues from the given page`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val p = Paginator(
            scope = this,
            loadPage = { page ->
                requested += page
                if (page == 1) gate.await()
                AppResult.Success(false)
            },
            onStatusChange = { statuses += it }
        )
        p.loadNext()
        advanceUntilIdle()
        p.reset(nextPage = 5)
        gate.complete(Unit)
        advanceUntilIdle()
        assertThat(p.status).isEqualTo(PageStatus()) // cancelled load never reported success

        p.loadNext()
        advanceUntilIdle()
        assertThat(requested).containsExactly(1, 5).inOrder()
    }
}
