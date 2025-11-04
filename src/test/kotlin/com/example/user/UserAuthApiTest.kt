package com.example.user

import com.example.shared.ApiTestFixtures
import com.example.shared.BaseApiTest
import com.example.shared.TestDataBuilder
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.Test

@QuarkusTest
class UserAuthApiTest : BaseApiTest() {
    @Test
    fun `should register new user`() {
        val email = TestDataBuilder.uniqueEmail()
        val username = TestDataBuilder.uniqueUsername()

        given()
            .contentType(ContentType.JSON)
            .body(TestDataBuilder.userRegistration(email = email, username = username))
            .`when`()
            .post("/api/users")
            .then()
            .statusCode(201)
            .body("user.email", equalTo(email))
            .body("user.username", equalTo(username))
            .body("user.token", notNullValue())
            .body("user.bio", equalTo(null))
            .body("user.image", equalTo(null))
    }

    @Test
    fun `should not register user with duplicate email`() {
        val email = TestDataBuilder.uniqueEmail()
        ApiTestFixtures.registerUser(email = email)

        given()
            .contentType(ContentType.JSON)
            .body(TestDataBuilder.userRegistration(email = email))
            .`when`()
            .post("/api/users")
            .then()
            .statusCode(422)
            .body("errors.email[0]", equalTo("is already taken"))
    }

    @Test
    fun `should not register user with duplicate username`() {
        val username = TestDataBuilder.uniqueUsername()
        ApiTestFixtures.registerUser(username = username)

        given()
            .contentType(ContentType.JSON)
            .body(TestDataBuilder.userRegistration(username = username))
            .`when`()
            .post("/api/users")
            .then()
            .statusCode(422)
            .body("errors.username[0]", equalTo("is already taken"))
    }

    @Test
    fun `should not register user with short password`() {
        given()
            .contentType(ContentType.JSON)
            .body(TestDataBuilder.userRegistration(password = "short"))
            .`when`()
            .post("/api/users")
            .then()
            .statusCode(422)
            .body("errors.password[0]", equalTo("must be at least 8 characters"))
    }

    @Test
    fun `should login with valid credentials`() {
        val user = ApiTestFixtures.registerUser()

        given()
            .contentType(ContentType.JSON)
            .body(TestDataBuilder.userLogin(user.email, user.password))
            .`when`()
            .post("/api/users/login")
            .then()
            .statusCode(200)
            .body("user.email", equalTo(user.email))
            .body("user.username", equalTo(user.username))
            .body("user.token", notNullValue())
    }

    @Test
    fun `should not login with invalid email`() {
        ApiTestFixtures.registerUser()

        given()
            .contentType(ContentType.JSON)
            .body(TestDataBuilder.userLogin("wrong@example.com", "password123"))
            .`when`()
            .post("/api/users/login")
            .then()
            .statusCode(401)
    }

    @Test
    fun `should not login with invalid password`() {
        val user = ApiTestFixtures.registerUser()

        given()
            .contentType(ContentType.JSON)
            .body(TestDataBuilder.userLogin(user.email, "wrongpassword"))
            .`when`()
            .post("/api/users/login")
            .then()
            .statusCode(401)
    }

    @Test
    fun `should get current user`() {
        val user = ApiTestFixtures.registerUser()

        ApiTestFixtures.authenticatedRequest(user.token)
            .`when`()
            .get("/api/user")
            .then()
            .statusCode(200)
            .body("user.email", equalTo(user.email))
            .body("user.username", equalTo(user.username))
    }

    @Test
    fun `should not get current user without token`() {
        given()
            .`when`()
            .get("/api/user")
            .then()
            .statusCode(401)
    }

    @Test
    fun `should update user profile`() {
        val user = ApiTestFixtures.registerUser()
        val newEmail = TestDataBuilder.uniqueEmail()
        val newUsername = TestDataBuilder.uniqueUsername()

        ApiTestFixtures.authenticatedRequest(user.token)
            .body(
                TestDataBuilder.userUpdate(
                    email = newEmail,
                    username = newUsername,
                    bio = "Updated bio",
                    image = "https://example.com/image.jpg",
                ),
            ).`when`()
            .put("/api/user")
            .then()
            .statusCode(200)
            .body("user.email", equalTo(newEmail))
            .body("user.username", equalTo(newUsername))
            .body("user.bio", equalTo("Updated bio"))
            .body("user.image", equalTo("https://example.com/image.jpg"))
    }

    @Test
    fun `should update user password`() {
        val user = ApiTestFixtures.registerUser()
        val newPassword = "newpassword123"

        ApiTestFixtures.authenticatedRequest(user.token)
            .body(TestDataBuilder.userUpdate(password = newPassword))
            .`when`()
            .put("/api/user")
            .then()
            .statusCode(200)

        given()
            .contentType(ContentType.JSON)
            .body(TestDataBuilder.userLogin(user.email, newPassword))
            .`when`()
            .post("/api/users/login")
            .then()
            .statusCode(200)
    }

    @Test
    fun `should get profile by username`() {
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
            .get("/api/profiles/nonexistent")
            .then()
            .statusCode(404)
    }

    @Test
    fun `should follow user`() {
        val follower = ApiTestFixtures.registerUser()
        val followee = ApiTestFixtures.registerUser()

        ApiTestFixtures.authenticatedRequest(follower.token)
            .`when`()
            .post("/api/profiles/${followee.username}/follow")
            .then()
            .statusCode(200)
            .body("profile.username", equalTo(followee.username))
            .body("profile.following", equalTo(true))
    }

    @Test
    fun `should unfollow user`() {
        val follower = ApiTestFixtures.registerUser()
        val followee = ApiTestFixtures.registerUser()

        ApiTestFixtures.authenticatedRequest(follower.token)
            .post("/api/profiles/${followee.username}/follow")

        ApiTestFixtures.authenticatedRequest(follower.token)
            .`when`()
            .delete("/api/profiles/${followee.username}/follow")
            .then()
            .statusCode(200)
            .body("profile.username", equalTo(followee.username))
            .body("profile.following", equalTo(false))
    }

    @Test
    fun `should not follow non-existent user`() {
        val user = ApiTestFixtures.registerUser()

        ApiTestFixtures.authenticatedRequest(user.token)
            .`when`()
            .post("/api/profiles/nonexistent/follow")
            .then()
            .statusCode(404)
    }

    @Test
    fun `should not follow yourself`() {
        val user = ApiTestFixtures.registerUser()

        ApiTestFixtures.authenticatedRequest(user.token)
            .`when`()
            .post("/api/profiles/${user.username}/follow")
            .then()
            .statusCode(400)
    }
}
