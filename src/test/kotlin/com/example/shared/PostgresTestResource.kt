package com.example.shared

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import org.testcontainers.containers.PostgreSQLContainer
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
        val migrationFile = Paths.get("db/migrations/20251013181033_initial.sql")
        val sql = Files.readString(migrationFile)

        postgres.createConnection("").use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute(sql)
            }
        }
    }
}
