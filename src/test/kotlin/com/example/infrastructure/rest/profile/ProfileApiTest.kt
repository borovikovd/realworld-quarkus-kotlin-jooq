package com.example.infrastructure.rest.profile

import com.example.testsupport.ApiTestFixtures
import com.example.testsupport.BaseApiTest
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

@QuarkusTest
class ProfileApiTest : BaseApiTest() {
    @Test
    fun `should get profile of existing user`() {
        val user = ApiTestFixtures.registerUser()

        given()
            .`when`()
            .get("/api/profiles/${user.username}")
            .then()
            .statusCode(200)
            .body("profile.username", equalTo(user.username))
            .body("profile.following", equalTo(false))
    }

    @Test
    fun `should return 404 for non-existent profile`() {
        given()
            .`when`()
            .get("/api/profiles/nobody-here")
            .then()
            .statusCode(404)
            .body("errors.body[0]", equalTo("Profile not found"))
    }

    @Test
    fun `should follow user and reflect following status`() {
        val follower = ApiTestFixtures.registerUser()
        val target = ApiTestFixtures.registerUser()

        ApiTestFixtures.authenticatedRequest(follower.token)
            .`when`()
            .post("/api/profiles/${target.username}/follow")
            .then()
            .statusCode(200)
            .body("profile.username", equalTo(target.username))
            .body("profile.following", equalTo(true))
    }

    @Test
    fun `should show following true when authenticated viewer follows`() {
        val follower = ApiTestFixtures.registerUser()
        val target = ApiTestFixtures.registerUser()

        ApiTestFixtures.authenticatedRequest(follower.token)
            .post("/api/profiles/${target.username}/follow")

        ApiTestFixtures.authenticatedRequest(follower.token)
            .`when`()
            .get("/api/profiles/${target.username}")
            .then()
            .statusCode(200)
            .body("profile.following", equalTo(true))
    }

    @Test
    fun `should unfollow user and reflect following status`() {
        val follower = ApiTestFixtures.registerUser()
        val target = ApiTestFixtures.registerUser()

        ApiTestFixtures.authenticatedRequest(follower.token)
            .post("/api/profiles/${target.username}/follow")

        ApiTestFixtures.authenticatedRequest(follower.token)
            .`when`()
            .delete("/api/profiles/${target.username}/follow")
            .then()
            .statusCode(200)
            .body("profile.username", equalTo(target.username))
            .body("profile.following", equalTo(false))
    }

    @Test
    fun `should show following false after unfollow`() {
        val follower = ApiTestFixtures.registerUser()
        val target = ApiTestFixtures.registerUser()

        ApiTestFixtures.authenticatedRequest(follower.token)
            .post("/api/profiles/${target.username}/follow")
        ApiTestFixtures.authenticatedRequest(follower.token)
            .delete("/api/profiles/${target.username}/follow")

        ApiTestFixtures.authenticatedRequest(follower.token)
            .`when`()
            .get("/api/profiles/${target.username}")
            .then()
            .statusCode(200)
            .body("profile.following", equalTo(false))
    }

    @Test
    fun `should return 422 when following yourself`() {
        val user = ApiTestFixtures.registerUser()

        ApiTestFixtures.authenticatedRequest(user.token)
            .`when`()
            .post("/api/profiles/${user.username}/follow")
            .then()
            .statusCode(422)
            .body("errors.username[0]", equalTo("cannot follow yourself"))
    }

    @Test
    fun `should return 401 when following without auth`() {
        val target = ApiTestFixtures.registerUser()

        given()
            .`when`()
            .post("/api/profiles/${target.username}/follow")
            .then()
            .statusCode(401)
    }

    @Test
    fun `should return 404 when following non-existent user`() {
        val user = ApiTestFixtures.registerUser()

        ApiTestFixtures.authenticatedRequest(user.token)
            .`when`()
            .post("/api/profiles/nobody-here/follow")
            .then()
            .statusCode(404)
            .body("errors.body[0]", equalTo("User not found"))
    }
}
