package com.example.common.web

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer

sealed class Patch<out T> {
    data object Absent : Patch<Nothing>()

    data object Null : Patch<Nothing>()

    data class Value<out T>(
        val value: T,
    ) : Patch<T>()
}

class StringPatchDeserializer : StdDeserializer<Patch<String>>(Patch::class.java) {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): Patch<String> = Patch.Value(p.text)

    override fun getNullValue(ctxt: DeserializationContext?): Patch<String> = Patch.Null
}

class ListStringPatchDeserializer : StdDeserializer<Patch<List<String>>>(Patch::class.java) {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): Patch<List<String>> = Patch.Value(p.readValueAs(object : TypeReference<List<String>>() {}))

    override fun getNullValue(ctxt: DeserializationContext?): Patch<List<String>> = Patch.Null
}
