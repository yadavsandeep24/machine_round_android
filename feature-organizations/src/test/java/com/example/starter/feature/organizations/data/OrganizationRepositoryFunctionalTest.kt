package com.example.starter.feature.organizations.data

import com.example.starter.core.common.AppResult
import com.example.starter.core.network.NetworkModule
import com.example.starter.feature.organizations.data.remote.NonprofitApi
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Functional tests of the data layer: real Retrofit + kotlinx.serialization + the repository,
 * against MockWebServer serving fixtures recorded from the live API's shapes.
 */
class OrganizationRepositoryFunctionalTest {

    private val server = MockWebServer()
    private lateinit var repo: OrganizationRepositoryImpl

    @Before
    fun setUp() {
        server.start()
        val api = NetworkModule.provideRetrofitBuilder(OkHttpClient(), NetworkModule.provideJson())
            .baseUrl(server.url("/nonprofits/api/v2/"))
            .build()
            .create(NonprofitApi::class.java)
        repo = OrganizationRepositoryImpl(api, Dispatchers.Unconfined)
    }

    @After
    fun tearDown() = server.shutdown()

    private fun fixture(name: String) = checkNotNull(javaClass.classLoader).getResource("fixtures/$name")!!.readText()

    private fun enqueue(code: Int = 200, body: String = "{}") = server.enqueue(MockResponse().setResponseCode(code).setBody(body))

    @Test
    fun `parses a page and tolerates unknown fields and missing locations`() = runTest {
        enqueue(body = fixture("search_page0.json"))
        val page = (repo.search("", 0) as AppResult.Success).data
        assertThat(
            page.items.map {
                it.name
            }
        ).containsExactly("Kaiser Foundation Health Plan Inc", "Gates Foundation Trust", "No Location Fund").inOrder()
        assertThat(page.items[0].location).isEqualTo("Oakland, CA")
        assertThat(page.items[2].location).isNull()
        assertThat(page.totalResults).isEqualTo(27)
        assertThat(page.endReached).isFalse()
    }

    @Test
    fun `sends the trimmed query and 0-based page, and omits q when browsing`() = runTest {
        enqueue(body = fixture("search_page0.json"))
        enqueue(body = fixture("search_page0.json"))
        repo.search("  red cross ", 3)
        repo.search("   ", 0)
        val search = server.takeRequest().requestUrl!!
        assertThat(search.encodedPath).isEqualTo("/nonprofits/api/v2/search.json")
        assertThat(search.queryParameter("q")).isEqualTo("red cross")
        assertThat(search.queryParameter("page")).isEqualTo("3")
        assertThat(server.takeRequest().requestUrl!!.queryParameter("q")).isNull()
    }

    @Test
    fun `last page is detected from num_pages`() = runTest {
        enqueue(body = fixture("search_last_page.json"))
        assertThat((repo.search("", 1) as AppResult.Success).data.endReached).isTrue()
    }

    @Test
    fun `API quirk - 404 means no matches, not an error`() = runTest {
        enqueue(code = 404, body = """{"api_version":2}""")
        val page = (repo.search("zzqqxx", 0) as AppResult.Success).data
        assertThat(page.items).isEmpty()
        assertThat(page.endReached).isTrue()
    }

    @Test
    fun `API quirk - 400 pagination out of range means end of list`() = runTest {
        enqueue(code = 400, body = """{"error":"Pagination out of range"}""")
        assertThat((repo.search("", 400) as AppResult.Success).data.endReached).isTrue()
    }

    @Test
    fun `server errors and malformed JSON become failures`() = runTest {
        enqueue(code = 503)
        enqueue(body = "<html>maintenance</html>")
        assertThat(repo.search("", 0)).isInstanceOf(AppResult.Failure::class.java)
        assertThat(repo.search("", 0)).isInstanceOf(AppResult.Failure::class.java)
    }

    @Test
    fun `detail joins the address and sorts filings newest first`() = runTest {
        enqueue(body = fixture("organization_detail.json"))
        val detail = (repo.detail(911663695) as AppResult.Success).data
        assertThat(server.takeRequest().requestUrl!!.encodedPath).isEqualTo("/nonprofits/api/v2/organizations/911663695.json")
        assertThat(detail.address).isEqualTo("2365 CARILLON PT Kirkland, WA 98033-7353")
        assertThat(detail.filings.map { it.year }).containsExactly(2023, 2022, 2021).inOrder()
        assertThat(detail.filings[1].revenue).isNull()
    }
}
