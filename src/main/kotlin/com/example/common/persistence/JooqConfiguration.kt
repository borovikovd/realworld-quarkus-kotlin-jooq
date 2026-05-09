package com.example.common.persistence

import io.quarkiverse.jooq.runtime.JooqCustomContext
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Named
import org.jooq.Configuration
import org.jooq.conf.RenderQuotedNames

@ApplicationScoped
@Named("com.example.common.persistence.JooqConfiguration")
class JooqConfiguration : JooqCustomContext {
    override fun apply(configuration: Configuration) {
        configuration
            .settings()
            .withRenderSchema(true)
            .withRenderQuotedNames(RenderQuotedNames.EXPLICIT_DEFAULT_QUOTED)
    }
}
