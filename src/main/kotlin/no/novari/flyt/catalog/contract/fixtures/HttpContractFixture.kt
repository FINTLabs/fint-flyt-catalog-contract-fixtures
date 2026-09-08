package no.novari.flyt.catalog.contract.fixtures

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode

@JsonIgnoreProperties(ignoreUnknown = true)
data class HttpContractFixture(
    val id: String,
    val domain: String,
    val surface: String,
    val description: String,
    val request: FixtureRequest,
    val expectedResponse: FixtureResponse,
) {
    override fun toString(): String = id
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class FixtureRequest(
    val method: String,
    val path: String,
    val queryParameters: Map<String, List<String>> = emptyMap(),
    val headers: Map<String, List<String>> = emptyMap(),
    val contentType: String? = null,
    val body: JsonNode? = null,
    val rawBody: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class FixtureResponse(
    val status: Int,
    val contentType: String? = null,
    val body: JsonNode? = null,
    val bodyComparison: BodyComparison = BodyComparison.STRICT,
    val ignoredPaths: List<String> = emptyList(),
)

enum class BodyComparison {
    STRICT,
    LENIENT,
    NONE,
}
