package com.example.archunit

import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import jakarta.ws.rs.Path

@AnalyzeClasses(
    packages = ["com.example"],
    importOptions = [ImportOption.DoNotIncludeTests::class, ImportOption.DoNotIncludeJars::class],
)
class LayerDependencyRules {
    @ArchTest
    val `resources should only access services and queries` =
        classes()
            .that().areAnnotatedWith(Path::class.java)
            .should().onlyAccessClassesThat().resideInAnyPackage(
                "..service..",
                "..query..",
                "..queries..",
                "..repository..",
                "..api..",
                "..shared..",
                "..article",
                "..user",
                "..comment",
                "..profile",
                "java..",
                "kotlin..",
                "jakarta..",
                "org.eclipse..",
                "io.quarkus..",
            ).because("Resources should orchestrate services and queries, not directly manipulate domain entities")

    @ArchTest
    val `services should not use OpenAPI DTOs` =
        noClasses()
            .that().haveSimpleNameEndingWith("Service")
            .should().dependOnClassesThat().resideInAPackage("com.example.api..")
            .because("Services work with domain entities, not API DTOs - Resources handle DTO mapping")

    @ArchTest
    val `domain entities should not use jOOQ or JAX-RS` =
        classes()
            .that().resideInAnyPackage("..article", "..user", "..comment", "..profile")
            .and().haveSimpleNameNotContaining("Repository")
            .and().haveSimpleNameNotContaining("Service")
            .and().haveSimpleNameNotContaining("Resource")
            .and().haveSimpleNameNotContaining("Queries")
            .and().haveSimpleNameNotStartingWith("Jooq")
            .and().resideOutsideOfPackage("..jooq..") // Exclude jOOQ generated code
            .and(
                object : DescribedPredicate<JavaClass>("full name not containing Jooq") {
                    override fun test(input: JavaClass): Boolean = !input.fullName.contains("Jooq")
                },
            )
            .should().onlyDependOnClassesThat().resideOutsideOfPackages(
                "org.jooq..",
                "jakarta.ws.rs..",
            ).because("Domain entities should not depend on jOOQ or JAX-RS")

    @ArchTest
    val `only jooq classes can import jooq generated code` =
        noClasses()
            .that(
                object : DescribedPredicate<JavaClass>("full name containing Jooq or in jooq package") {
                    override fun test(input: JavaClass): Boolean =
                        !input.fullName.contains("Jooq") && !input.packageName.contains(".jooq")
                },
            )
            .and().haveSimpleNameNotContaining("Test")
            .and().haveSimpleNameNotContaining("Base")
            .and().haveSimpleNameNotContaining("Fixture")
            .and().resideOutsideOfPackage("..test..")
            .should().dependOnClassesThat().resideInAPackage("com.example.jooq..")
            .because("Only Jooq*Repository and Jooq*Queries should access jOOQ generated code")
}
