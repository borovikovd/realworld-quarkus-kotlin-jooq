package com.example.article

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
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.notNullValue

@QuarkusTest
@QuarkusTestResource(PostgresTestResource::class)
class ArticleApiTest {
    @Inject
    lateinit var dataSource: DataSource

    private lateinit var token: String

    @BeforeEach
    fun setup() {
        cleanDatabase()
        token = registerAndLoginUser()
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
                stmt.execute("ALTER SEQUENCE articles_id_seq RESTART WITH 1")
            }
        }
    }

    private fun registerAndLoginUser(): String {
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

    @Test
    fun `should create article`() {
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $token")
            .body(
                """
                {
                  "article": {
                    "title": "Test Article",
                    "description": "Test description",
                    "body": "Test body content",
                    "tagList": ["test", "kotlin"]
                  }
                }
                """.trimIndent(),
            ).`when`()
            .post("/api/articles")
            .then()
            .statusCode(201)
            .body("article.slug", equalTo("test-article"))
            .body("article.title", equalTo("Test Article"))
            .body("article.description", equalTo("Test description"))
            .body("article.body", equalTo("Test body content"))
            .body("article.tagList", hasSize<Any>(2))
            .body("article.favorited", equalTo(false))
            .body("article.favoritesCount", equalTo(0))
            .body("article.author.username", equalTo("testuser"))
            .body("article.createdAt", notNullValue())
            .body("article.updatedAt", notNullValue())
    }

    @Test
    fun `should get article by slug`() {
        createTestArticle()

        given()
            .`when`()
            .get("/api/articles/test-article")
            .then()
            .statusCode(200)
            .body("article.slug", equalTo("test-article"))
            .body("article.title", equalTo("Test Article"))
    }

    @Test
    fun `should return 404 when article not found`() {
        given()
            .`when`()
            .get("/api/articles/non-existent-slug")
            .then()
            .statusCode(404)
    }

    @Test
    fun `should update article`() {
        createTestArticle()

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $token")
            .body(
                """
                {
                  "article": {
                    "title": "Updated Title",
                    "description": "Updated description",
                    "body": "Updated body"
                  }
                }
                """.trimIndent(),
            ).`when`()
            .put("/api/articles/test-article")
            .then()
            .statusCode(200)
            .body("article.slug", equalTo("updated-title"))
            .body("article.title", equalTo("Updated Title"))
            .body("article.description", equalTo("Updated description"))
            .body("article.body", equalTo("Updated body"))
    }

    @Test
    fun `should not allow non-author to update article`() {
        createTestArticle()

        val otherUserToken = createOtherUser()

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $otherUserToken")
            .body(
                """
                {
                  "article": {
                    "title": "Hacked Title"
                  }
                }
                """.trimIndent(),
            ).`when`()
            .put("/api/articles/test-article")
            .then()
            .statusCode(403)
    }

    @Test
    fun `should delete article`() {
        createTestArticle()

        given()
            .header("Authorization", "Bearer $token")
            .`when`()
            .delete("/api/articles/test-article")
            .then()
            .statusCode(204)

        given()
            .`when`()
            .get("/api/articles/test-article")
            .then()
            .statusCode(404)
    }

    @Test
    fun `should not allow non-author to delete article`() {
        createTestArticle()

        val otherUserToken = createOtherUser()

        given()
            .header("Authorization", "Bearer $otherUserToken")
            .`when`()
            .delete("/api/articles/test-article")
            .then()
            .statusCode(403)
    }

    @Test
    fun `should favorite article`() {
        createTestArticle()

        given()
            .header("Authorization", "Bearer $token")
            .`when`()
            .post("/api/articles/test-article/favorite")
            .then()
            .statusCode(200)
            .body("article.favorited", equalTo(true))
            .body("article.favoritesCount", equalTo(1))
    }

    @Test
    fun `should unfavorite article`() {
        createTestArticle()

        given()
            .header("Authorization", "Bearer $token")
            .post("/api/articles/test-article/favorite")

        given()
            .header("Authorization", "Bearer $token")
            .`when`()
            .delete("/api/articles/test-article/favorite")
            .then()
            .statusCode(200)
            .body("article.favorited", equalTo(false))
            .body("article.favoritesCount", equalTo(0))
    }

    @Test
    fun `should list articles`() {
        createTestArticle()

        given()
            .`when`()
            .get("/api/articles")
            .then()
            .statusCode(200)
            .body("articles", hasSize<Any>(1))
            .body("articlesCount", equalTo(1))
    }

    @Test
    fun `should filter articles by tag`() {
        createTestArticle()

        given()
            .queryParam("tag", "test")
            .`when`()
            .get("/api/articles")
            .then()
            .statusCode(200)
            .body("articles", hasSize<Any>(1))

        given()
            .queryParam("tag", "nonexistent")
            .`when`()
            .get("/api/articles")
            .then()
            .statusCode(200)
            .body("articles", hasSize<Any>(0))
    }

    @Test
    fun `should filter articles by author`() {
        createTestArticle()

        given()
            .queryParam("author", "testuser")
            .`when`()
            .get("/api/articles")
            .then()
            .statusCode(200)
            .body("articles", hasSize<Any>(1))

        given()
            .queryParam("author", "otheruser")
            .`when`()
            .get("/api/articles")
            .then()
            .statusCode(200)
            .body("articles", hasSize<Any>(0))
    }

    @Test
    fun `should create comment on article`() {
        createTestArticle()

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $token")
            .body(
                """
                {
                  "comment": {
                    "body": "Great article!"
                  }
                }
                """.trimIndent(),
            ).`when`()
            .post("/api/articles/test-article/comments")
            .then()
            .statusCode(201)
            .body("comment.body", equalTo("Great article!"))
            .body("comment.author.username", equalTo("testuser"))
    }

    @Test
    fun `should get comments for article`() {
        createTestArticle()
        createTestComment()

        given()
            .`when`()
            .get("/api/articles/test-article/comments")
            .then()
            .statusCode(200)
            .body("comments", hasSize<Any>(1))
            .body("comments[0].body", equalTo("Great article!"))
    }

    @Test
    fun `should delete comment`() {
        createTestArticle()
        val commentId = createTestComment()

        given()
            .header("Authorization", "Bearer $token")
            .`when`()
            .delete("/api/articles/test-article/comments/$commentId")
            .then()
            .statusCode(204)
    }

    @Test
    fun `should get all tags`() {
        createTestArticle()

        given()
            .`when`()
            .get("/api/tags")
            .then()
            .statusCode(200)
            .body("tags", hasSize<Any>(2))
    }

    private fun createTestArticle() {
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $token")
            .body(
                """
                {
                  "article": {
                    "title": "Test Article",
                    "description": "Test description",
                    "body": "Test body content",
                    "tagList": ["test", "kotlin"]
                  }
                }
                """.trimIndent(),
            ).`when`()
            .post("/api/articles")
            .then()
            .statusCode(201)
    }

    private fun createTestComment(): Int {
        val response =
            given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $token")
                .body(
                    """
                    {
                      "comment": {
                        "body": "Great article!"
                      }
                    }
                    """.trimIndent(),
                ).`when`()
                .post("/api/articles/test-article/comments")
                .then()
                .statusCode(201)
                .extract()
                .response()

        return response.jsonPath().getInt("comment.id")
    }

    private fun createOtherUser(): String {
        val response =
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
                ).`when`()
                .post("/api/users")
                .then()
                .statusCode(201)
                .extract()
                .response()

        return response.jsonPath().getString("user.token")
    }
}
