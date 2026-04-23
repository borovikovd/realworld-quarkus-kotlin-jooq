package com.example.archunit

import com.example.domain.Entity
import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.Architectures.layeredArchitecture

@AnalyzeClasses(
    packages = ["com.example"],
    importOptions = [ImportOption.DoNotIncludeTests::class, ImportOption.DoNotIncludeJars::class],
)
class LayerDependencyRules {
    @ArchTest
    val `layer dependencies respect hexagonal direction` =
        layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("domain").definedBy("com.example.domain..")
            .layer("application").definedBy("com.example.application..")
            .layer("infrastructure").definedBy("com.example.infrastructure..")
            .whereLayer("infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("application").mayOnlyBeAccessedByLayers("infrastructure")
            .whereLayer("domain").mayOnlyBeAccessedByLayers("application", "infrastructure")
            .because("Dependencies flow inward: infrastructure -> application -> domain")

    @ArchTest
    val `application classes should not use OpenAPI DTOs` =
        noClasses()
            .that().resideInAPackage("com.example.application..")
            .should().dependOnClassesThat().resideInAPackage("com.example.api..")
            .because("Application layer works with domain types - only Resources handle API DTO mapping")

    @ArchTest
    val `domain entities should not use jOOQ or JAX-RS` =
        classes()
            .that().areAssignableTo(Entity::class.java)
            .and(
                object : DescribedPredicate<JavaClass>("not infrastructure classes") {
                    override fun test(input: JavaClass): Boolean {
                        val name = input.simpleName
                        return !name.contains("Repository") &&
                            !name.contains("Resource") &&
                            !name.contains("Queries") &&
                            !name.contains("Commands") &&
                            !name.startsWith("Jooq") &&
                            !input.fullName.contains("Jooq") &&
                            !input.packageName.contains(".jooq")
                    }
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
                object : DescribedPredicate<JavaClass>("not Jooq classes and not in jooq package") {
                    override fun test(input: JavaClass): Boolean =
                        !input.fullName.contains("Jooq") &&
                            !input.packageName.contains(".jooq") &&
                            !input.simpleName.contains("Test") &&
                            !input.simpleName.contains("Base") &&
                            !input.simpleName.contains("Fixture")
                },
            )
            .should().dependOnClassesThat().resideInAPackage("com.example.jooq..")
            .because("Only Jooq* adapters should access jOOQ generated code")
}
