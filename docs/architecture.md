# Architecture

C4 level 3 for flyt-catalog-contract-fixtures. The library has no system context or containers of its own: it runs in
the contract tests of the service that depends on it, `fint-flyt-intgr-conf-service`. Blue is the library, turquoise is
the test and Spring's `MockMvc`.

## C4 level 3: Components

```mermaid
C4Component
    title flyt-catalog-contract-fixtures in a contract test

    Component(fixtures, "CatalogContractFixtures", "Kotlin", "Loads, groups, looks up")
    Component(json, "Fixture files", "JSON", "HTTP and Kafka cases per domain")
    Component_Ext(test, "Contract test", "JUnit", "Stubs services, picks fixtures")
    Component(http, "HTTP runner", "Kotlin", "Request, status, body")
    Component(kafka, "Kafka runner", "Kotlin", "Payload round trip")
    Component_Ext(mockmvc, "MockMvc", "Spring Test", "The service's controllers")

    Rel(test, fixtures, "Gets fixtures")
    Rel(fixtures, json, "Reads")
    Rel(test, http, "Verifies")
    Rel(test, kafka, "Verifies")
    Rel(http, mockmvc, "Performs")

    UpdateRelStyle(test, fixtures, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="10")
    UpdateRelStyle(fixtures, json, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="-35", $offsetY="-30")
    UpdateRelStyle(test, http, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="-35", $offsetY="-30")
    UpdateRelStyle(test, kafka, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="10")
    UpdateRelStyle(http, mockmvc, $textColor="#3B82F6", $lineColor="#3B82F6", $offsetX="10")
    UpdateElementStyle(fixtures, $bgColor="#93C5FD", $fontColor="#1F2937", $borderColor="#3B82F6")
    UpdateElementStyle(json, $bgColor="#93C5FD", $fontColor="#1F2937", $borderColor="#3B82F6")
    UpdateElementStyle(http, $bgColor="#93C5FD", $fontColor="#1F2937", $borderColor="#3B82F6")
    UpdateElementStyle(kafka, $bgColor="#93C5FD", $fontColor="#1F2937", $borderColor="#3B82F6")
    UpdateElementStyle(test, $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    UpdateElementStyle(mockmvc, $bgColor="#A5F3FC", $fontColor="#1F2937", $borderColor="#06B6D4")
    UpdateLayoutConfig($c4ShapeInRow="2", $c4BoundaryInRow="1")
```

- **Fixture files** are in `src/main/resources/catalog-contract-fixtures/<domain>/{http,kafka}/` and ship in the jar.
- **`CatalogContractFixtures`** finds them on the classpath, checks that every id is unique, and returns them sorted by
  id, per domain, surface or contract.
- **`HttpContractFixtureRunner`** builds the request, sends it to the test's `MockMvc` and compares status, content type
  and body with JSONAssert. `ignoredPaths` are removed from both bodies first.
- **`KafkaPayloadFixtureRunner`** reads a payload into the type the test names and writes the value back, and compares
  the JSON strictly. Kafka itself is not involved.
- Both runners use **`FixtureObjectMapper`**, which builds the same `JsonMapper` as Spring Boot's auto-configuration,
  so dates and nulls are written as in production.
