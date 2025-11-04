package com.example.archunit

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
 *
 * KNOWN VIOLATIONS (to be fixed in Phase 2):
 * 1. Profile package manages followers - should be part of User aggregate
 * 2. ArticleQueries directly accesses FOLLOWERS table - should use UserQueries
 */
@AnalyzeClasses(
    packages = ["com.example"],
    importOptions = [ImportOption.DoNotIncludeTests::class, ImportOption.DoNotIncludeJars::class],
)
class AggregateBoundaryRules {
    @ArchTest
    val `user aggregate domain entities should not depend on other aggregates` =
        classes()
            .that().resideInAPackage("..user..")
            .and().haveSimpleNameNotContaining("Service")
            .and().haveSimpleNameNotContaining("Repository")
            .and().haveSimpleNameNotContaining("Queries")
            .and().haveSimpleNameNotContaining("Resource")
            .and().haveSimpleNameNotStartingWith("Jooq")
            .should().onlyAccessClassesThat().resideInAnyPackage(
                "..user..",
                "..shared..",
                "java..",
                "kotlin..",
                "jakarta..",
                "org.eclipse..",
                "io.quarkus..",
                "org.jooq..",
                "com.example.jooq..",
                "com.example.api..",
            ).because("User domain entities should not import domain entities from other aggregates")

    @ArchTest
    val `article aggregate domain entities should not depend on other aggregates` =
        classes()
            .that().resideInAPackage("..article..")
            .and().haveSimpleNameNotContaining("Service")
            .and().haveSimpleNameNotContaining("Repository")
            .and().haveSimpleNameNotContaining("Queries")
            .and().haveSimpleNameNotContaining("Resource")
            .and().haveSimpleNameNotStartingWith("Jooq")
            .should().onlyAccessClassesThat().resideInAnyPackage(
                "..article..",
                "..shared..",
                "java..",
                "kotlin..",
                "jakarta..",
                "org.eclipse..",
                "io.quarkus..",
                "org.jooq..",
                "com.example.jooq..",
                "com.example.api..",
            ).because("Article domain entities should not import domain entities from other aggregates")

    @ArchTest
    val `comment aggregate domain entities should not depend on other aggregates` =
        classes()
            .that().resideInAPackage("..comment..")
            .and().haveSimpleNameNotContaining("Service")
            .and().haveSimpleNameNotContaining("Repository")
            .and().haveSimpleNameNotContaining("Queries")
            .and().haveSimpleNameNotContaining("Resource")
            .and().haveSimpleNameNotStartingWith("Jooq")
            .should().onlyAccessClassesThat().resideInAnyPackage(
                "..comment..",
                "..shared..",
                "java..",
                "kotlin..",
                "jakarta..",
                "org.eclipse..",
                "io.quarkus..",
                "org.jooq..",
                "com.example.jooq..",
                "com.example.api..",
            ).because("Comment domain entities should not import domain entities from other aggregates")

    @ArchTest
    val `profile aggregate domain entities should not depend on other aggregates` =
        classes()
            .that().resideInAPackage("..profile..")
            .and().haveSimpleNameNotContaining("Service")
            .and().haveSimpleNameNotContaining("Repository")
            .and().haveSimpleNameNotContaining("Queries")
            .and().haveSimpleNameNotContaining("Resource")
            .and().haveSimpleNameNotStartingWith("Jooq")
            .should().onlyAccessClassesThat().resideInAnyPackage(
                "..profile..",
                "..shared..",
                "java..",
                "kotlin..",
                "jakarta..",
                "org.eclipse..",
                "io.quarkus..",
                "org.jooq..",
                "com.example.jooq..",
                "com.example.api..",
            ).because("Profile domain entities should not import domain entities from other aggregates")

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
