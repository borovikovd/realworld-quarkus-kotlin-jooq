package com.example.infrastructure.persistence.jooq

import com.example.application.outport.security.CryptoService
import com.example.application.readmodel.ProfileReadModel
import com.example.jooq.vault.tables.references.PERSON
import org.jooq.Record

internal fun Record.decryptAuthorProfile(
    crypto: CryptoService,
    userId: Long?,
    following: Boolean = false,
): ProfileReadModel {
    val usernameEnc = get(PERSON.USERNAME_ENC)
    return if (usernameEnc != null && userId != null) {
        ProfileReadModel(
            username = crypto.decryptField(userId, CryptoService.USERNAME, usernameEnc),
            bio = get(PERSON.BIO_ENC)?.let { crypto.decryptField(userId, CryptoService.BIO, it) },
            image = get(PERSON.IMAGE_ENC)?.let { crypto.decryptField(userId, CryptoService.IMAGE, it) },
            following = following,
        )
    } else {
        ProfileReadModel(
            username = "user_$userId",
            bio = null,
            image = null,
            following = following,
        )
    }
}
