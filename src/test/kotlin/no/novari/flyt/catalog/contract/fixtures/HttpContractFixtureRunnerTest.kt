package no.novari.flyt.catalog.contract.fixtures

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

class HttpContractFixtureRunnerTest {
    private val fixture =
        HttpContractFixture(
            id = "selvtest/echo/ok",
            domain = "selvtest",
            surface = "GET /echo",
            description = "Selvtest av runneren",
            request =
                FixtureRequest(
                    method = "GET",
                    path = "/echo",
                    queryParameters = mapOf("navn" to listOf("kontrakt")),
                ),
            expectedResponse =
                FixtureResponse(
                    status = 200,
                    contentType = MediaType.APPLICATION_JSON_VALUE,
                    body = jsonOf("""{"navn":"kontrakt","stabil":true}"""),
                ),
        )

    @Test
    fun `en uendret flate gir grønn test`() {
        val runner = HttpContractFixtureRunner(mockMvcFor(EchoController()))

        val result = runner.verify(fixture)

        assertThat(result.response.status).isEqualTo(200)
    }

    @Test
    fun `et ekstra felt i responsen fanges som kontraktsbrudd`() {
        val runner = HttpContractFixtureRunner(mockMvcFor(ExtraFieldController()))

        assertThatThrownBy { runner.verify(fixture) }
            .isInstanceOf(AssertionError::class.java)
            .hasMessageContaining("selvtest/echo/ok")
            .hasMessageContaining("responsbody er endret")
    }

    @Test
    fun `en endret statuskode fanges som kontraktsbrudd`() {
        val runner = HttpContractFixtureRunner(mockMvcFor(WrongStatusController()))

        assertThatThrownBy { runner.verify(fixture) }
            .isInstanceOf(AssertionError::class.java)
            .hasMessageContaining("statuskoden er endret")
    }

    @Test
    fun `ignorerte stier tas ut av sammenligningen`() {
        val tolerant =
            fixture.copy(
                expectedResponse =
                    fixture.expectedResponse.copy(
                        body = jsonOf("""{"navn":"kontrakt","stabil":true,"tidspunkt":"uansett"}"""),
                        ignoredPaths = listOf("$.tidspunkt"),
                    ),
            )
        val runner = HttpContractFixtureRunner(mockMvcFor(EchoController()))

        runner.verify(tolerant)
    }

    private fun mockMvcFor(controller: Any) = MockMvcBuilders.standaloneSetup(controller).build()

    private fun jsonOf(json: String) = FixtureObjectMapper.springBoot().readTree(json)

    @RestController
    @RequestMapping("/echo")
    class EchoController {
        @GetMapping
        fun echo(
            @RequestParam navn: String,
        ) = mapOf("navn" to navn, "stabil" to true)
    }

    @RestController
    @RequestMapping("/echo")
    class ExtraFieldController {
        @GetMapping
        fun echo(
            @RequestParam navn: String,
        ) = mapOf("navn" to navn, "stabil" to true, "nytt" to "felt")
    }

    @RestController
    @RequestMapping("/echo")
    class WrongStatusController {
        @GetMapping
        fun echo(
            @RequestParam navn: String,
        ): ResponseEntity<Map<String, Any>> = ResponseEntity.status(201).body(mapOf("navn" to navn, "stabil" to true))
    }
}
