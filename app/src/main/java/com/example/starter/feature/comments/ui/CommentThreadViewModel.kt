package com.example.starter.feature.comments.ui

import androidx.lifecycle.viewModelScope
import com.example.starter.core.common.AppResult
import com.example.starter.core.common.DefaultDispatcher
import com.example.starter.core.mvi.MviViewModel
import com.example.starter.core.ui.toUserMessage
import com.example.starter.feature.comments.domain.Comment
import com.example.starter.feature.comments.domain.CommentFilter
import com.example.starter.feature.comments.domain.CommentRepository
import com.example.starter.feature.comments.domain.Preference
import com.example.starter.feature.comments.domain.ThreadFlattener
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * comments + preference + threshold + collapsed  ->  filter (BFS) + flatten (DFS)  ->  rows.
 * The tree work is CPU-bound, so it runs on the Default dispatcher (flowOn), never on Main.
 */
@HiltViewModel
class CommentThreadViewModel @Inject constructor(
    private val repository: CommentRepository,
    @DefaultDispatcher private val computeDispatcher: CoroutineDispatcher
) : MviViewModel<CommentThreadIntent, CommentThreadState, CommentThreadEffect>(CommentThreadState()) {

    private val comments = MutableStateFlow<List<Comment>>(emptyList())
    private val preference = MutableStateFlow(Preference.SHOW_ALL)
    private val threshold = MutableStateFlow<Int?>(null)
    private val collapsed = MutableStateFlow<Set<Int>>(emptySet())
    private var loadJob: Job? = null

    init {
        load()
        viewModelScope.launch {
            combine(comments, preference, threshold, collapsed) { all, pref, limit, folded ->
                val hidden = CommentFilter.hiddenIds(all, pref, limit)
                Result(pref, limit, ThreadFlattener.flatten(all, hidden, folded), hidden.size)
            }
                .flowOn(computeDispatcher)
                .collect { r ->
                    setState { copy(preference = r.preference, scoreThreshold = r.threshold, rows = r.rows, hiddenCount = r.hiddenCount) }
                }
        }
    }

    override fun handleIntent(intent: CommentThreadIntent) {
        when (intent) {
            CommentThreadIntent.Retry -> load()
            is CommentThreadIntent.SetPreference -> preference.value = intent.preference
            is CommentThreadIntent.SetScoreThreshold -> threshold.value = intent.threshold
            is CommentThreadIntent.ToggleCollapse -> collapsed.update { set ->
                if (intent.commentId in set) set - intent.commentId else set + intent.commentId
            }
        }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            when (val result = repository.comments(POST_ID)) {
                is AppResult.Success -> {
                    comments.value = result.data
                    setState { copy(isLoading = false) }
                }
                is AppResult.Failure -> setState { copy(isLoading = false, error = result.throwable.toUserMessage()) }
            }
        }
    }

    private data class Result(
        val preference: Preference,
        val threshold: Int?,
        val rows: List<com.example.starter.feature.comments.domain.CommentRow>,
        val hiddenCount: Int
    )

    private companion object {
        const val POST_ID = 1
    }
}
