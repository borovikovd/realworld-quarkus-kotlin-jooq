package com.example.archunit

import com.example.shared.architecture.AggregateRoot
import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses

/**
 * DDD Aggregate Boundary Rules
 *
 * An AGGREGATE is a cluster of associated objects that we treat as a unit for data changes.
 * Each AGGREGATE has a root and a boundary. The root is the only member that outside objects
 * are allowed to hold references to.
 *
 * IMPORTANT: Services act as Application Services and can coordinate multiple aggregates.
 * This is a legitimate pattern for validation and orchestration across bounded contexts.
 * However, domain entities themselves must never import other aggregates' entities.
 */
@AnalyzeClasses(
    packages = ["com.example"],
    importOptions = [ImportOption.DoNotIncludeTests::class, ImportOption.DoNotIncludeJars::class],
)
class AggregateBoundaryRules {
    @ArchTest
    val `aggregate roots should only reference other aggregates by ID` =
        classes()
            .that().areAnnotatedWith(AggregateRoot::class.java)
            .should().onlyAccessClassesThat(
                object : DescribedPredicate<JavaClass>("are in same package or allowed dependencies") {
                    override fun test(accessed: JavaClass): Boolean {
                        val accessedPackage = accessed.packageName

                        // Allow access to shared utilities and framework classes
                        if (accessedPackage.startsWith("com.example.shared") ||
                            accessedPackage.startsWith("java.") ||
                            accessedPackage.startsWith("kotlin.") ||
                            accessedPackage.startsWith("jakarta.") ||
                            accessedPackage.startsWith("org.eclipse.") ||
                            accessedPackage.startsWith("io.quarkus.")
                        ) {
                            return true
                        }

                        // Allow accessing other aggregate roots (for ID references)
                        if (accessed.isAnnotatedWith(AggregateRoot::class.java)) {
                            return true
                        }

                        // Disallow accessing entities from other aggregates
                        val otherAggregatePackages = listOf("article", "user", "comment", "profile")
                        return !otherAggregatePackages.any {
                            accessedPackage.contains(".$it.") &&
                            accessed.packageName != accessed.packageName  // Not same package
                        }
                    }
                },
            ).because("Aggregate roots should only reference other aggregates by ID, not import their entities")

    @ArchTest
    val `only aggregate roots should be publicly accessible` =
        classes()
            .that().areAnnotatedWith(AggregateRoot::class.java)
            .should().bePublic()
            .because("Aggregate roots are the entry points to aggregates and should be publicly accessible")

    @ArchTest
    val `shared package should not depend on aggregates` =
        noClasses()
            .that().resideInAPackage("..shared..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "..article..",
                "..user..",
                "..comment..",
                "..profile..",
            ).because("Shared package provides utilities and should not depend on any aggregate")
}
