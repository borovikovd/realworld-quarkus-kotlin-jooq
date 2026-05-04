package com.example.infrastructure.rest.user

import com.example.testsupport.ApiTestFixtures
import com.example.testsupport.BaseApiTest
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.Test

@QuarkusTest
class RefreshTokenApiTest : BaseApiTest() {
    @Test
    fun `should issue refresh token on register`() {
        val user = ApiTestFixtures.registerUser()
        assert(user.refreshToken.isNotBlank())
    }

    @Test
    fun `should refresh access token with valid refresh token`() {
        val user = ApiTestFixtures.registerUser()

        given()
            .contentType(ContentType.JSON)
            .body("""{"refreshToken":"${user.refreshToken}"}""")
            .`when`()
            .post("/api/users/refresh")
            .then()
            .statusCode(200)
            .body("user.token", notNullValue())
            .body("user.refreshToken", notNullValue())
    }

    @Test
    fun `refresh rotates the refresh token (old one is invalidated)`() {
        val user = ApiTestFixtures.registerUser()

        given()
            .contentType(ContentType.JSON)
            .body("""{"refreshToken":"${user.refreshToken}"}""")
            .`when`()
            .post("/api/users/refresh")
            .then()
            .statusCode(200)

        // Reusing the same refresh token must now fail.
        given()
            .contentType(ContentType.JSON)
            .body("""{"refreshToken":"${user.refreshToken}"}""")
            .`when`()
            .post("/api/users/refresh")
            .then()
            .statusCode(401)
    }

    @Test
    fun `should reject unknown refresh token`() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"refreshToken":"not-a-real-token"}""")
            .`when`()
            .post("/api/users/refresh")
            .then()
            .statusCode(401)
    }

    @Test
    fun `logout revokes the refresh token`() {
        val user = ApiTestFixtures.registerUser()

        ApiTestFixtures
            .authenticatedRequest(user.token)
            .contentType(ContentType.JSON)
            .body("""{"refreshToken":"${user.refreshToken}"}""")
            .`when`()
            .post("/api/users/logout")
            .then()
            .statusCode(204)

        given()
            .contentType(ContentType.JSON)
            .body("""{"refreshToken":"${user.refreshToken}"}""")
            .`when`()
            .post("/api/users/refresh")
            .then()
            .statusCode(401)
    }

    @Test
    fun `logout revokes the access token (jti blocklist)`() {
        val user = ApiTestFixtures.registerUser()

        ApiTestFixtures
            .authenticatedRequest(user.token)
            .contentType(ContentType.JSON)
            .body("""{"refreshToken":"${user.refreshToken}"}""")
            .`when`()
            .post("/api/users/logout")
            .then()
            .statusCode(204)

        // The same access token must now be rejected by RevokedTokenFilter.
        ApiTestFixtures
            .authenticatedRequest(user.token)
            .`when`()
            .get("/api/user")
            .then()
            .statusCode(401)
    }

    @Test
    fun `erase revokes all refresh tokens for the user`() {
        val user = ApiTestFixtures.registerUser()

        ApiTestFixtures
            .authenticatedRequest(user.token)
            .`when`()
            .delete("/api/user")
            .then()
            .statusCode(204)

        given()
            .contentType(ContentType.JSON)
            .body("""{"refreshToken":"${user.refreshToken}"}""")
            .`when`()
            .post("/api/users/refresh")
            .then()
            .statusCode(401)
    }

    @Test
    fun `erase revokes the access token (jti blocklist)`() {
        val user = ApiTestFixtures.registerUser()

        ApiTestFixtures
            .authenticatedRequest(user.token)
            .`when`()
            .delete("/api/user")
            .then()
            .statusCode(204)

        ApiTestFixtures
            .authenticatedRequest(user.token)
            .`when`()
            .get("/api/user")
            .then()
            .statusCode(401)
    }
}
