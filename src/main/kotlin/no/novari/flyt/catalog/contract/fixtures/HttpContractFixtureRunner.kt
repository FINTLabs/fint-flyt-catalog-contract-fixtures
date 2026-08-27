package no.novari.flyt.catalog.contract.fixtures

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import java.nio.charset.StandardCharsets

class HttpContractFixtureRunner(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper = FixtureObjectMapper.springBoot(),
    private val customizeRequest: (MockHttpServletRequestBuilder) -> MockHttpServletRequestBuilder = { it },
) {
    fun verify(fixture: HttpContractFixture): MvcResult {
        val result = mockMvc.perform(customizeRequest(buildRequest(fixture))).andReturn()

        result.resolvedException?.let { exception ->
            if (fixture.expectedResponse.status < 400) {
                throw AssertionError(describe(fixture, "uventet unntak fra handleren"), exception)
            }
        }

        assertStatus(fixture, result)
        assertContentType(fixture, result)
        assertBody(fixture, result)

        return result
    }

    private fun buildRequest(fixture: HttpContractFixture): MockHttpServletRequestBuilder {
        val request = fixture.request
        val builder =
            MockMvcRequestBuilders.request(HttpMethod.valueOf(request.method.uppercase()), request.path)

        request.queryParameters.forEach { (name, values) ->
            builder.queryParam(name, *values.toTypedArray())
        }
        request.headers.forEach { (name, values) ->
            values.forEach { builder.header(name, it) }
        }

        val body = request.rawBody ?: request.body?.let(objectMapper::writeValueAsString)
        if (body != null) {
            builder.contentType(request.contentType ?: MediaType.APPLICATION_JSON_VALUE)
            builder.content(body)
        } else {
            request.contentType?.let(builder::contentType)
        }

        return builder
    }

    private fun assertStatus(
        fixture: HttpContractFixture,
        result: MvcResult,
    ) = assertEquals(
        fixture.expectedResponse.status,
        result.response.status,
        describe(fixture, "statuskoden er endret. Responsbody: ${bodyOf(result)}"),
    )

    private fun assertContentType(
        fixture: HttpContractFixture,
        result: MvcResult,
    ) {
        val expected = fixture.expectedResponse.contentType ?: return
        val actual = result.response.contentType

        if (actual == null || !MediaType.parseMediaType(actual).isCompatibleWith(MediaType.parseMediaType(expected))) {
            throw AssertionError(describe(fixture, "content-type er endret. Forventet $expected, fikk $actual"))
        }
    }

    private fun assertBody(
        fixture: HttpContractFixture,
        result: MvcResult,
    ) {
        val expectedResponse = fixture.expectedResponse
        if (expectedResponse.bodyComparison == BodyComparison.NONE) {
            return
        }

        val expectedBody = expectedResponse.body
        val actualBody = bodyOf(result)

        if (expectedBody == null) {
            assertEquals("", actualBody.trim(), describe(fixture, "forventet tom responsbody"))
            return
        }

        val expectedJson = JsonPaths.prune(objectMapper.writeValueAsString(expectedBody), expectedResponse.ignoredPaths)
        val actualJson = JsonPaths.prune(actualBody, expectedResponse.ignoredPaths)

        try {
            JSONAssert.assertEquals(expectedJson, actualJson, expectedResponse.bodyComparison.toCompareMode())
        } catch (error: AssertionError) {
            throw AssertionError(describe(fixture, "responsbody er endret.\n${error.message}"), error)
        }
    }

    private fun bodyOf(result: MvcResult): String = result.response.getContentAsString(StandardCharsets.UTF_8)

    private fun describe(
        fixture: HttpContractFixture,
        problem: String,
    ): String =
        "Kontraktsbrudd i fixturen '${fixture.id}' (${fixture.surface}): $problem\n" +
            "Fixturen beskriver: ${fixture.description}"

    private fun BodyComparison.toCompareMode(): JSONCompareMode =
        when (this) {
            BodyComparison.STRICT -> JSONCompareMode.STRICT
            BodyComparison.LENIENT -> JSONCompareMode.LENIENT
            BodyComparison.NONE -> error("NONE sammenlignes ikke")
        }
}
