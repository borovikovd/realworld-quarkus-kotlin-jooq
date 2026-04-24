package com.example.archunit

import com.example.application.outport.Repository
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
    val `inbound command ports should be interfaces ending with Commands` =
        classes()
            .that().resideInAPackage("..application.inport.command..")
            .and().areTopLevelClasses()
            .should().beInterfaces()
            .andShould().haveSimpleNameEndingWith("Commands")
            .because("Inbound command ports are contracts — interfaces named *Commands")

    @ArchTest
    val `inbound query ports should be interfaces ending with Queries` =
        classes()
            .that().resideInAPackage("..application.inport.query")
            .and().areTopLevelClasses()
            .should().beInterfaces()
            .andShould().haveSimpleNameEndingWith("Queries")
            .because("Inbound query ports are contracts — interfaces named *Queries")

    @ArchTest
    val `application services should be concrete and end with ApplicationService` =
        classes()
            .that().resideInAPackage("..application.service..")
            .and().areTopLevelClasses()
            .should().notBeInterfaces()
            .andShould().haveSimpleNameEndingWith("ApplicationService")
            .because("Use-case implementations are concrete classes named *ApplicationService")

    @ArchTest
    val `outbound ports should be interfaces` =
        classes()
            .that().resideInAPackage("..application.outport..")
            .and().areTopLevelClasses()
            .should().beInterfaces()
            .because("Outbound ports are contracts implemented by infrastructure adapters")

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
}
