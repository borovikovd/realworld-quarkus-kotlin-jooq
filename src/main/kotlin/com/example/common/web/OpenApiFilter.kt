package com.example.common.web

import org.eclipse.microprofile.openapi.OASFactory
import org.eclipse.microprofile.openapi.OASFilter
import org.eclipse.microprofile.openapi.models.Operation

// Smallrye-OpenAPI auto-injects a 400 for any @Valid parameter, but this app returns 422
// for bean-validation failures (see ConstraintViolationExceptionMapper). Swap 400 -> 422
// only when the operation hasn't already declared 422 explicitly — that lets an endpoint
// opt out by declaring its own 422 (and keep any genuine 400 like "malformed JSON").
class OpenApiFilter : OASFilter {
    override fun filterOperation(operation: Operation): Operation {
        val responses = operation.responses
        val codes = responses?.getAPIResponses()
        if (codes != null && "400" in codes && "422" !in codes) {
            responses.removeAPIResponse("400")
            responses.addAPIResponse(
                "422",
                OASFactory.createAPIResponse().description("Unprocessable Entity"),
            )
        }
        return operation
    }
}
