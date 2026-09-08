package no.novari.flyt.catalog.contract.fixtures

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class CatalogContractFixturesTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("httpDomains")
    fun `http-fixturene i domenet er velformede`(domain: String) {
        val fixtures = CatalogContractFixtures.http(domain)

        assertThat(fixtures).isNotEmpty
        assertThat(fixtures).allSatisfy { fixture ->
            assertThat(fixture.id).startsWith("$domain/")
            assertThat(fixture.description).isNotBlank
            assertThat(fixture.surface).isNotBlank
            assertThat(fixture.request.method).isNotBlank
            assertThat(fixture.request.path).startsWith("/api/intern/")
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("httpDomains")
    fun `fixture-id-ene i domenet er sortert slik at testrapporter er stabile`(domain: String) {
        assertThat(CatalogContractFixtures.http(domain).map(HttpContractFixture::id)).isSorted
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("kafkaDomains")
    fun `kafka-fixturene i domenet er velformede`(domain: String) {
        val fixtures = CatalogContractFixtures.kafka(domain)

        assertThat(fixtures).isNotEmpty
        assertThat(fixtures).allSatisfy { fixture ->
            assertThat(fixture.id).startsWith("$domain/")
            assertThat(fixture.description).isNotBlank
            assertThat(fixture.contract).matches("(request|event)\\..+")
        }
    }

    @Test
    fun `oppslag på flate gir bare fixturene for den flaten`() {
        val fixtures = CatalogContractFixtures.http("value-converting", "DELETE /api/intern/value-convertings/{id}")

        assertThat(fixtures).isNotEmpty
        assertThat(fixtures).allSatisfy { assertThat(it.request.method).isEqualTo("DELETE") }
    }

    @Test
    fun `ukjent domene nevner de kjente domenene`() {
        assertThatThrownBy { CatalogContractFixtures.http("finnes-ikke") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("value-converting")
    }

    @Test
    fun `oppslag på kontrakt gir bare fixturene for den kontrakten`() {
        val fixtures =
            CatalogContractFixtures.kafka(
                "value-converting",
                "request.value-converting.by.value-converting-id",
            )

        assertThat(fixtures.map(KafkaPayloadFixture::role))
            .containsExactlyInAnyOrder(PayloadRole.REQUEST, PayloadRole.REPLY, PayloadRole.REPLY)
    }

    @Test
    fun `ukjent kontrakt nevner de kjente kontraktene`() {
        assertThatThrownBy { CatalogContractFixtures.kafka("value-converting", "request.finnes.ikke") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("request.value-converting.by.value-converting-id")
    }

    @Test
    fun `oppslag på kafka-id gir riktig fixture`() {
        val fixture = CatalogContractFixtures.kafkaById("value-converting/request/value-converting-by-id")

        assertThat(fixture.role).isEqualTo(PayloadRole.REQUEST)
        assertThat(fixture.payload?.asLong()).isEqualTo(1L)
    }

    @Test
    fun `oppslag på id gir riktig fixture`() {
        val fixture = CatalogContractFixtures.httpById("value-converting/delete/no-content")

        assertThat(fixture.expectedResponse.status).isEqualTo(204)
        assertThat(fixture.expectedResponse.body).isNull()
    }

    companion object {
        @JvmStatic
        fun httpDomains(): Set<String> = CatalogContractFixtures.httpDomains()

        @JvmStatic
        fun kafkaDomains(): Set<String> = CatalogContractFixtures.kafkaDomains()
    }
}
