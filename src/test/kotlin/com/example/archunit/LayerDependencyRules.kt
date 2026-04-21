package com.example.archunit

import com.example.domain.shared.Entity
import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses

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
            .layer("presentation").definedBy("com.example.presentation..")
            .whereLayer("presentation").mayNotBeAccessedByAnyLayer()
            .whereLayer("infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("application").mayOnlyBeAccessedByLayers("infrastructure", "presentation")
            .whereLayer("domain").mayOnlyBeAccessedByLayers("application", "infrastructure", "presentation")
            .because("Dependencies flow inward: presentation -> application -> domain; infrastructure adapts domain ports")

    @ArchTest
    val `services should not use OpenAPI DTOs` =
        noClasses()
            .that().haveSimpleNameEndingWith("Service")
            .should().dependOnClassesThat().resideInAPackage("com.example.api..")
            .because("Services work with domain entities, not API DTOs - only Resources handle DTO mapping")

    @ArchTest
    val `domain entities should not use jOOQ or JAX-RS` =
        classes()
            .that().areAssignableTo(Entity::class.java)
            .and(
                object : DescribedPredicate<JavaClass>("not infrastructure classes") {
                    override fun test(input: JavaClass): Boolean {
                        val name = input.simpleName
                        return !name.contains("Repository") &&
                            !name.contains("Service") &&
                            !name.contains("Resource") &&
                            !name.contains("Queries") &&
                            !name.contains("ViewReader") &&
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
                object : DescribedPredicate<JavaClass>("not Jooq/Queries/ViewReader classes and not in jooq package") {
                    override fun test(input: JavaClass): Boolean =
                        !input.fullName.contains("Jooq") &&
                            !input.fullName.contains("Queries") &&
                            !input.fullName.contains("ViewReader") &&
                            !input.packageName.contains(".jooq") &&
                            !input.simpleName.contains("Test") &&
                            !input.simpleName.contains("Base") &&
                            !input.simpleName.contains("Fixture")
                },
            )
            .should().dependOnClassesThat().resideInAPackage("com.example.jooq..")
            .because("Only Jooq*Repository and *ViewReader should access jOOQ generated code")
}
