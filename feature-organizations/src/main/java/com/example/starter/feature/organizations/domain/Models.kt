package com.example.starter.feature.organizations.domain

import com.example.starter.core.common.AppResult

data class Organization(
    val ein: Long,
    val name: String,
    /** "Seattle, WA"; null when the API has neither field. */
    val location: String?,
    val nteeCode: String?
)

data class OrganizationPage(val items: List<Organization>, val page: Int, val totalResults: Int, val endReached: Boolean)

data class Filing(val year: Int, val revenue: Long?, val expenses: Long?, val assets: Long?)

data class OrganizationDetail(
    val ein: Long,
    val name: String,
    val address: String?,
    val nteeCode: String?,
    /** Newest year first. */
    val filings: List<Filing>
)

interface OrganizationRepository {
    /** [query] blank = browse everything. [page] is 0-based. */
    suspend fun search(query: String, page: Int): AppResult<OrganizationPage>

    suspend fun detail(ein: Long): AppResult<OrganizationDetail>
}
