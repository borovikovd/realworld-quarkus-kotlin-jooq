package com.example.common.web

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer

sealed class Patch<out T> {
    data object Absent : Patch<Nothing>()

    data class Present<out T>(
        val value: T?,
    ) : Patch<T>()
}

/** Absent → [default]; Present → value (including null). For nullable fields where null is meaningful. */
fun <T> Patch<T>.orElseNullable(default: T?): T? =
    when (this) {
        is Patch.Absent -> default
        is Patch.Present -> value
    }

class StringPatchDeserializer : StdDeserializer<Patch<String>>(Patch::class.java) {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): Patch<String> = Patch.Present(p.text)

    override fun getNullValue(ctxt: DeserializationContext?): Patch<String> = Patch.Present(null)
}

class ListStringPatchDeserializer : StdDeserializer<Patch<List<String>>>(Patch::class.java) {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): Patch<List<String>> = Patch.Present(p.readValueAs(object : TypeReference<List<String>>() {}))

    // null in JSON ("tagList": null) treated as absent — keep existing tags
    override fun getNullValue(ctxt: DeserializationContext?): Patch<List<String>> = Patch.Absent
}
