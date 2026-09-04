package no.novari.flyt.catalog.contract.fixtures

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

object CatalogContractFixtures {
    private const val ROOT = "catalog-contract-fixtures"

    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    private val resolver = PathMatchingResourcePatternResolver(CatalogContractFixtures::class.java.classLoader)

    private val httpFixtures: Map<String, List<HttpContractFixture>> by lazy {
        load<HttpContractFixture>("http").groupBy(HttpContractFixture::domain)
    }

    private val kafkaFixtures: Map<String, List<KafkaPayloadFixture>> by lazy {
        load<KafkaPayloadFixture>("kafka").groupBy(KafkaPayloadFixture::domain)
    }

    fun http(domain: String): List<HttpContractFixture> = httpFixtures[domain] ?: throw missingDomain(domain, httpFixtures.keys)

    fun http(
        domain: String,
        surface: String,
    ): List<HttpContractFixture> {
        val forSurface = http(domain).filter { it.surface == surface }
        require(forSurface.isNotEmpty()) {
            "Ingen HTTP-fixtures for flaten '$surface' i domenet '$domain'. " +
                "Kjente flater: ${http(domain).map(HttpContractFixture::surface).distinct().sorted()}"
        }
        return forSurface
    }

    fun httpById(id: String): HttpContractFixture =
        httpFixtures.values.flatten().singleOrNull { it.id == id }
            ?: throw IllegalArgumentException("Fant ingen HTTP-fixture med id '$id'")

    fun kafka(domain: String): List<KafkaPayloadFixture> = kafkaFixtures[domain] ?: throw missingDomain(domain, kafkaFixtures.keys)

    fun kafka(
        domain: String,
        contract: String,
    ): List<KafkaPayloadFixture> {
        val forContract = kafka(domain).filter { it.contract == contract }
        require(forContract.isNotEmpty()) {
            "Ingen Kafka-fixtures for kontrakten '$contract' i domenet '$domain'. " +
                "Kjente kontrakter: ${kafka(domain).map(KafkaPayloadFixture::contract).distinct().sorted()}"
        }
        return forContract
    }

    fun kafkaById(id: String): KafkaPayloadFixture =
        kafkaFixtures.values.flatten().singleOrNull { it.id == id }
            ?: throw IllegalArgumentException("Fant ingen Kafka-fixture med id '$id'")

    private inline fun <reified T : Any> load(kind: String): List<T> {
        val resources = resolver.getResources("classpath*:$ROOT/*/$kind/*.json")
        val fixtures =
            resources.map { resource ->
                try {
                    resource.inputStream.use { objectMapper.readValue<T>(it) }
                } catch (exception: Exception) {
                    throw IllegalStateException("Klarte ikke lese fixturen ${resource.description}", exception)
                }
            }

        val duplicates =
            fixtures
                .groupingBy(::idOf)
                .eachCount()
                .filterValues { it > 1 }
                .keys
        check(duplicates.isEmpty()) { "Fixture-ID-er må være unike. Duplikater: ${duplicates.sorted()}" }

        return fixtures.sortedBy(::idOf)
    }

    private fun idOf(fixture: Any): String =
        when (fixture) {
            is HttpContractFixture -> fixture.id
            is KafkaPayloadFixture -> fixture.id
            else -> error("Ukjent fixture-type: ${fixture::class}")
        }

    private fun missingDomain(
        domain: String,
        known: Set<String>,
    ) = IllegalArgumentException("Ingen fixtures for domenet '$domain'. Kjente domener: ${known.sorted()}")
}
