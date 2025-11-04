package com.example.article

import com.example.shared.ApiTestFixtures
import com.example.shared.BaseApiTest
import com.example.shared.TestDataBuilder
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.hasSize
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.Test

@QuarkusTest
class ArticleApiTest : BaseApiTest() {
    @Test
    fun `should create article`() {
        val user = ApiTestFixtures.registerUser()
        val title = TestDataBuilder.uniqueTitle()

        ApiTestFixtures.authenticatedRequest(user.token)
            .body(TestDataBuilder.articleCreation(title = title))
            .`when`()
            .post("/api/articles")
            .then()
            .statusCode(201)
            .body("article.title", equalTo(title))
            .body("article.description", equalTo("Test description"))
            .body("article.body", equalTo("Test body content"))
            .body("article.tagList", hasSize<Any>(2))
            .body("article.favorited", equalTo(false))
            .body("article.favoritesCount", equalTo(0))
            .body("article.author.username", equalTo(user.username))
            .body("article.createdAt", notNullValue())
            .body("article.updatedAt", notNullValue())
    }

    @Test
    fun `should get article by slug`() {
        val user = ApiTestFixtures.registerUser()
        val article = ApiTestFixtures.createArticle(user.token)

        given()
            .`when`()
            .get("/api/articles/${article.slug}")
            .then()
            .statusCode(200)
            .body("article.slug", equalTo(article.slug))
            .body("article.title", equalTo(article.title))
    }

    @Test
    fun `should return 404 for non-existent article`() {
        given()
            .`when`()
            .get("/api/articles/nonexistent-slug")
            .then()
            .statusCode(404)
    }

    @Test
    fun `should update article`() {
        val user = ApiTestFixtures.registerUser()
        val article = ApiTestFixtures.createArticle(user.token)
        val newTitle = TestDataBuilder.uniqueTitle("Updated")

        ApiTestFixtures.authenticatedRequest(user.token)
            .body(TestDataBuilder.articleUpdate(title = newTitle))
            .`when`()
            .put("/api/articles/${article.slug}")
            .then()
            .statusCode(200)
            .body("article.title", equalTo(newTitle))
    }

    @Test
    fun `should not allow non-author to update article`() {
        val author = ApiTestFixtures.registerUser()
        val other = ApiTestFixtures.registerUser()
        val article = ApiTestFixtures.createArticle(author.token)

        ApiTestFixtures.authenticatedRequest(other.token)
            .body(TestDataBuilder.articleUpdate(title = "Hacked"))
            .`when`()
            .put("/api/articles/${article.slug}")
            .then()
            .statusCode(403)
    }

    @Test
    fun `should delete article`() {
        val user = ApiTestFixtures.registerUser()
        val article = ApiTestFixtures.createArticle(user.token)

        ApiTestFixtures.authenticatedRequest(user.token)
            .`when`()
            .delete("/api/articles/${article.slug}")
            .then()
            .statusCode(204)

        given()
            .`when`()
            .get("/api/articles/${article.slug}")
            .then()
            .statusCode(404)
    }

    @Test
    fun `should not allow non-author to delete article`() {
        val author = ApiTestFixtures.registerUser()
        val other = ApiTestFixtures.registerUser()
        val article = ApiTestFixtures.createArticle(author.token)

        ApiTestFixtures.authenticatedRequest(other.token)
            .`when`()
            .delete("/api/articles/${article.slug}")
            .then()
            .statusCode(403)
    }

    @Test
    fun `should favorite article`() {
        val user = ApiTestFixtures.registerUser()
        val article = ApiTestFixtures.createArticle(user.token)

        ApiTestFixtures.authenticatedRequest(user.token)
            .`when`()
            .post("/api/articles/${article.slug}/favorite")
            .then()
            .statusCode(200)
            .body("article.favorited", equalTo(true))
            .body("article.favoritesCount", equalTo(1))
    }

