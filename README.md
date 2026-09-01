# fint-flyt-catalog-contract-fixtures

Delte kontraktsfixtures for katalogdomenet i FLYT: `configuration`, `integration`, `discovery` og
`value-converting`.

Et sett med request/response-par for HTTP og payloads for Kafka, som både de fire eksisterende
tjenestene og `fint-flyt-integration-configuration-service` kjører sine tester mot. Samme tilfeller,
samme forventede utfall — slik at et avvik mellom gammel og ny tjeneste fanges i test framfor i
drift.

Etablert i [FFS-2258](https://novari-iks.atlassian.net/browse/FFS-2258), under epicen
[FFS-1948](https://novari-iks.atlassian.net/browse/FFS-1948).

## Hvorfor

Sammenslåingen av de fire katalogtjenestene skal være kontraktsnøytral: HTTP-flaten og
request/reply-kontraktene skal svare uendret. «Uendret API» er lett å si og vanskelig å garantere
manuelt over fire URL-flater, åtte request/reply-kontrakter og et tresifret antall felter.

Fixturene gjør sammenligningen maskinell. En fixture beskriver ett konkret tilfelle — hva som sendes
inn, og nøyaktig hva som skal komme ut. Begge tjenester kjører den samme fixturen mot sin egen
implementasjon. Hvis svarene divergerer, feiler testen.

## Bruk

```kotlin
testImplementation("no.novari:flyt-catalog-contract-fixtures:<versjon>")
```

Fixturene er data; runnerne kjører dem mot en `MockMvc` som testen din leverer. Hvordan det
`MockMvc`-objektet er satt opp — standalone med stubbet tjenestelag, eller en full app mot
Testcontainers — er testens eget valg. Fixturen bryr seg bare om det som går over tråden.

### HTTP

```kotlin
@ParameterizedTest(name = "{0}")
@MethodSource("fixtures")
fun `HTTP-kontrakten er uendret`(fixture: HttpContractFixture) {
    stubTjenestelagetFor(fixture)

    HttpContractFixtureRunner(mockMvc, customizeRequest = { it.principal(authentication) })
        .verify(fixture)
}

companion object {
    @JvmStatic
    fun fixtures() = CatalogContractFixtures.http("value-converting")
}
```

Fordi kilden er *hele* settet for domenet, kjøres alle fixturene. En fixture som mangler oppsett
feiler umiddelbart — det er dekningsgarantien, og den er strukturell framfor å hvile på at noen
husket å skrive testen.

Trenger du bare én, eller én flate:

```kotlin
CatalogContractFixtures.httpById("value-converting/delete/no-content")
CatalogContractFixtures.http("value-converting", "GET /api/intern/value-convertings/{id}")
```

### Kafka

Kafka-kontraktene fastholdes på serialiseringsnivå: at DTO-en serialiserer til nøyaktig den
payloaden, og deserialiserer tilbake uten tap.

```kotlin
val fixture = CatalogContractFixtures.kafkaById("value-converting/reply/value-converting-by-id")

KafkaPayloadFixtureRunner().verifyRoundTrip<ValueConversion>(fixture)
```

Foretrekk den reifiserte formen over `verifyRoundTrip(fixture, ValueConversion::class.java)`. Begge
finnes — `Class`-varianten trengs når typen først er kjent ved kjøring — men `Long::class.java` gir
den primitive `long`, som Jackson ikke kan deserialisere til. Den reifiserte formen velger
`javaObjectType` for deg.

Runneren bruker som default `FixtureObjectMapper.springBoot()`, som speiler ObjectMapper-en Spring
Boot auto-konfigurerer: serialiseringen i drift går gjennom Spring-kontekstens mapper —
`ConsumerFactoryService` og `ProducerFactory` i `fint-kafka` får den injisert. Datoformatet i
payloadene er derfor Spring Boot sitt, ikke Jacksons default.

Typen oppgis av kallstedet, ikke av fixturen. Det er med vilje: den nye tjenesten har andre
klassenavn og pakker enn den gamle, og fixturen skal overleve begge.

## Format

Fixturene ligger under `src/main/resources/catalog-contract-fixtures/<domene>/{http,kafka}/`.
Filnavnet er fritt; `id` er identiteten, og den må være unik på tvers av hele settet.

### HTTP

```json
{
  "id": "value-converting/get-by-id/not-found",
  "domain": "value-converting",
  "surface": "GET /api/intern/value-convertings/{id}",
  "description": "Ukjent id gir 404. Feilmeldingen inneholder id-en.",
  "request": {
    "method": "GET",
    "path": "/api/intern/value-convertings/123",
    "queryParameters": { "size": ["10"] },
    "headers": {},
    "contentType": "application/json",
    "body": { "felt": "verdi" },
    "rawBody": null
  },
  "expectedResponse": {
    "status": 404,
    "contentType": "application/problem+json",
    "body": { "type": "about:blank", "title": "Not Found", "status": 404, "detail": "..." },
    "bodyComparison": "STRICT",
    "ignoredPaths": []
  }
}
```

| Felt                              | Betydning                                                                                                           |
|-----------------------------------|---------------------------------------------------------------------------------------------------------------------|
| `id`                              | Unik identitet. Konvensjon: `<domene>/<flate>/<utfall>`                                                             |
| `surface`                         | Logisk flate, med path-variabler i klammer. Grupperer fixturer og gir lesbare feilmeldinger                         |
| `description`                     | Hva tilfellet fastholder, og hvorfor det ser slik ut. Særlig viktig der atferden er bevisst bevart                  |
| `request.body`                    | JSON-body. Serialiseres av runneren                                                                                 |
| `request.rawBody`                 | Rå streng, for tilfeller der bodyen ikke er gyldig JSON eller inneholder felter DTO-en ikke har. Vinner over `body` |
| `expectedResponse.bodyComparison` | `STRICT` (default), `LENIENT` eller `NONE`                                                                          |
| `expectedResponse.ignoredPaths`   | JSONPath-uttrykk som fjernes fra både forventet og faktisk body før sammenligning                                   |

`STRICT` betyr at ekstra felter i responsen er et kontraktsbrudd, og at rekkefølgen i lister er
bindende. Det er default fordi et nytt felt i den nye tjenesten *er* en endring frontend kan se.

**Unngå `LENIENT`.** Den betyr «ignorer alt jeg ikke nevnte», og skjuler dermed nøyaktig det
fixturene finnes for å fange: felter som forsvinner, endrer form eller kommer til. Fristelsen er å
bruke den når tilfellet «handler om noe annet enn responsformen» — men også da er responsen en
kontrakt, og et tilfelle som ikke sier hva den skal være, dekker mindre enn det ser ut til. Ingen
fixture i settet bruker `LENIENT` i dag.

Trenger du å slippe unna en enkeltverdi, bruk `ignoredPaths` framfor `LENIENT`: den er presis om hva
som ikke sjekkes, mens resten av bodyen fortsatt holdes STRICT. Det er for verdier testen ikke kan
kontrollere, typisk tidsstempler generert ved kjøring. Bruk det sparsomt — en ignorert sti er et hull
i kontraktsdekningen, og der testen kan kontrollere verdien, skal den heller gjøre det.

Utelates `expectedResponse.body`, kreves tom responsbody — det er slik `204 No Content` uttrykkes.

### Kafka

```json
{
  "id": "value-converting/reply/value-converting-by-id",
  "domain": "value-converting",
  "contract": "request.value-converting.by.value-converting-id",
  "role": "REPLY",
  "description": "Svaret er JPA-entiteten serialisert direkte, ikke en DTO.",
  "payload": { "displayName": "Display name" },
  "ignoredPaths": []
}
```

`role` er `REQUEST`, `REPLY` eller `EVENT`. `contract` er topic-navnet uten prefiks — det samme
navnet som `RequestTopicNameParameters` eller `EventTopicNameParameters` bygger opp.

`"payload": null` er en gyldig kontrakt, og betyr at det faktisk legges en tom payload på topicen.
Det er tilfellet for oppslag som ikke finner noen rad, og klientene må tåle det.

## Legge til fixtures

1. Skriv fixturen slik tjenesten **faktisk** svarer i dag — ikke slik den burde svart. Formålet er å
   fastholde dagens atferd, også der den er inkonsistent. Der atferden er bevisst bevart, si det i
   `description`.
2. Kjør testen i den gamle tjenesten. Den skal bli grønn uten at produksjonskode endres. Blir den
   ikke det, er det fixturen som er feil.
3. `./gradlew check` her, som verifiserer at fixturen laster og er velformet.
4. Speil testen i `fint-flyt-integration-configuration-service` når domenet flyttes.

Ingen fixture skal inneholde data fra en ekte tenant. Repoet er offentlig, og fixturene er
syntetiske.

## Bygg

```bash
./gradlew check
```

Publiseres til `repo.fintlabs.no` ved GitHub release. Under utvikling, mot en konsument som har
`mavenLocal()` i `repositories`:

```bash
./gradlew publishToMavenLocal
```
