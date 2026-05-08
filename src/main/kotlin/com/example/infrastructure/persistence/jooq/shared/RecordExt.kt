package com.example.infrastructure.persistence.jooq.shared

import org.jooq.Field
import org.jooq.Record

internal fun <T : Any> Record.req(field: Field<T?>): T =
    get(field) ?: error("Schema drift: column ${field.name} was NULL but should be NOT NULL")

internal fun <T : Any> Record.req(
    name: String,
    type: Class<T>,
): T = get(name, type) ?: error("Schema drift: alias '$name' was NULL but should be NOT NULL")
