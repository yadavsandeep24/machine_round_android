package com.example.starter.feature.organizations.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * ProPublica Nonprofit Explorer API v2 (the endpoint given in DocuSign's candidate task).
 * Quirks verified against the live API:
 *  - `page` is 0-based, 25 results per page
 *  - a search with no matches returns HTTP 404 (not an empty list)
 *  - a page past the end returns HTTP 400 "Pagination out of range"
 */
interface NonprofitApi {
    @GET("search.json")
    suspend fun search(@Query("q") query: String?, @Query("page") page: Int): SearchResponseDto

    @GET("organizations/{ein}.json")
    suspend fun organization(@Path("ein") ein: Long): OrganizationDetailResponseDto

    companion object {
        const val BASE_URL = "https://projects.propublica.org/nonprofits/api/v2/"
    }
}

@Serializable
data class SearchResponseDto(
    @SerialName("total_results") val totalResults: Int = 0,
    @SerialName("organizations") val organizations: List<OrganizationDto> = emptyList(),
    @SerialName("num_pages") val numPages: Int = 0,
    @SerialName("cur_page") val curPage: Int = 0,
    @SerialName("per_page") val perPage: Int = 0
)

@Serializable
data class OrganizationDto(
    @SerialName("ein") val ein: Long,
    @SerialName("name") val name: String = "",
    @SerialName("city") val city: String? = null,
    @SerialName("state") val state: String? = null,
    @SerialName("ntee_code") val nteeCode: String? = null
)

@Serializable
data class OrganizationDetailResponseDto(
    @SerialName("organization") val organization: OrganizationDetailDto,
    @SerialName("filings_with_data") val filings: List<FilingDto> = emptyList()
)

@Serializable
data class OrganizationDetailDto(
    @SerialName("ein") val ein: Long,
    @SerialName("name") val name: String = "",
    @SerialName("address") val address: String? = null,
    @SerialName("city") val city: String? = null,
    @SerialName("state") val state: String? = null,
    @SerialName("zipcode") val zipcode: String? = null,
    @SerialName("ntee_code") val nteeCode: String? = null
)

@Serializable
data class FilingDto(
    @SerialName("tax_prd_yr") val year: Int,
    @SerialName("totrevenue") val totalRevenue: Long? = null,
    @SerialName("totfuncexpns") val totalExpenses: Long? = null,
    @SerialName("totassetsend") val totalAssets: Long? = null
)
