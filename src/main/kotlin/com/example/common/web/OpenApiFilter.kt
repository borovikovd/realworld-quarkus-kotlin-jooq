package com.example.common.web

import org.eclipse.microprofile.openapi.OASFactory
import org.eclipse.microprofile.openapi.OASFilter
import org.eclipse.microprofile.openapi.models.Operation

class OpenApiFilter : OASFilter {
    override fun filterOperation(operation: Operation): Operation {
        val responses = operation.responses
        val codes = responses?.getAPIResponses()
        if (responses != null && codes != null && "400" in codes) {
            responses.removeAPIResponse("400")
            if ("422" !in codes) {
                responses.addAPIResponse(
                    "422",
                    OASFactory.createAPIResponse().description("Unprocessable Entity"),
                )
            }
        }
        return operation
    }
}
