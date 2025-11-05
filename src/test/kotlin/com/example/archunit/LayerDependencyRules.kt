package com.example.archunit

import com.example.shared.domain.Entity
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
    companion object {
        private fun isFrameworkPackage(pkg: String): Boolean =
            pkg.startsWith("java.") ||
                pkg.startsWith("kotlin.") ||
                pkg.startsWith("jakarta.") ||
                pkg.startsWith("org.eclipse.") ||
                pkg.startsWith("io.quarkus.") ||
                pkg.startsWith("com.example.shared") ||
                pkg.startsWith("com.example.api")
    }

    @ArchTest
    val `services should not use OpenAPI DTOs` =
        noClasses()
            .that().haveSimpleNameEndingWith("Service")
            .should().dependOnClassesThat().resideInAPackage("com.example.api..")
            .because("Services work with domain entities, not API DTOs - Resources handle DTO mapping")

    @ArchTest
    val `domain entities should not use jOOQ or JAX-RS` =
        classes()
            .that().implement(Entity::class.java)
            .and(
                object : DescribedPredicate<JavaClass>("not infrastructure classes") {
                    override fun test(input: JavaClass): Boolean {
                        val name = input.simpleName
                        // Exclude infrastructure classes (repositories, services, queries, resources)
                        return !name.contains("Repository") &&
                            !name.contains("Service") &&
                            !name.contains("Resource") &&
                            !name.contains("Queries") &&
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
            .because("Only Jooq*Repository and Jooq*Queries should access jOOQ generated code")
}
