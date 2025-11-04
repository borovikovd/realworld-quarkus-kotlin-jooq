package com.example.shared

import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import io.restassured.response.Response
import io.restassured.specification.RequestSpecification

object ApiTestFixtures {
    fun authenticatedRequest(token: String): RequestSpecification =
        given()
            .header("Authorization", "Bearer $token")
            .contentType(ContentType.JSON)

    fun registerUser(
        email: String = TestDataBuilder.uniqueEmail(),
        username: String = TestDataBuilder.uniqueUsername(),
        password: String = "password123",
    ): UserRegistrationResult {
        val response =
            given()
                .contentType(ContentType.JSON)
                .body(TestDataBuilder.userRegistration(email, username, password))
                .`when`()
                .post("/api/users")
                .then()
                .statusCode(201)
                .extract()
                .response()

        return UserRegistrationResult(
            email = email,
            username = username,
            password = password,
            token = response.jsonPath().getString("user.token"),
        )
    }

    fun loginUser(
        email: String,
        password: String,
    ): String {
        val response =
            given()
                .contentType(ContentType.JSON)
                .body(TestDataBuilder.userLogin(email, password))
                .`when`()
                .post("/api/users/login")
                .then()
                .statusCode(200)
                .extract()
                .response()

        return response.jsonPath().getString("user.token")
    }

    fun createArticle(
        token: String,
        title: String = TestDataBuilder.uniqueTitle(),
        description: String = "Test description",
        body: String = "Test body content",
        tags: List<String> = listOf("test", "kotlin"),
    ): ArticleCreationResult {
        val response =
            authenticatedRequest(token)
                .body(TestDataBuilder.articleCreation(title, description, body, tags))
                .`when`()
                .post("/api/articles")
                .then()
                .statusCode(201)
                .extract()
                .response()

        return ArticleCreationResult(
            slug = response.jsonPath().getString("article.slug"),
            title = title,
        )
    }

    fun createComment(
        token: String,
        articleSlug: String,
        body: String = "Test comment",
    ): CommentCreationResult {
        val response =
            authenticatedRequest(token)
                .body(TestDataBuilder.commentCreation(body))
                .`when`()
                .post("/api/articles/$articleSlug/comments")
                .then()
                .statusCode(201)
                .extract()
                .response()

        return CommentCreationResult(
            id = response.jsonPath().getInt("comment.id"),
            body = body,
        )
    }
}

data class UserRegistrationResult(
    val email: String,
    val username: String,
    val password: String,
    val token: String,
)

data class ArticleCreationResult(
    val slug: String,
    val title: String,
)

data class CommentCreationResult(
    val id: Int,
    val body: String,
)
