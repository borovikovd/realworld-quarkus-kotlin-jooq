package com.example.testsupport

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import org.testcontainers.postgresql.PostgreSQLContainer
import java.nio.file.Files
import java.nio.file.Paths

class PostgresTestResource : QuarkusTestResourceLifecycleManager {
    private val postgres =
        PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("realworld_test")
            .withUsername("test")
            .withPassword("test")

    override fun start(): Map<String, String> {
        postgres.start()

        applyMigrations()

        return mapOf(
            "quarkus.datasource.jdbc.url" to postgres.jdbcUrl,
            "quarkus.datasource.username" to postgres.username,
            "quarkus.datasource.password" to postgres.password,
        )
    }

    override fun stop() {
        postgres.stop()
    }

    private fun applyMigrations() {
        val migrationsDir = Paths.get("db/migrations")
        val migrations =
            Files
                .list(migrationsDir)
                .use { stream ->
                    stream
                        .filter { it.fileName.toString().endsWith(".sql") }
                        .sorted()
                        .toList()
                }

        postgres.createConnection("").use { conn ->
            conn.createStatement().use { stmt ->
                migrations.forEach { stmt.execute(Files.readString(it)) }
            }
        }
    }
}
