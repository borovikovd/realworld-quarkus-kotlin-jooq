package com.example.archunit

import com.example.shared.domain.Queries
import com.example.shared.domain.Repository
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import jakarta.ws.rs.Path

@AnalyzeClasses(
    packages = ["com.example"],
    importOptions = [ImportOption.DoNotIncludeTests::class, ImportOption.DoNotIncludeJars::class],
)
class NamingConventionRules {
    @ArchTest
    val `repository implementations should be named Jooq-Repository` =
        classes()
            .that().implement(Repository::class.java)
            .and().areNotInterfaces()
            .should().haveSimpleNameStartingWith("Jooq")
            .andShould().haveSimpleNameEndingWith("Repository")
            .because("Repository implementations should follow Jooq*Repository naming pattern")

    @ArchTest
    val `query implementations should be named Jooq-Queries` =
        classes()
            .that().implement(Queries::class.java)
            .and().areNotInterfaces()
            .should().haveSimpleNameStartingWith("Jooq")
            .andShould().haveSimpleNameEndingWith("Queries")
            .because("Query implementations should follow Jooq*Queries naming pattern")

    @ArchTest
    val `resources should end with Resource` =
        classes()
            .that().areAnnotatedWith(Path::class.java)
            .should().haveSimpleNameEndingWith("Resource")
            .because("REST endpoints should be named *Resource")

    @ArchTest
    val `services should end with Service` =
        classes()
            .that().resideInAnyPackage("..article", "..user", "..comment", "..profile")
            .and().haveSimpleNameContaining("Service")
            .should().haveSimpleNameEndingWith("Service")
            .because("Service classes should be named *Service")

    @ArchTest
    val `exceptions should end with Exception` =
        classes()
            .that().areAssignableTo(Exception::class.java)
            .and().resideInAPackage("..exceptions..")
            .and().haveSimpleNameNotContaining("Mapper")
            .should().haveSimpleNameEndingWith("Exception")
            .because("Domain exceptions should be named *Exception")

    @ArchTest
    val `exception mappers should end with ExceptionMapper` =
        classes()
            .that().resideInAPackage("..exceptions..")
            .and().haveSimpleNameContaining("Mapper")
            .should().haveSimpleNameEndingWith("ExceptionMapper")
            .because("JAX-RS exception mappers should be named *ExceptionMapper")
}