    @Test
    fun `should unfavorite article`() {
        val user = ApiTestFixtures.registerUser()
        val article = ApiTestFixtures.createArticle(user.token)

        ApiTestFixtures.authenticatedRequest(user.token)
            .post("/api/articles/${article.slug}/favorite")

        ApiTestFixtures.authenticatedRequest(user.token)
            .`when`()
            .delete("/api/articles/${article.slug}/favorite")
            .then()
            .statusCode(200)
            .body("article.favorited", equalTo(false))
            .body("article.favoritesCount", equalTo(0))
    }

    @Test
    fun `should list articles`() {
        val user = ApiTestFixtures.registerUser()
        ApiTestFixtures.createArticle(user.token, title = "Article 1")
        ApiTestFixtures.createArticle(user.token, title = "Article 2")

        given()
            .`when`()
            .get("/api/articles")
            .then()
            .statusCode(200)
            .body("articles", hasSize<Any>(greaterThan(0)))
            .body("articlesCount", greaterThan(0))
    }

    @Test
    fun `should filter articles by author`() {
        val author = ApiTestFixtures.registerUser()
        val other = ApiTestFixtures.registerUser()
        ApiTestFixtures.createArticle(author.token)
        ApiTestFixtures.createArticle(other.token)

        given()
            .queryParam("author", author.username)
            .`when`()
            .get("/api/articles")
            .then()
            .statusCode(200)
            .body("articles", hasSize<Any>(1))
            .body("articles[0].author.username", equalTo(author.username))
    }

    @Test
    fun `should filter articles by tag`() {
        val user = ApiTestFixtures.registerUser()
        val uniqueTag = "tag-${TestDataBuilder.uniqueUsername()}"
        ApiTestFixtures.createArticle(user.token, tags = listOf(uniqueTag))
        ApiTestFixtures.createArticle(user.token, tags = listOf("other"))

        given()
            .queryParam("tag", uniqueTag)
            .`when`()
            .get("/api/articles")
            .then()
            .statusCode(200)
            .body("articles", hasSize<Any>(1))
    }

    @Test
    fun `should filter articles by favorited user`() {
        val user1 = ApiTestFixtures.registerUser()
        val user2 = ApiTestFixtures.registerUser()
        val article = ApiTestFixtures.createArticle(user1.token)

        ApiTestFixtures.authenticatedRequest(user2.token)
            .post("/api/articles/${article.slug}/favorite")

        given()
            .queryParam("favorited", user2.username)
            .`when`()
            .get("/api/articles")
            .then()
            .statusCode(200)
            .body("articles", hasSize<Any>(1))
            .body("articles[0].slug", equalTo(article.slug))
    }

    @Test
    fun `should get articles feed`() {
        val follower = ApiTestFixtures.registerUser()
        val author = ApiTestFixtures.registerUser()

        ApiTestFixtures.authenticatedRequest(follower.token)
            .post("/api/profiles/${author.username}/follow")

        ApiTestFixtures.createArticle(author.token)

        ApiTestFixtures.authenticatedRequest(follower.token)
            .`when`()
            .get("/api/articles/feed")
            .then()
            .statusCode(200)
            .body("articles", hasSize<Any>(1))
            .body("articles[0].author.username", equalTo(author.username))
    }

    @Test
    fun `should not get feed without authentication`() {
        given()
            .`when`()
            .get("/api/articles/feed")
            .then()
            .statusCode(401)
    }

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
    }

    @Test
    fun `should get all tags`() {
        val user = ApiTestFixtures.registerUser()
        val tag1 = "tag-${TestDataBuilder.uniqueUsername()}"
        val tag2 = "tag-${TestDataBuilder.uniqueUsername()}"
        ApiTestFixtures.createArticle(user.token, tags = listOf(tag1, tag2))

        given()
            .`when`()
            .get("/api/tags")
            .then()
            .statusCode(200)
            .body("tags", hasSize<Any>(greaterThan(0)))
    }
}
