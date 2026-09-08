package no.novari.flyt.catalog.contract.fixtures

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder

/**
 * Speiler ObjectMapper-en Spring Boot auto-konfigurerer. Både HTTP-responsene og Kafka-payloadene
 * serialiseres av den i drift - `ConsumerFactoryService` og `ProducerFactory` i fint-kafka får
 * Spring-kontekstens mapper injisert.
 *
 * `WRITE_DATES_AS_TIMESTAMPS` slås av eksplisitt: det er Boot sin auto-konfigurasjon som gjør det,
 * ikke `Jackson2ObjectMapperBuilder` alene. Uten dette blir Instant serialisert som
 * epoch-desimaltall her og ISO-8601 i drift, og fixturene ville fastholdt feil form.
 */
object FixtureObjectMapper {
    fun springBoot(): ObjectMapper =
        Jackson2ObjectMapperBuilder
            .json()
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build()
}
