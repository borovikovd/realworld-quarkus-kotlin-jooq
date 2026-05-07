package com.example.testsupport

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import org.testcontainers.postgresql.PostgreSQLContainer
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.Connection

class PostgresTestResource : QuarkusTestResourceLifecycleManager {
    // withReuse(true) only engages when the dev opts in via ~/.testcontainers.properties
    // (testcontainers.reuse.enable=true); CI sees fresh containers per run.
    private val postgres =
        PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("realworld_test")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true)

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
        postgres.createConnection("").use { conn ->
            if (alreadyMigrated(conn)) return
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
            conn.createStatement().use { stmt ->
                migrations.forEach { stmt.execute(Files.readString(it)) }
            }
        }
    }

    // Sentinel: if the canonical app table is already present, the schema was applied by
    // a prior run that left a reused container behind. Skip re-apply (would fail on plain
    // CREATE TABLE). On schema changes, the dev removes the container manually.
    private fun alreadyMigrated(conn: Connection): Boolean =
        conn
            .createStatement()
            .use { stmt ->
                stmt.executeQuery("SELECT to_regclass('public.\"user\"') IS NOT NULL").use { rs ->
                    rs.next() && rs.getBoolean(1)
                }
            }
}
