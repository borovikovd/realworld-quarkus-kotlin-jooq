package com.example.shared

import java.util.UUID

object TestDataBuilder {
    fun uniqueEmail(prefix: String = "test"): String = "$prefix-${UUID.randomUUID().toString().take(8)}@example.com"

    fun uniqueUsername(prefix: String = "user"): String = "$prefix-${UUID.randomUUID().toString().take(8)}"

    fun uniqueTitle(prefix: String = "Article"): String = "$prefix ${UUID.randomUUID().toString().take(8)}"

    fun userRegistration(
        email: String = uniqueEmail(),
        username: String = uniqueUsername(),
        password: String = "password123",
    ) = """
        {
          "user": {
            "email": "$email",
            "username": "$username",
            "password": "$password"
          }
        }
    """.trimIndent()

    fun userLogin(
        email: String,
        password: String,
    ) = """
        {
          "user": {
            "email": "$email",
            "password": "$password"
          }
        }
    """.trimIndent()

    fun articleCreation(
        title: String = uniqueTitle(),
        description: String = "Test description",
        body: String = "Test body content",
        tags: List<String> = listOf("test", "kotlin"),
    ) = """
        {
          "article": {
            "title": "$title",
            "description": "$description",
            "body": "$body",
            "tagList": [${tags.joinToString(",") { "\"$it\"" }}]
          }
        }
    """.trimIndent()

    fun articleUpdate(
        title: String? = null,
        description: String? = null,
        body: String? = null,
    ): String {
        val fields = mutableListOf<String>()
        title?.let { fields.add("\"title\": \"$it\"") }
        description?.let { fields.add("\"description\": \"$it\"") }
        body?.let { fields.add("\"body\": \"$it\"") }

        return """
            {
              "article": {
                ${fields.joinToString(",")}
              }
            }
        """.trimIndent()
    }

    fun userUpdate(
        email: String? = null,
        username: String? = null,
        password: String? = null,
        bio: String? = null,
        image: String? = null,
    ): String {
        val fields = mutableListOf<String>()
        email?.let { fields.add("\"email\": \"$it\"") }
        username?.let { fields.add("\"username\": \"$it\"") }
        password?.let { fields.add("\"password\": \"$it\"") }
        bio?.let { fields.add("\"bio\": \"$it\"") }
        image?.let { fields.add("\"image\": \"$it\"") }

        return """
            {
              "user": {
                ${fields.joinToString(",")}
              }
            }
        """.trimIndent()
    }

    fun commentCreation(body: String = "Test comment") = """
        {
          "comment": {
            "body": "$body"
          }
        }
    """.trimIndent()
}
