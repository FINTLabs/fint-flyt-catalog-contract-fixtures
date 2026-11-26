package no.novari.flyt.catalog.contract.fixtures

import tools.jackson.databind.json.JsonMapper

/**
 * Mirrors the JsonMapper Spring Boot auto-configures. Both the HTTP responses and the Kafka payloads are serialized by it
 * in production. Jackson 3 writes dates as ISO-8601 by default, and the Kotlin and java.time support are found as
 * modules, as Boot does.
 */
object FixtureObjectMapper {
    fun springBoot(): JsonMapper = JsonMapper.builder().findAndAddModules().build()
}
