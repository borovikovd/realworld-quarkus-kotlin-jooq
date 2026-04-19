package com.example.archunit

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import org.jooq.DSLContext

@AnalyzeClasses(
    packages = ["com.example"],
    importOptions = [ImportOption.DoNotIncludeTests::class, ImportOption.DoNotIncludeJars::class],
)
class TechnologyBoundaryRules {
    @ArchTest
    val `only Resources can import OpenAPI generated code` =
        noClasses()
            .that(
                object : DescribedPredicate<JavaClass>("not in api package, not Resource, and not Queries") {
                    override fun test(input: JavaClass): Boolean {
                        if (input.packageName.contains(".api")) return false
                        if (input.simpleName.endsWith("Resource")) return false
                        if (input.simpleName.endsWith("ResourceKt")) return false
                        if (input.fullName.contains("Queries")) return false
                        return true
                    }
                },
            )
            .and().resideOutsideOfPackage("..test..")
            .and().haveSimpleNameNotContaining("Test")
            .and().haveSimpleNameNotContaining("Fixture")
            .and().haveSimpleNameNotContaining("Builder")
            .should().dependOnClassesThat().resideInAPackage("com.example.api..")
            .because("Only Resources should use OpenAPI DTOs")

    @ArchTest
    val `only infrastructure security adapters can import JWT` =
        noClasses()
            .that().doNotHaveSimpleName("JwtService")
            .and().doNotHaveSimpleName("JwtCurrentUser")
            .and().doNotHaveSimpleName("JwtTokenIssuer")
            .and().doNotHaveSimpleName("JwtTokenVerifier")
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.eclipse.microprofile.jwt..",
                "io.smallrye.jwt..",
            ).because("JWT token handling belongs to the infrastructure.security adapters only")

    @ArchTest
    val `only Jooq repositories and ReadServices can inject DSLContext` =
        fields()
            .that().haveRawType(DSLContext::class.java)
            .should().beDeclaredInClassesThat().haveSimpleNameStartingWith("Jooq")
            .orShould().beDeclaredInClassesThat().haveSimpleNameEndingWith("Queries")
            .orShould().beDeclaredInClassesThat().haveSimpleNameEndingWith("ReadService")
            .orShould().beDeclaredInClassesThat().haveSimpleNameContaining("Test")
            .because("Only Jooq*Repository and *ReadService should have direct access to jOOQ DSLContext")

    @ArchTest
    val `services should not use JAX-RS Response` =
        noClasses()
            .that().haveSimpleNameEndingWith("Service")
            .should().dependOnClassesThat().haveSimpleName("Response")
            .because("Services should not construct JAX-RS Response objects - that's the Resource layer's job")
}
