package com.example.starter.feature.posts.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.starter.R
import com.example.starter.core.ui.CenteredLoader
import com.example.starter.core.ui.CenteredText
import com.example.starter.core.ui.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailRoute(onBack: () -> Unit, viewModel: PostDetailViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.post_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        when (val s = state) {
            UiState.Loading -> CenteredLoader()
            is UiState.Error -> CenteredText(s.message.asString())
            is UiState.Success -> Column(
                Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState())
            ) {
                Text(s.data.title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(12.dp))
                Text(s.data.body, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
