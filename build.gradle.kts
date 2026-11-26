import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
}

group = "no.novari"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    withSourcesJar()
}

kotlin {
    compilerOptions {
        // Spring Boot 4.2 manages Kotlin 2.4, so apps may run with a 2.4 stdlib.
        apiVersion = KotlinVersion.KOTLIN_2_4
    }
    // The stdlib version Spring Boot manages, not the Kotlin plugin's, so the library does not upgrade it in apps.
    coreLibrariesVersion =
        libs.versions.kotlin.stdlib
            .get()
}

ktlint {
    version = libs.versions.ktlint.cli
}

dependencyLocking {
    lockAllConfigurations()
}

// The Spring Boot BOM sets versions in this build only. It is not published, so it cannot change the app's Spring Boot
// or Kotlin version. versionMapping below publishes the resolved versions instead.
val springBootBom = configurations.dependencyScope("springBootBom")
listOf("compileClasspath", "runtimeClasspath", "testCompileClasspath", "testRuntimeClasspath").forEach {
    configurations.named(it) { extendsFrom(springBootBom.get()) }
}

dependencies {
    springBootBom(platform(libs.spring.boot.dependencies))

    constraints {
        ktlint(libs.logback.core) {
            because(
                "ktlint 1.8.0 brings logback-core 1.3.16, which has CVE-2026-1225, CVE-2026-9828 and " +
                    "CVE-2026-10532. Only the ktlint tool classpath, not the published library",
            )
        }
    }

    api("tools.jackson.core:jackson-databind")
    api("tools.jackson.module:jackson-module-kotlin")
    api("org.springframework:spring-test")
    api("org.springframework:spring-web")
    api("org.skyscreamer:jsonassert")
    api("com.jayway.jsonpath:json-path")
    api("jakarta.servlet:jakarta.servlet-api")
    api("org.junit.jupiter:junit-jupiter-api")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    repositories {
        maven {
            url = uri("https://repo.fintlabs.no/releases")
            credentials {
                username = System.getenv("REPOSILITE_USERNAME")
                password = System.getenv("REPOSILITE_PASSWORD")
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            // Publish the versions resolved from the Spring Boot BOM, so consumers do not need the BOM.
            versionMapping {
                allVariants {
                    fromResolutionResult()
                }
            }
        }
    }
}
