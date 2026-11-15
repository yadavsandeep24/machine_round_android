package com.example.starter.feature.organizations.ui

import com.example.starter.core.common.AppResult
import com.example.starter.feature.organizations.domain.Filing
import com.example.starter.feature.organizations.domain.Organization
import com.example.starter.feature.organizations.domain.OrganizationDetail
import com.example.starter.feature.organizations.domain.OrganizationPage
import com.example.starter.feature.organizations.domain.OrganizationRepository
import java.io.IOException
import kotlinx.coroutines.delay

/** 60 organizations; names contain their index so tests can search. Pages of 25 -> 25, 25, 10. */
class FakeOrganizationRepository(private val latencyMs: Long = 100) : OrganizationRepository {
    private val all = List(60) { Organization(ein = 100_000_000L + it, name = "Org $it", location = "City, ST", nteeCode = null) }
    val requests = mutableListOf<Pair<String, Int>>()
    val failingPages = mutableSetOf<Int>()
    var failDetail = false

    override suspend fun search(query: String, page: Int): AppResult<OrganizationPage> {
        requests += query to page
        delay(latencyMs)
        if (page in failingPages) return AppResult.Failure(IOException("boom"))
        val matches = all.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
        val items = matches.drop(page * PAGE).take(PAGE)
        return AppResult.Success(OrganizationPage(items, page, matches.size, endReached = (page + 1) * PAGE >= matches.size))
    }

    override suspend fun detail(ein: Long): AppResult<OrganizationDetail> {
        delay(latencyMs)
        if (failDetail) return AppResult.Failure(IOException("boom"))
        return AppResult.Success(OrganizationDetail(ein, "Org ${ein - 100_000_000L}", "1 Main St", "B99", listOf(Filing(2023, 10, 5, 100))))
    }

    private companion object {
        const val PAGE = 25
    }
}
