package no.novari.flyt.catalog.contract.fixtures

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import tools.jackson.databind.JsonNode

@JsonIgnoreProperties(ignoreUnknown = true)
data class KafkaPayloadFixture(
    val id: String,
    val domain: String,
    val contract: String,
    val role: PayloadRole,
    val description: String,
    val payload: JsonNode?,
    val ignoredPaths: List<String> = emptyList(),
) {
    override fun toString(): String = id
}

enum class PayloadRole {
    REQUEST,
    REPLY,
    EVENT,
}
