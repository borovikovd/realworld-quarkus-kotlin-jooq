package com.example.infrastructure.ratelimit

import com.example.testsupport.ApiTestFixtures
import com.example.testsupport.BaseApiTest
import com.example.testsupport.TestDataBuilder
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.QuarkusTestProfile
import io.quarkus.test.junit.TestProfile
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

@QuarkusTest
@TestProfile(RateLimitApiTest.Profile::class)
class RateLimitApiTest : BaseApiTest() {
    class Profile : QuarkusTestProfile {
        override fun getConfigOverrides(): Map<String, String> =
            mapOf(
                "rate-limit.login.max-requests" to "2",
                "rate-limit.window-seconds" to "60",
                "rate-limit.trusted-proxy-count" to "1",
            )
    }

    @Test
    fun `returns 429 with problem+json after exceeding login budget for the same client IP`() {
        val user = ApiTestFixtures.registerUser()
        val clientIp = "203.0.113.7"

        repeat(2) {
            given()
                .header("X-Forwarded-For", clientIp)
                .contentType(ContentType.JSON)
                .body(TestDataBuilder.userLogin(user.email, user.password))
                .`when`()
                .post("/api/users/login")
                .then()
                .statusCode(200)
        }

        given()
            .header("X-Forwarded-For", clientIp)
            .contentType(ContentType.JSON)
            .body(TestDataBuilder.userLogin(user.email, user.password))
            .`when`()
            .post("/api/users/login")
            .then()
            .statusCode(429)
            .header("Retry-After", "60")
            .header("Content-Type", containsString("application/problem+json"))
            .body("title", equalTo("Too Many Requests"))
            .body("status", equalTo(429))
    }

    @Test
    fun `distinct client IPs from a trusted proxy each get their own budget`() {
        val user = ApiTestFixtures.registerUser()

        // 4 requests > budget of 2, but each from a different client IP — none should be limited.
        (1..4).forEach { i ->
            given()
                .header("X-Forwarded-For", "198.51.100.$i")
                .contentType(ContentType.JSON)
                .body(TestDataBuilder.userLogin(user.email, user.password))
                .`when`()
                .post("/api/users/login")
                .then()
                .statusCode(200)
        }
    }
}
