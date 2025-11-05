package com.example.archunit

import com.example.shared.domain.Queries
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import jakarta.ws.rs.Path
import org.jooq.DSLContext

@AnalyzeClasses(
    packages = ["com.example"],
    importOptions = [ImportOption.DoNotIncludeTests::class, ImportOption.DoNotIncludeJars::class],
)
class TechnologyBoundaryRules {
    @ArchTest
    val `only Resources and Queries can import OpenAPI generated code` =
        noClasses()
            .that(
                object : DescribedPredicate<JavaClass>("not in api package, not Resource, and not Queries") {
                    override fun test(input: JavaClass): Boolean {
                        // Exclude API package itself
                        if (input.packageName.contains(".api")) return false

                        // Exclude Resources (annotated with @Path)
                        if (input.isAnnotatedWith(Path::class.java)) return false

                        // Exclude Queries (implements Queries interface or contains "Queries" in name)
                        if (input.isAssignableTo(Queries::class.java)) return false
                        if (input.fullName.contains("Queries")) return false

                        // This class should be checked
                        return true
                    }
                },
            )
            .and().resideOutsideOfPackage("..test..")
            .and().haveSimpleNameNotContaining("Test")
            .and().haveSimpleNameNotContaining("Fixture")
            .and().haveSimpleNameNotContaining("Builder")
            .should().dependOnClassesThat().resideInAPackage("com.example.api..")
            .because("Only Resources and Queries should use OpenAPI DTOs (CQRS-lite pattern)")

    @ArchTest
    val `only SecurityContext can import JWT` =
        noClasses()
            .that().doNotHaveSimpleName("SecurityContext")
            .and().doNotHaveSimpleName("JwtService")
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.eclipse.microprofile.jwt..",
            ).because("JWT token handling should be isolated to SecurityContext and JwtService")

    @ArchTest
    val `only Jooq classes can inject DSLContext` =
        fields()
            .that().haveRawType(DSLContext::class.java)
            .should().beDeclaredInClassesThat().haveSimpleNameStartingWith("Jooq")
            .orShould().beDeclaredInClassesThat().haveSimpleNameContaining("Test")
            .because("Only Jooq*Repository and Jooq*Queries should have direct access to jOOQ DSLContext")

    @ArchTest
    val `services should not use JAX-RS Response` =
        noClasses()
            .that().haveSimpleNameEndingWith("Service")
            .should().dependOnClassesThat().haveSimpleName("Response")
            .because("Services should not construct JAX-RS Response objects - that's the Resource layer's job")
}
