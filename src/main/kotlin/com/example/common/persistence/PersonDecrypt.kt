package com.example.common.persistence

import com.example.common.security.CryptoService
import com.example.jooq.vault.tables.references.PERSON
import com.example.user.ProfileDto
import org.jooq.Record

internal const val FIELD_EMAIL = "email"
internal const val FIELD_USERNAME = "username"
internal const val FIELD_BIO = "bio"
internal const val FIELD_IMAGE = "image"

fun Record.decryptAuthorProfile(
    crypto: CryptoService,
    userId: Long?,
    following: Boolean = false,
): ProfileDto {
    checkNotNull(userId) { "Author userId must not be null" }
    val usernameEnc = checkNotNull(get(PERSON.USERNAME_ENC)) { "Author PERSON record missing for userId=$userId" }
    return ProfileDto(
        username = crypto.decryptField(userId, FIELD_USERNAME, usernameEnc),
        bio = get(PERSON.BIO_ENC)?.let { crypto.decryptField(userId, FIELD_BIO, it) },
        image = get(PERSON.IMAGE_ENC)?.let { crypto.decryptField(userId, FIELD_IMAGE, it) },
        following = following,
    )
}
