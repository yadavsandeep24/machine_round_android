package com.example.starter.feature.organizations.data

import com.example.starter.core.common.AppResult
import com.example.starter.core.common.IoDispatcher
import com.example.starter.core.network.safeCall
import com.example.starter.feature.organizations.data.remote.FilingDto
import com.example.starter.feature.organizations.data.remote.NonprofitApi
import com.example.starter.feature.organizations.data.remote.OrganizationDto
import com.example.starter.feature.organizations.domain.Filing
import com.example.starter.feature.organizations.domain.Organization
import com.example.starter.feature.organizations.domain.OrganizationDetail
import com.example.starter.feature.organizations.domain.OrganizationPage
import com.example.starter.feature.organizations.domain.OrganizationRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class OrganizationRepositoryImpl @Inject constructor(private val api: NonprofitApi, @IoDispatcher private val io: CoroutineDispatcher) :
    OrganizationRepository {

    override suspend fun search(query: String, page: Int): AppResult<OrganizationPage> = withContext(io) {
        val q = query.trim().ifEmpty { null }
        try {
            val response = api.search(q, page)
            val items = response.organizations.map { it.toDomain() }
            AppResult.Success(
                OrganizationPage(
                    items = items,
                    page = page,
                    totalResults = response.totalResults,
                    // Last page, or a short page (the API caps results, so trust both signals).
                    endReached = items.isEmpty() || page >= response.numPages - 1
                )
            )
        } catch (e: HttpException) {
            when (e.code()) {
                // API quirk: "no matches" is a 404 and "past the last page" is a 400. Neither is an error for the user.
                HTTP_NOT_FOUND -> AppResult.Success(OrganizationPage(emptyList(), page, totalResults = 0, endReached = true))
                HTTP_BAD_REQUEST -> AppResult.Success(OrganizationPage(emptyList(), page, totalResults = 0, endReached = true))
                else -> AppResult.Failure(e)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            AppResult.Failure(e)
        }
    }

    override suspend fun detail(ein: Long): AppResult<OrganizationDetail> = withContext(io) {
        safeCall {
            val response = api.organization(ein)
            val org = response.organization
            OrganizationDetail(
                ein = org.ein,
                name = org.name,
                address = listOfNotNull(org.address, listOfNotNull(org.city, org.state).joinToString(", ").ifEmpty { null }, org.zipcode)
                    .joinToString(" ")
                    .ifBlank { null },
                nteeCode = org.nteeCode,
                filings = response.filings.map { it.toDomain() }.sortedByDescending { it.year }
            )
        }
    }

    private fun OrganizationDto.toDomain() = Organization(
        ein = ein,
        name = name,
        location = listOfNotNull(city?.takeIf { it.isNotBlank() }, state?.takeIf { it.isNotBlank() }).joinToString(", ").ifEmpty { null },
        nteeCode = nteeCode
    )

    private fun FilingDto.toDomain() = Filing(year, totalRevenue, totalExpenses, totalAssets)

    private companion object {
        const val HTTP_BAD_REQUEST = 400
        const val HTTP_NOT_FOUND = 404
    }
}
