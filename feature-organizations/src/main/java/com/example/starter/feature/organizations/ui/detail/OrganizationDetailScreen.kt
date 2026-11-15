package com.example.starter.feature.organizations.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.starter.core.ui.CenteredLoader
import com.example.starter.core.ui.ErrorView
import com.example.starter.core.ui.UiState
import com.example.starter.feature.organizations.R
import com.example.starter.feature.organizations.domain.Filing
import com.example.starter.feature.organizations.domain.OrganizationDetail

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizationDetailRoute(onBack: () -> Unit, viewModel: OrganizationDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.orgs_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(com.example.starter.core.R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        when (val s = state) {
            UiState.Loading -> CenteredLoader()
            is UiState.Error -> ErrorView(s.message.asString(), viewModel::retry)
            is UiState.Success -> DetailContent(s.data, PaddingValues(top = padding.calculateTopPadding()))
        }
    }
}

@Composable
private fun DetailContent(detail: OrganizationDetail, padding: PaddingValues) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(padding).fillMaxSize()
    ) {
        item {
            Text(detail.name, style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.orgs_ein, formatEin(detail.ein)), style = MaterialTheme.typography.bodyMedium)
            detail.address?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            detail.nteeCode?.let { Text(stringResource(R.string.orgs_ntee, it), style = MaterialTheme.typography.bodyMedium) }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.orgs_filings), style = MaterialTheme.typography.titleMedium)
        }
        if (detail.filings.isEmpty()) {
            item { Text(stringResource(R.string.orgs_no_filings), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            item {
                FilingRow(
                    stringResource(R.string.orgs_col_year),
                    stringResource(R.string.orgs_col_revenue),
                    stringResource(R.string.orgs_col_expenses),
                    stringResource(R.string.orgs_col_assets),
                    header = true
                )
                HorizontalDivider()
            }
            items(detail.filings, key = Filing::year) { f ->
                FilingRow(f.year.toString(), formatUsdCompact(f.revenue), formatUsdCompact(f.expenses), formatUsdCompact(f.assets))
            }
        }
    }
}

@Composable
private fun FilingRow(year: String, revenue: String, expenses: String, assets: String, header: Boolean = false) {
    val weight = if (header) FontWeight.Bold else FontWeight.Normal
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(year, Modifier.weight(1f), fontWeight = weight)
        listOf(revenue, expenses, assets).forEach { Text(it, Modifier.weight(1f), fontWeight = weight, textAlign = TextAlign.End) }
    }
}
