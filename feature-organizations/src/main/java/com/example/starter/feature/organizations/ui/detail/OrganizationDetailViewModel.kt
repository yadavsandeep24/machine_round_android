package com.example.starter.feature.organizations.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.starter.core.common.AppResult
import com.example.starter.core.ui.UiState
import com.example.starter.core.ui.toUserMessage
import com.example.starter.feature.organizations.domain.OrganizationDetail
import com.example.starter.feature.organizations.domain.OrganizationRepository
import com.example.starter.feature.organizations.navigation.OrganizationDetailDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** MVVM: one observable state plus a public retry(). Compare with the list's MVI contract. */
@HiltViewModel
class OrganizationDetailViewModel @Inject constructor(savedStateHandle: SavedStateHandle, private val repository: OrganizationRepository) :
    ViewModel() {

    // Read by property name: works in plain JVM tests (toRoute() needs Robolectric).
    private val ein: Long = checkNotNull(savedStateHandle[OrganizationDetailDestination::ein.name])

    private val _state = MutableStateFlow<UiState<OrganizationDetail>>(UiState.Loading)
    val state: StateFlow<UiState<OrganizationDetail>> = _state.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = when (val result = repository.detail(ein)) {
                is AppResult.Success -> UiState.Success(result.data)
                is AppResult.Failure -> UiState.Error(result.throwable.toUserMessage())
            }
        }
    }
}
