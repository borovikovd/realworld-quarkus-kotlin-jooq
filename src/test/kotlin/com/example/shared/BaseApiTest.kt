package com.example.shared

import io.quarkus.test.common.QuarkusTestResource
import jakarta.inject.Inject
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach

@QuarkusTestResource(PostgresTestResource::class)
abstract class BaseApiTest {
    @Inject
    lateinit var dsl: DSLContext

    @BeforeEach
    fun baseSetup() {
        cleanDatabase()
    }

    private fun cleanDatabase() {
        val tables = dsl.meta().tables
            .filter { it.schema?.name == "public" }
            .map { it.name }
            .joinToString(",\n                ")

        dsl.execute(
            """
            TRUNCATE TABLE
                $tables
            RESTART IDENTITY CASCADE
            """.trimIndent(),
        )
    }
}
