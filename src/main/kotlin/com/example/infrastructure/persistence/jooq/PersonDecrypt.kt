package com.example.infrastructure.persistence.jooq

import com.example.application.outport.CryptoService
import com.example.application.readmodel.ProfileReadModel
import com.example.jooq.vault.tables.references.ENCRYPTION_KEY
import com.example.jooq.vault.tables.references.PERSON
import org.jooq.Record

internal fun Record.decryptAuthorProfile(
    crypto: CryptoService,
    fallbackUserId: Long?,
    following: Boolean = false,
): ProfileReadModel {
    val keyCiphertext = get(ENCRYPTION_KEY.KEY_CIPHERTEXT)
    return if (keyCiphertext != null) {
        val dek = crypto.decryptDek(keyCiphertext)
        ProfileReadModel(
            username = crypto.decryptField(dek, get(PERSON.USERNAME_ENC)!!),
            bio = get(PERSON.BIO_ENC)?.let { crypto.decryptField(dek, it) },
            image = get(PERSON.IMAGE_ENC)?.let { crypto.decryptField(dek, it) },
            following = following,
        )
    } else {
        ProfileReadModel(
            username = "user_$fallbackUserId",
            bio = null,
            image = null,
            following = following,
        )
    }
}
