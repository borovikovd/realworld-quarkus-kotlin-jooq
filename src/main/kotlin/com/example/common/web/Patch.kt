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

    // Absent fields and explicit JSON null both land here; treat both as "no change".
    // Jackson calls this both for absent fields (when KotlinModule can't apply the Kotlin default
    // because a custom deserializer is registered) and for explicit JSON null tokens.
    override fun getNullValue(ctxt: DeserializationContext?): Patch<List<String>> = Patch.Absent
}
