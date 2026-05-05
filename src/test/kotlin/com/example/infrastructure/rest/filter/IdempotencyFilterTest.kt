package com.example.infrastructure.rest.filter

import com.example.testsupport.ApiTestFixtures
import com.example.testsupport.BaseApiTest
import com.example.testsupport.TestDataBuilder
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import java.util.UUID

@QuarkusTest
class IdempotencyFilterTest : BaseApiTest() {
    @Test
    fun `rejects Idempotency-Key on unauthenticated login`() {
        given()
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .contentType(ContentType.JSON)
            .body(TestDataBuilder.userLogin("anyone@example.com", "password123"))
            .`when`()
            .post("/api/users/login")
            .then()
            .statusCode(400)
            .body("errors.idempotencyKey[0]", equalTo("requires an authenticated request"))
    }

    @Test
    fun `rejects Idempotency-Key on unauthenticated registration`() {
        given()
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .contentType(ContentType.JSON)
            .body(TestDataBuilder.userRegistration())
            .`when`()
            .post("/api/users")
            .then()
            .statusCode(400)
            .body("errors.idempotencyKey[0]", equalTo("requires an authenticated request"))
    }

    @Test
    fun `rejects Idempotency-Key on unauthenticated refresh`() {
        given()
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .contentType(ContentType.JSON)
            .body("""{"refreshToken":"some-token"}""")
            .`when`()
            .post("/api/users/refresh")
            .then()
            .statusCode(400)
            .body("errors.idempotencyKey[0]", equalTo("requires an authenticated request"))
    }

    @Test
    fun `cross-user replay of login Idempotency-Key cannot leak tokens`() {
        // Regression for the token-replay vulnerability: an attacker who replays a
        // victim's Idempotency-Key against /api/users/login must NOT receive the
        // cached login response (which would contain the victim's JWT + refresh token).
        val victim = ApiTestFixtures.registerUser()
        val sharedKey = UUID.randomUUID().toString()

        given()
            .header("Idempotency-Key", sharedKey)
            .contentType(ContentType.JSON)
            .body(TestDataBuilder.userLogin(victim.email, victim.password))
            .`when`()
            .post("/api/users/login")
            .then()
            .statusCode(400)

        given()
            .header("Idempotency-Key", sharedKey)
            .contentType(ContentType.JSON)
            .body(TestDataBuilder.userLogin("attacker@example.com", "irrelevant"))
            .`when`()
            .post("/api/users/login")
            .then()
            .statusCode(400)
            .body("user", nullValue())
    }

    @Test
    fun `authenticated idempotent POST replays the cached response`() {
        val user = ApiTestFixtures.registerUser()
        val key = UUID.randomUUID().toString()
        val body = TestDataBuilder.articleCreation(title = "Idempotent Title ${UUID.randomUUID()}")

        val firstSlug =
            ApiTestFixtures
                .authenticatedRequest(user.token)
                .header("Idempotency-Key", key)
                .body(body)
                .`when`()
                .post("/api/articles")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getString("article.slug")

        ApiTestFixtures
            .authenticatedRequest(user.token)
            .header("Idempotency-Key", key)
            .body(body)
            .`when`()
            .post("/api/articles")
            .then()
            .statusCode(201)
            .body("article.slug", equalTo(firstSlug))
    }

    @Test
    fun `different users sharing an Idempotency-Key are isolated by scope`() {
        val alice = ApiTestFixtures.registerUser()
        val bob = ApiTestFixtures.registerUser()
        val sharedKey = UUID.randomUUID().toString()

        val aliceSlug =
            ApiTestFixtures
                .authenticatedRequest(alice.token)
                .header("Idempotency-Key", sharedKey)
                .body(TestDataBuilder.articleCreation(title = "Alice ${UUID.randomUUID()}"))
                .`when`()
                .post("/api/articles")
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getString("article.slug")

        ApiTestFixtures
            .authenticatedRequest(bob.token)
            .header("Idempotency-Key", sharedKey)
            .body(TestDataBuilder.articleCreation(title = "Bob ${UUID.randomUUID()}"))
            .`when`()
            .post("/api/articles")
            .then()
            .statusCode(201)
            .body("article.author.username", equalTo(bob.username))
            .body("article.slug", not(equalTo(aliceSlug)))
    }

    @Test
    fun `unauthenticated login without Idempotency-Key is unaffected`() {
        val user = ApiTestFixtures.registerUser()

        given()
            .contentType(ContentType.JSON)
            .body(TestDataBuilder.userLogin(user.email, user.password))
            .`when`()
            .post("/api/users/login")
            .then()
            .statusCode(200)
            .body("user.email", equalTo(user.email))
    }
}
