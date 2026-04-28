package com.example.testsupport

import io.quarkus.test.common.QuarkusTestResource
import jakarta.inject.Inject
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach

@QuarkusTestResource(PostgresTestResource::class)
@QuarkusTestResource(VaultTestResource::class)
abstract class BaseApiTest {
    @Inject
    lateinit var dsl: DSLContext

    @BeforeEach
    fun baseSetup() {
        cleanDatabase()
    }

    private fun cleanDatabase() {
        dsl.execute(
            """
            TRUNCATE TABLE
                vault.person,
                vault.encryption_key,
                auth.password,
                public.article_tags,
                public.favorites,
                public.followers,
                public.comments,
                public.articles,
                public.tags,
                public."user"
            RESTART IDENTITY CASCADE
            """.trimIndent(),
        )
    }
}
