package no.novari.flyt.catalog.contract.fixtures

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class KafkaPayloadFixtureRunnerTest {
    private val runner = KafkaPayloadFixtureRunner()

    @Test
    fun `en payload som deserialiserer og reserialiserer likt er uendret`() {
        val fixture = fixtureOf("""{"navn":"kontrakt","antall":3}""")

        val value = runner.verifyRoundTrip(fixture, Payload::class.java)

        assertThat(value).isEqualTo(Payload("kontrakt", 3))
    }

    @Test
    fun `et felt som forsvinner i deserialiseringen fanges`() {
        val fixture = fixtureOf("""{"navn":"kontrakt","antall":3,"fjernet":"verdi"}""")

        assertThatThrownBy { runner.verifyRoundTrip(fixture, Payload::class.java) }
            .isInstanceOf(AssertionError::class.java)
            .hasMessageContaining("serialiseringen er endret")
    }

    @Test
    fun `en tom payload er en gyldig kontrakt`() {
        val fixture = fixtureOf("null")

        assertThat(runner.verifyRoundTrip(fixture, Payload::class.java)).isNull()
    }

    @Test
    fun `en verdi der kontrakten krever tom payload fanges`() {
        val fixture = fixtureOf("null")

        assertThatThrownBy { runner.verifySerialization(fixture, Payload("kontrakt", 3)) }
            .isInstanceOf(AssertionError::class.java)
            .hasMessageContaining("tom payload")
    }

    @Test
    fun `reified-varianten bokser primitive typer slik Jackson krever`() {
        val fixture = fixtureOf("1")

        assertThat(runner.deserialize<Long>(fixture)).isEqualTo(1L)
    }

    @Test
    fun `reified-varianten gir samme resultat som Class-varianten`() {
        val fixture = fixtureOf("""{"navn":"kontrakt","antall":3}""")

        assertThat(runner.verifyRoundTrip<Payload>(fixture))
            .isEqualTo(runner.verifyRoundTrip(fixture, Payload::class.java))
    }

    private fun fixtureOf(payload: String) =
        KafkaPayloadFixture(
            id = "selvtest/reply/payload",
            domain = "selvtest",
            contract = "request.selvtest.by.id",
            role = PayloadRole.REPLY,
            description = "Selvtest av runneren",
            payload = FixtureObjectMapper.springBoot().readTree(payload),
        )

    data class Payload(
        val navn: String,
        val antall: Int,
    )
}
