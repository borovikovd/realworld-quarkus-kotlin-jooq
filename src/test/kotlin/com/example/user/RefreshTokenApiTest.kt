package com.example.user

import com.example.testsupport.ApiTestFixtures
import com.example.testsupport.BaseApiTest
import com.example.testsupport.TestDataBuilder
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

    @Test
    fun `password change revokes all prior sessions`() {
        val user = ApiTestFixtures.registerUser()

        ApiTestFixtures
            .authenticatedRequest(user.token)
            .body(TestDataBuilder.userUpdate(password = "new-password-456"))
            .`when`()
            .put("/api/user")
            .then()
            .statusCode(200)

        // The old refresh token chain is dead.
        given()
            .contentType(ContentType.JSON)
            .body("""{"refreshToken":"${user.refreshToken}"}""")
            .`when`()
            .post("/api/users/refresh")
            .then()
            .statusCode(401)

        // The old access token's jti is blocklisted.
        ApiTestFixtures
            .authenticatedRequest(user.token)
            .`when`()
            .get("/api/user")
            .then()
            .statusCode(401)
    }

    @Test
    fun `email change revokes all prior sessions`() {
        val user = ApiTestFixtures.registerUser()

        ApiTestFixtures
            .authenticatedRequest(user.token)
            .body(TestDataBuilder.userUpdate(email = TestDataBuilder.uniqueEmail()))
            .`when`()
            .put("/api/user")
            .then()
            .statusCode(200)

        given()
            .contentType(ContentType.JSON)
            .body("""{"refreshToken":"${user.refreshToken}"}""")
            .`when`()
            .post("/api/users/refresh")
            .then()
            .statusCode(401)

        ApiTestFixtures
            .authenticatedRequest(user.token)
            .`when`()
            .get("/api/user")
            .then()
            .statusCode(401)
    }

    @Test
    fun `logout does not revoke another user's refresh token`() {
        val alice = ApiTestFixtures.registerUser()
        val bob = ApiTestFixtures.registerUser()

        // Bob, authenticated, submits Alice's refresh token in the logout body.
        ApiTestFixtures
            .authenticatedRequest(bob.token)
            .contentType(ContentType.JSON)
            .body("""{"refreshToken":"${alice.refreshToken}"}""")
            .`when`()
            .post("/api/users/logout")
            .then()
            .statusCode(204)

        // Alice's refresh token must still work.
        given()
            .contentType(ContentType.JSON)
            .body("""{"refreshToken":"${alice.refreshToken}"}""")
            .`when`()
            .post("/api/users/refresh")
            .then()
            .statusCode(200)
    }
}
