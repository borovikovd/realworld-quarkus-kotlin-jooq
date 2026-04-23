package com.example.archunit

import com.example.domain.Repository
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes

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
    val `resources should end with Resource` =
        classes()
            .that().haveSimpleNameEndingWith("Resource")
            .and().resideOutsideOfPackage("..shared..")
            .should().beAnnotatedWith(jakarta.enterprise.context.ApplicationScoped::class.java)
            .because("REST resource classes should be @ApplicationScoped CDI beans")

    @ArchTest
    val `application commands should end with Commands` =
        classes()
            .that().resideInAPackage("..application.command..")
            .and().areNotInterfaces()
            .and().areTopLevelClasses()
            .should().haveSimpleNameEndingWith("Commands")
            .because("Command-side application classes should be named *Commands")

    @ArchTest
    val `exceptions should end with Exception` =
        classes()
            .that().areAssignableTo(Exception::class.java)
            .and().resideInAPackage("..domain.exception..")
            .and().haveSimpleNameNotContaining("Mapper")
            .should().haveSimpleNameEndingWith("Exception")
            .because("Domain exceptions should be named *Exception")

    @ArchTest
    val `exception mappers should end with ExceptionMapper` =
        classes()
            .that().resideInAnyPackage("..exceptions..", "..infrastructure.web..")
            .and().haveSimpleNameContaining("Mapper")
            .should().haveSimpleNameEndingWith("ExceptionMapper")
            .because("JAX-RS exception mappers should be named *ExceptionMapper")

    @ArchTest
    val `queries should end with Queries` =
        classes()
            .that().haveSimpleNameContaining("Queries")
            .and().resideOutsideOfPackage("..shared..")
            .should().haveSimpleNameEndingWith("Queries")
            .because("Query classes should be named *Queries")
}
