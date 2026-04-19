package com.example.archunit

import com.example.domain.shared.Repository
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
    val `services should end with Service` =
        classes()
            .that().haveSimpleNameContaining("Service")
            .and().resideOutsideOfPackage("..shared..")
            .and().resideOutsideOfPackage("..api..")
            .and().resideOutsideOfPackage("..jooq..")
            .and().resideOutsideOfPackage("..exceptions..")
            .should().haveSimpleNameEndingWith("Service")
            .because("Service classes should be named *Service")

    @ArchTest
    val `exceptions should end with Exception` =
        classes()
            .that().areAssignableTo(Exception::class.java)
            .and().resideInAnyPackage("..exceptions..", "..domain.shared..")
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
    val `read services should end with ReadService` =
        classes()
            .that().haveSimpleNameContaining("ReadService")
            .and().resideOutsideOfPackage("..shared..")
            .should().haveSimpleNameEndingWith("ReadService")
            .because("Read service classes should be named *ReadService")
}
