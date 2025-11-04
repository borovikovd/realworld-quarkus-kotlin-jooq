package com.example.archunit

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.RequestScoped
import jakarta.transaction.Transactional

@AnalyzeClasses(
    packages = ["com.example"],
    importOptions = [ImportOption.DoNotIncludeTests::class, ImportOption.DoNotIncludeJars::class],
)
class ScopeAndTransactionRules {
    @ArchTest
    val `only SecurityContext should be RequestScoped` =
        classes()
            .that().areAnnotatedWith(RequestScoped::class.java)
            .should().haveSimpleName("SecurityContext")
            .because("Only SecurityContext should be @RequestScoped - all other beans should be @ApplicationScoped")

    @ArchTest
    val `services should be ApplicationScoped` =
        classes()
            .that().haveSimpleNameEndingWith("Service")
            .and().resideInAnyPackage("..article", "..user", "..comment", "..profile")
            .should().beAnnotatedWith(ApplicationScoped::class.java)
            .because("Service classes should be @ApplicationScoped singletons")

    @ArchTest
    val `only services can have transactional methods` =
        methods()
            .that().areAnnotatedWith(Transactional::class.java)
            .should().beDeclaredInClassesThat().haveSimpleNameEndingWith("Service")
            .because("@Transactional should only be on Service methods, not Repositories or Resources")

    @ArchTest
    val `repositories should not be transactional` =
        classes()
            .that().haveSimpleNameEndingWith("Repository")
            .and().areNotInterfaces()
            .should().notBeAnnotatedWith(Transactional::class.java)
            .because("Repositories should not manage transactions - Services do that")

    @ArchTest
    val `queries should not be transactional` =
        classes()
            .that().haveSimpleNameEndingWith("Queries")
            .and().areNotInterfaces()
            .should().notBeAnnotatedWith(Transactional::class.java)
            .because("Query classes should not manage transactions")
}
