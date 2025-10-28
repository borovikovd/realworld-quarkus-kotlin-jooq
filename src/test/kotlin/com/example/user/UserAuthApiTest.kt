package com.example.user

import com.example.shared.PostgresTestResource
import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.sql.DataSource
import jakarta.inject.Inject
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue

@QuarkusTest
@QuarkusTestResource(PostgresTestResource::class)
class UserAuthApiTest {
    @Inject
    lateinit var dataSource: DataSource

    @BeforeEach
    fun setup() {
        cleanDatabase()
    }

    private fun cleanDatabase() {
        dataSource.connection.use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("TRUNCATE TABLE favorites CASCADE")
                stmt.execute("TRUNCATE TABLE article_tags CASCADE")
                stmt.execute("TRUNCATE TABLE comments CASCADE")
                stmt.execute("TRUNCATE TABLE articles CASCADE")
                stmt.execute("TRUNCATE TABLE tags CASCADE")
                stmt.execute("TRUNCATE TABLE followers CASCADE")
                stmt.execute("TRUNCATE TABLE users CASCADE")
                stmt.execute("ALTER SEQUENCE users_id_seq RESTART WITH 1")
            }
        }
    }

    @Test
    fun `should register new user`() {
        given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "user": {
                    "email": "test@example.com",
                    "username": "testuser",
                    "password": "password123"
                  }
                }
                """.trimIndent(),
            ).`when`()
            .post("/api/users")
            .then()
            .statusCode(201)
            .body("user.email", equalTo("test@example.com"))
            .body("user.username", equalTo("testuser"))
            .body("user.token", notNullValue())
            .body("user.bio", equalTo(null))
            .body("user.image", equalTo(null))
    }

    @Test
    fun `should not register user with duplicate email`() {
        given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "user": {
                    "email": "test@example.com",
                    "username": "testuser",
                    "password": "password123"
                  }
                }
                """.trimIndent(),
            ).post("/api/users")

        given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "user": {
                    "email": "test@example.com",
                    "username": "differentuser",
                    "password": "password123"
                  }
                }
                """.trimIndent(),
            ).`when`()
            .post("/api/users")
            .then()
            .statusCode(422)
            .body("errors.email[0]", equalTo("is already taken"))
    }

    @Test
    fun `should not register user with duplicate username`() {
        given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "user": {
                    "email": "test@example.com",
                    "username": "testuser",
                    "password": "password123"
                  }
                }
                """.trimIndent(),
            ).post("/api/users")

        given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "user": {
                    "email": "different@example.com",
                    "username": "testuser",
                    "password": "password123"
                  }
                }
                """.trimIndent(),
            ).`when`()
            .post("/api/users")
            .then()
            .statusCode(422)
            .body("errors.username[0]", equalTo("is already taken"))
    }

    @Test
    fun `should not register user with short password`() {
        given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "user": {
                    "email": "test@example.com",
                    "username": "testuser",
                    "password": "short"
                  }
                }
                """.trimIndent(),
            ).`when`()
            .post("/api/users")
            .then()
            .statusCode(422)
            .body("errors.password[0]", equalTo("must be at least 8 characters"))
    }

    @Test
    fun `should login with valid credentials`() {
        given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "user": {
                    "email": "test@example.com",
                    "username": "testuser",
                    "password": "password123"
                  }
                }
                """.trimIndent(),
            ).post("/api/users")

        given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "user": {
                    "email": "test@example.com",
                    "password": "password123"
                  }
                }
                """.trimIndent(),
            ).`when`()
            .post("/api/users/login")
            .then()
            .statusCode(200)
            .body("user.email", equalTo("test@example.com"))
            .body("user.username", equalTo("testuser"))
            .body("user.token", notNullValue())
    }

    @Test
    fun `should not login with invalid email`() {
        given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "user": {
                    "email": "test@example.com",
                    "username": "testuser",
                    "password": "password123"
                  }
                }
                """.trimIndent(),
            ).post("/api/users")

        given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "user": {
                    "email": "wrong@example.com",
                    "password": "password123"
                  }
                }
                """.trimIndent(),
            ).`when`()
            .post("/api/users/login")
            .then()
            .statusCode(401)
    }

    @Test
    fun `should not login with invalid password`() {
        given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "user": {
                    "email": "test@example.com",
                    "username": "testuser",
                    "password": "password123"
                  }
                }
                """.trimIndent(),
            ).post("/api/users")

        given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "user": {
                    "email": "test@example.com",
                    "password": "wrongpassword"
                  }
                }
                """.trimIndent(),
            ).`when`()
            .post("/api/users/login")
            .then()
            .statusCode(401)
    }

    @Test
    fun `should get current user`() {
        val token = registerUser()

        given()
            .header("Authorization", "Bearer $token")
            .`when`()
            .get("/api/user")
            .then()
            .statusCode(200)
            .body("user.email", equalTo("test@example.com"))
            .body("user.username", equalTo("testuser"))
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
        val token = registerUser()

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $token")
            .body(
                """
                {
                  "user": {
                    "email": "updated@example.com",
                    "username": "updateduser",
                    "bio": "Updated bio",
                    "image": "https://example.com/image.jpg"
                  }
                }
                """.trimIndent(),
            ).`when`()
            .put("/api/user")
            .then()
            .statusCode(200)
            .body("user.email", equalTo("updated@example.com"))
            .body("user.username", equalTo("updateduser"))
            .body("user.bio", equalTo("Updated bio"))
            .body("user.image", equalTo("https://example.com/image.jpg"))
    }

    @Test
    fun `should update user password`() {
        val token = registerUser()

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $token")
            .body(
                """
                {
                  "user": {
                    "password": "newpassword123"
                  }
                }
                """.trimIndent(),
            ).`when`()
            .put("/api/user")
            .then()
            .statusCode(200)

        given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "user": {
                    "email": "test@example.com",
                    "password": "newpassword123"
                  }
                }
                """.trimIndent(),
            ).`when`()
            .post("/api/users/login")
            .then()
            .statusCode(200)
    }

    @Test
    fun `should get profile by username`() {
        registerUser()

        given()
            .`when`()
            .get("/api/profiles/testuser")
            .then()
            .statusCode(200)
            .body("profile.username", equalTo("testuser"))
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
        val token = registerUser()

        given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "user": {
                    "email": "other@example.com",
                    "username": "otheruser",
                    "password": "password123"
                  }
                }
                """.trimIndent(),
            ).post("/api/users")

        given()
            .header("Authorization", "Bearer $token")
            .`when`()
            .post("/api/profiles/otheruser/follow")
            .then()
            .statusCode(200)
            .body("profile.username", equalTo("otheruser"))
            .body("profile.following", equalTo(true))
    }

    @Test
    fun `should unfollow user`() {
        val token = registerUser()

        given()
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "user": {
                    "email": "other@example.com",
                    "username": "otheruser",
                    "password": "password123"
                  }
                }
                """.trimIndent(),
            ).post("/api/users")

        given()
            .header("Authorization", "Bearer $token")
            .post("/api/profiles/otheruser/follow")

        given()
            .header("Authorization", "Bearer $token")
            .`when`()
            .delete("/api/profiles/otheruser/follow")
            .then()
            .statusCode(200)
            .body("profile.username", equalTo("otheruser"))
            .body("profile.following", equalTo(false))
    }

    @Test
    fun `should not follow non-existent user`() {
        val token = registerUser()

        given()
            .header("Authorization", "Bearer $token")
            .`when`()
            .post("/api/profiles/nonexistent/follow")
            .then()
            .statusCode(404)
    }

    @Test
    fun `should not follow yourself`() {
        val token = registerUser()

        given()
            .header("Authorization", "Bearer $token")
            .`when`()
            .post("/api/profiles/testuser/follow")
            .then()
            .statusCode(400)
    }

    private fun registerUser(): String {
        val response =
            given()
                .contentType(ContentType.JSON)
                .body(
                    """
                    {
                      "user": {
                        "email": "test@example.com",
                        "username": "testuser",
                        "password": "password123"
                      }
                    }
                    """.trimIndent(),
                ).`when`()
                .post("/api/users")
                .then()
                .statusCode(201)
                .extract()
                .response()

        return response.jsonPath().getString("user.token")
    }
}
