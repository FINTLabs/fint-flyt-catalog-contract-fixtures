package no.novari.flyt.catalog.contract.fixtures

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.NullNode
import org.junit.jupiter.api.Assertions.assertNull
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

class KafkaPayloadFixtureRunner(
    private val objectMapper: ObjectMapper = FixtureObjectMapper.springBoot(),
) {
    fun <T : Any> deserialize(
        fixture: KafkaPayloadFixture,
        type: Class<T>,
    ): T? {
        if (fixture.isNullPayload()) {
            return null
        }

        return try {
            objectMapper.treeToValue(fixture.payload, type)
        } catch (exception: Exception) {
            throw AssertionError(
                describe(fixture, "payloaden kan ikke lenger deserialiseres til ${type.simpleName}"),
                exception,
            )
        }
    }

    fun verifySerialization(
        fixture: KafkaPayloadFixture,
        value: Any?,
    ) {
        if (fixture.isNullPayload()) {
            assertNull(value, describe(fixture, "kontrakten er en tom payload, men verdien er ikke null"))
            return
        }

        val expected = JsonPaths.prune(objectMapper.writeValueAsString(fixture.payload), fixture.ignoredPaths)
        val actual = JsonPaths.prune(objectMapper.writeValueAsString(value), fixture.ignoredPaths)

        try {
            JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT)
        } catch (error: AssertionError) {
            throw AssertionError(describe(fixture, "serialiseringen er endret.\n${error.message}"), error)
        }
    }

    fun <T : Any> verifyRoundTrip(
        fixture: KafkaPayloadFixture,
        type: Class<T>,
    ): T? {
        val value = deserialize(fixture, type)
        verifySerialization(fixture, value)
        return value
    }

    /**
     * `T::class.javaObjectType` framfor `T::class.java`: sistnevnte gir den primitive typen for
     * Long, Int og de andre, som Jackson ikke kan deserialisere til.
     */
    inline fun <reified T : Any> deserialize(fixture: KafkaPayloadFixture): T? = deserialize(fixture, T::class.javaObjectType)

    inline fun <reified T : Any> verifyRoundTrip(fixture: KafkaPayloadFixture): T? = verifyRoundTrip(fixture, T::class.javaObjectType)

    private fun KafkaPayloadFixture.isNullPayload(): Boolean = payload == null || payload is NullNode

    private fun describe(
        fixture: KafkaPayloadFixture,
        problem: String,
    ): String =
        "Kontraktsbrudd i fixturen '${fixture.id}' (${fixture.contract}, ${fixture.role}): $problem\n" +
            "Fixturen beskriver: ${fixture.description}"
}
