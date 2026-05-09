package com.example.comment

import com.example.testsupport.ApiTestFixtures
import com.example.testsupport.BaseApiTest
import com.example.testsupport.TestDataBuilder
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Test

@QuarkusTest
class CommentApiTest : BaseApiTest() {
    @Test
    fun `should add comment to article`() {
        val user = ApiTestFixtures.registerUser()
        val article = ApiTestFixtures.createArticle(user.token)

        ApiTestFixtures.authenticatedRequest(user.token)
            .body(TestDataBuilder.commentCreation("Great article!"))
            .`when`()
            .post("/api/articles/${article.slug}/comments")
            .then()
            .statusCode(201)
            .body("comment.body", equalTo("Great article!"))
            .body("comment.author.username", equalTo(user.username))
    }

    @Test
    fun `should get comments for article`() {
        val user = ApiTestFixtures.registerUser()
        val article = ApiTestFixtures.createArticle(user.token)
        ApiTestFixtures.createComment(user.token, article.slug, "Comment 1")
        ApiTestFixtures.createComment(user.token, article.slug, "Comment 2")

        given()
            .`when`()
            .get("/api/articles/${article.slug}/comments")
            .then()
            .statusCode(200)
            .body("comments", hasSize<Any>(2))
    }

    @Test
    fun `should delete comment`() {
        val user = ApiTestFixtures.registerUser()
        val article = ApiTestFixtures.createArticle(user.token)
        val comment = ApiTestFixtures.createComment(user.token, article.slug)

        ApiTestFixtures.authenticatedRequest(user.token)
            .`when`()
            .delete("/api/articles/${article.slug}/comments/${comment.id}")
            .then()
            .statusCode(204)
    }

    @Test
    fun `should not allow non-author to delete comment`() {
        val author = ApiTestFixtures.registerUser()
        val other = ApiTestFixtures.registerUser()
        val article = ApiTestFixtures.createArticle(author.token)
        val comment = ApiTestFixtures.createComment(author.token, article.slug)

        ApiTestFixtures.authenticatedRequest(other.token)
            .`when`()
            .delete("/api/articles/${article.slug}/comments/${comment.id}")
            .then()
            .statusCode(403)
            .body("errors.comment[0]", equalTo("forbidden"))
    }

    @Test
    fun `should return 401 when adding comment without auth`() {
        val user = ApiTestFixtures.registerUser()
        val article = ApiTestFixtures.createArticle(user.token)

        given()
            .contentType(ContentType.JSON)
            .body(TestDataBuilder.commentCreation("Test"))
            .`when`()
            .post("/api/articles/${article.slug}/comments")
            .then()
            .statusCode(401)
    }

    @Test
    fun `should return 404 when commenting on non-existent article`() {
        val user = ApiTestFixtures.registerUser()

        ApiTestFixtures.authenticatedRequest(user.token)
            .body(TestDataBuilder.commentCreation("Test"))
            .`when`()
            .post("/api/articles/nonexistent-slug/comments")
            .then()
            .statusCode(404)
            .body("errors.article[0]", equalTo("not found"))
    }

    @Test
    fun `should return 404 when listing comments on non-existent article`() {
        given()
            .`when`()
            .get("/api/articles/nonexistent-slug/comments")
            .then()
            .statusCode(404)
            .body("errors.article[0]", equalTo("not found"))
    }

    @Test
    fun `should return 404 when deleting non-existent comment`() {
        val user = ApiTestFixtures.registerUser()
        val article = ApiTestFixtures.createArticle(user.token)

        ApiTestFixtures.authenticatedRequest(user.token)
            .`when`()
            .delete("/api/articles/${article.slug}/comments/999999")
            .then()
            .statusCode(404)
            .body("errors.comment[0]", equalTo("not found"))
    }

    @Test
    fun `should return 404 when deleting comment from wrong article`() {
        val user = ApiTestFixtures.registerUser()
        val article1 = ApiTestFixtures.createArticle(user.token)
        val article2 = ApiTestFixtures.createArticle(user.token)
        val comment = ApiTestFixtures.createComment(user.token, article1.slug)

        ApiTestFixtures.authenticatedRequest(user.token)
            .`when`()
            .delete("/api/articles/${article2.slug}/comments/${comment.id}")
            .then()
            .statusCode(404)
            .body("errors.comment[0]", equalTo("not found"))
    }

    @Test
    fun `should return 422 when adding comment with blank body`() {
        val user = ApiTestFixtures.registerUser()
        val article = ApiTestFixtures.createArticle(user.token)

        ApiTestFixtures.authenticatedRequest(user.token)
            .body(TestDataBuilder.commentCreation(""))
            .`when`()
            .post("/api/articles/${article.slug}/comments")
            .then()
            .statusCode(422)
            .body("errors.body[0]", equalTo("can't be blank"))
    }
}
