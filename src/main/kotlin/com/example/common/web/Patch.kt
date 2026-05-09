package com.example.common.web

import com.fasterxml.jackson.core.JsonParser
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
