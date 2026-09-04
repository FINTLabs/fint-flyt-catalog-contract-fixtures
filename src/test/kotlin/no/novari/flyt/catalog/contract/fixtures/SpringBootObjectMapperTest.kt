package no.novari.flyt.catalog.contract.fixtures

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import java.time.Instant

/**
 * Fixturene skriver tidsstempler som ISO-8601. Den formen kommer fra Spring Boot sin
 * auto-konfigurasjon, ikke fra Jackson selv, så antakelsen fastholdes her framfor å bli antatt.
 */
class SpringBootObjectMapperTest {
    private val instant = Instant.parse("2026-01-15T09:00:00Z")

    @Test
    fun `Spring Boot sin auto-konfigurerte mapper skriver Instant som ISO-8601`() {
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration::class.java))
            .run { context ->
                val autoConfigured = context.getBean<ObjectMapper>()

                assertThat(autoConfigured.writeValueAsString(instant)).isEqualTo("\"2026-01-15T09:00:00Z\"")
            }
    }

    @Test
    fun `runnerens mapper gir samme form som den auto-konfigurerte`() {
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration::class.java))
            .run { context ->
                val autoConfigured = context.getBean<ObjectMapper>()
                val runners = FixtureObjectMapper.springBoot()

                assertThat(runners.writeValueAsString(instant))
                    .isEqualTo(autoConfigured.writeValueAsString(instant))
            }
    }
}
