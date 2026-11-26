# fint-flyt-catalog-contract-fixtures

Shared contract fixtures for the FLYT catalog domains `configuration`, `integration`, `discovery` and
`value-converting`. Each fixture is one concrete case: what goes in, and exactly what comes out over HTTP or Kafka.
`fint-flyt-intgr-conf-service` runs its tests against them, so a change in the API is caught in a test and not in
production. Set up in [FFS-2258](https://novari-iks.atlassian.net/browse/FFS-2258).

Requires Spring Boot 4.2 or newer (Jackson 3) and Java 25 or newer. Version 1.x is for Spring Boot 3.5 (Jackson 2).
The architecture is described in [docs/architecture.md](docs/architecture.md).

## Usage

```kotlin
testImplementation("no.novari:flyt-catalog-contract-fixtures:<version>")
```

### HTTP

The runner sends the fixture's request to a `MockMvc` the test sets up, and compares status, content type and body.

```kotlin
@ParameterizedTest(name = "{0}")
@MethodSource("fixtures")
fun `the HTTP contract is unchanged`(fixture: HttpContractFixture) {
    stubServicesFor(fixture)
    HttpContractFixtureRunner(mockMvc, customizeRequest = { it.with(jwt()) }).verify(fixture)
}

companion object {
    @JvmStatic
    fun fixtures() = CatalogContractFixtures.http("value-converting")
}
```

Taking every fixture of a domain means a new fixture without test setup fails right away. For one fixture or one
surface, use `CatalogContractFixtures.httpById("value-converting/delete/no-content")` or
`CatalogContractFixtures.http("value-converting", "GET /api/intern/value-convertings/{id}")`.

### Kafka

Kafka contracts are checked at the serialization level: the DTO serializes to exactly the payload, and reads it back
without loss. `deserialize` and `verifySerialization` check one direction each.

```kotlin
val fixture = CatalogContractFixtures.kafkaById("discovery/event/integration-metadata-received")
KafkaPayloadFixtureRunner().deserialize<IntegrationMetadata>(fixture)
```

Use the reified form: `Long::class.java` is the primitive `long`, which Jackson cannot read into.

Both runners use `FixtureObjectMapper.springBoot()` by default, which mirrors the `JsonMapper` Spring Boot
auto-configures. `SpringBootObjectMapperTest` checks that they write `Instant` the same way.

## Format

Fixtures are in `src/main/resources/catalog-contract-fixtures/<domain>/{http,kafka}/`. The file name is free; `id` must
be unique across the whole set.

```json
{
  "id": "value-converting/get-by-id/not-found",
  "domain": "value-converting",
  "surface": "GET /api/intern/value-convertings/{id}",
  "description": "An unknown id gives 404. The message contains the id.",
  "request": { "method": "GET", "path": "/api/intern/value-convertings/123" },
  "expectedResponse": {
    "status": 404,
    "contentType": "application/problem+json",
    "body": { "type": "about:blank", "title": "Not Found", "status": 404, "detail": "..." }
  }
}
```

| Field                             | Meaning                                                                                       |
| --------------------------------- | --------------------------------------------------------------------------------------------- |
| `id`                              | Unique. Convention: `<domain>/<surface>/<outcome>`                                            |
| `surface`                         | Logical surface, with path variables in braces. Groups fixtures and names them in failures    |
| `description`                     | What the case pins down, and why, especially where behaviour is kept on purpose               |
| `request.body`                    | JSON body, serialized by the runner                                                           |
| `request.rawBody`                 | Raw string, for bodies that are not valid JSON or have fields the DTO lacks. Wins over `body` |
| `expectedResponse.body`           | Left out means an empty body, as for `204 No Content`                                         |
| `expectedResponse.bodyComparison` | `STRICT` (default), `LENIENT` or `NONE`                                                       |
| `expectedResponse.ignoredPaths`   | JSONPath expressions removed from both bodies before comparing                                |

`STRICT` makes extra fields and a changed list order a contract break, since the frontend sees them. Avoid `LENIENT`;
no fixture uses it. For a value the test cannot control, such as a timestamp made at runtime, use `ignoredPaths`.

A Kafka fixture has `contract` (the topic name without prefix), `role` (`REQUEST`, `REPLY` or `EVENT`), `payload` and
`ignoredPaths`. `"payload": null` is a valid contract: an empty payload on the topic.

## Adding fixtures

1. Write the fixture as the service answers **today**, also where that is inconsistent. Say so in `description`.
2. Run the service's contract test. It must pass without changing production code; if not, the fixture is wrong.
3. Run `./gradlew check` here, which checks that every fixture loads and is well formed.

Fixtures hold synthetic data only. The repo is public.

## Build

```bash
./gradlew check
./gradlew publishToMavenLocal
```

Published to `repo.fintlabs.no` on a GitHub release. Dependencies are locked in `gradle.lockfile`; after changing a
dependency, run `./gradlew dependencies --write-locks` and commit the lockfiles.
