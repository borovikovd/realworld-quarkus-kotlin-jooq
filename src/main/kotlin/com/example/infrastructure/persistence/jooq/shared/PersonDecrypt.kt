package com.example.infrastructure.persistence.jooq.shared

import com.example.application.outport.security.CryptoService
import com.example.application.readmodel.ProfileReadModel
import com.example.jooq.vault.tables.references.PERSON
import org.jooq.Record

internal fun Record.decryptAuthorProfile(
    crypto: CryptoService,
    userId: Long?,
    following: Boolean = false,
): ProfileReadModel {
    checkNotNull(userId) { "Author userId must not be null" }
    val usernameEnc = checkNotNull(get(PERSON.USERNAME_ENC)) { "Author PERSON record missing for userId=$userId" }
    return ProfileReadModel(
        username = crypto.decryptField(userId, CryptoService.USERNAME, usernameEnc),
        bio = get(PERSON.BIO_ENC)?.let { crypto.decryptField(userId, CryptoService.BIO, it) },
        image = get(PERSON.IMAGE_ENC)?.let { crypto.decryptField(userId, CryptoService.IMAGE, it) },
        following = following,
    )
}
