// ============================================
// Plugins
// ============================================
plugins {
    kotlin("jvm") version "2.4.0"
    kotlin("plugin.allopen") version "2.4.0"
    id("io.quarkus")
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    id("nu.studer.jooq") version "10.2.1"
    id("com.github.spotbugs") version "6.5.8"
    // Pinned to 12.1.9 — 12.2.1 introduced a ConcurrentModificationException regression
    // when iterating configurations that interacts badly with Quarkus's dynamic config
    // registration. Track fix at https://github.com/dependency-check/dependency-check-gradle/issues/500
    id("org.owasp.dependencycheck") version "12.2.2"
    id("org.cyclonedx.bom") version "3.2.4"
}

// ============================================
// Project Properties
// ============================================
group = "com.example"
version = "1.0.0"

// ============================================
// Repositories
// ============================================
repositories {
    mavenCentral()
}

// ============================================
// Dependencies
// ============================================
dependencies {
    // Quarkus BOM
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:3.37.2"))

    // Quarkus extensions (versions managed by BOM)
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-kotlin")
    implementation("io.quarkus:quarkus-smallrye-openapi")
    implementation("io.quarkus:quarkus-swagger-ui")
    implementation("io.quarkus:quarkus-smallrye-jwt")
    implementation("io.quarkus:quarkus-smallrye-jwt-build")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-hibernate-validator")

    // jOOQ integration
    implementation("io.quarkiverse.jooq:quarkus-jooq:2.1.0")

    // Observability
    implementation("io.quarkus:quarkus-logging-json")
    implementation("io.quarkus:quarkus-micrometer-registry-prometheus")
    implementation("io.quarkus:quarkus-smallrye-health")
    implementation("io.quarkus:quarkus-opentelemetry")

    // Scheduled jobs (refresh token cleanup)
    implementation("io.quarkus:quarkus-scheduler")

    // Container image (native build via Quarkus tools)
    implementation("io.quarkus:quarkus-container-image-docker")

    // External dependencies
    implementation("org.bouncycastle:bcprov-jdk18on:1.84")
    // jOOQ code generation
    jooqGenerator("org.jooq:jooq-meta-extensions:3.21.6")

    // SpotBugs + FindSecBugs (bytecode security analysis)
    spotbugsPlugins("com.h3xstream.findsecbugs:findsecbugs-plugin:1.14.0")

    // Test dependencies
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("org.testcontainers:testcontainers-postgresql:2.0.5")
    testImplementation("org.testcontainers:testcontainers:2.0.5")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

// ============================================
// Java/Kotlin Configuration
// ============================================
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
        javaParameters = true
        allWarningsAsErrors = true
    }
}

// Kotlin all-open plugin for Quarkus
allOpen {
    annotation("jakarta.ws.rs.Path")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("jakarta.persistence.Entity")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

// ============================================
// Plugin Configurations
// ============================================

// jOOQ code generation
jooq {
    version = "3.20.8"
    configurations {
        create("main") {
            generateSchemaSourceOnCompilation = false
            jooqConfiguration.apply {
                generator.apply {
                    name = "org.jooq.codegen.KotlinGenerator"
                    database.apply {
                        name = "org.jooq.meta.extensions.ddl.DDLDatabase"
                        properties.add(
                            org.jooq.meta.jaxb.Property().apply {
                                key = "scripts"
                                value = "db/migrations/*.sql"
                            },
                        )
                        properties.add(
                            org.jooq.meta.jaxb.Property().apply {
                                key = "sort"
                                value = "semantic"
                            },
                        )
                        properties.add(
                            org.jooq.meta.jaxb.Property().apply {
                                key = "defaultNameCase"
                                value = "lower"
                            },
                        )
                    }
                    generate.apply {
                        isDeprecated = false
                        isRecords = true
                        isImmutablePojos = true
                        isFluentSetters = true
                        isImplicitJoinPathsToMany = false
                    }
                    target.apply {
                        packageName = "com.example.jooq"
                        directory = layout.buildDirectory.dir("generated/jooq").get().asFile.path
                    }
                }
            }
        }
    }
}

// ktlint (code formatting)
ktlint {
    version = "1.5.0"
    android = false
    ignoreFailures = false
}

// Detekt (static analysis - complexity, code smells, potential bugs)
detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$rootDir/detekt.yml")
    parallel = true
    source.setFrom(
        "src/main/kotlin",
    )
}

// OWASP Dependency-Check (SCA)
dependencyCheck {
    formats = listOf("HTML", "SARIF")
    outputDirectory.set(layout.buildDirectory.dir("reports/dependency-check"))
    suppressionFile = "gradle/dependency-check-suppressions.xml"
    nvd.apiKey = providers.environmentVariable("NVD_API_KEY").orNull
    analyzers.assemblyEnabled = false
    analyzers.nodeEnabled = false
    analyzers.retirejs.enabled = false
    analyzers.kev.enabled = false
    // Workaround for ConcurrentModificationException (issue #500): Kotlin scripting plugin
    // registers configurations dynamically while the plugin iterates all configurations.
    // Pinning to runtimeClasspath avoids the race and covers all transitive runtime deps.
    scanConfigurations = listOf("runtimeClasspath")
}

// CycloneDX SBOM
tasks.withType<org.cyclonedx.gradle.CyclonedxDirectTask> {
    includeConfigs.set(listOf("runtimeClasspath"))
    schemaVersion.set(org.cyclonedx.Version.VERSION_15)
    xmlOutput.set(layout.buildDirectory.file("reports/sbom/sbom.xml"))
    jsonOutput.set(layout.buildDirectory.file("reports/sbom/sbom.json"))
}

// SpotBugs + FindSecBugs (bytecode security analysis)
spotbugs {
    ignoreFailures = false
    effort = com.github.spotbugs.snom.Effort.MAX
    reportLevel = com.github.spotbugs.snom.Confidence.MEDIUM
    excludeFilter = file("gradle/spotbugs-exclude.xml")
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    reports.create("sarif") { required = true }
    reports.create("html") { required = true }
}

// SpotBugs Gradle plugin only scans Java classes by default — override for Kotlin
tasks.named<com.github.spotbugs.snom.SpotBugsTask>("spotbugsMain") {
    dependsOn("compileKotlin", "compileJava")
    classDirs = files(layout.buildDirectory.dir("classes/kotlin/main"))
    auxClassPaths.from(files(layout.buildDirectory.dir("classes/java/main")))
}

// Disable SpotBugs on test code (too many Kotlin/MockK false positives)
tasks.named<com.github.spotbugs.snom.SpotBugsTask>("spotbugsTest") {
    enabled = false
}

// ============================================
// Task Configuration
// ============================================

// Quarkus test logging configuration (required for @QuarkusTest)
tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}

// Ensure jOOQ code generation runs before compilation
// jOOQ plugin auto-wiring is disabled (generateSchemaSourceOnCompilation = false)
tasks.named("compileKotlin") {
    dependsOn("generateJooq")
}

// Ensure ktlint only checks source code, not generated code
tasks.withType<org.jlleitschuh.gradle.ktlint.tasks.BaseKtLintCheckTask> {
    mustRunAfter("generateJooq")
    setSource(fileTree("src/main/kotlin"))
}

