package com.example.archunit

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods
import jakarta.enterprise.context.RequestScoped
import jakarta.transaction.Transactional

@AnalyzeClasses(
    packages = ["com.example"],
    importOptions = [ImportOption.DoNotIncludeTests::class, ImportOption.DoNotIncludeJars::class],
)
class ScopeAndTransactionRules {
    @ArchTest
    val `only JwtCurrentUser should be RequestScoped` =
        classes()
            .that().areAnnotatedWith(RequestScoped::class.java)
            .should().haveSimpleName("JwtCurrentUser")
            .because(
                "Only JwtCurrentUser should be @RequestScoped - " +
                    "all other beans should be @ApplicationScoped",
            )

    @ArchTest
    val `transactional methods belong in the application layer` =
        methods()
            .that().areAnnotatedWith(Transactional::class.java)
            .should().beDeclaredInClassesThat().resideInAPackage("..application..")
            .because("@Transactional is a command-side concern and belongs to application services only")

    @ArchTest
    val `repositories should not be transactional` =
        classes()
            .that().haveSimpleNameEndingWith("Repository")
            .and().areNotInterfaces()
            .should().notBeAnnotatedWith(Transactional::class.java)
            .because("Repositories should not manage transactions - application services do that")

    @ArchTest
    val `read services should not be transactional` =
        classes()
            .that().haveSimpleNameEndingWith("ReadService")
            .and().areNotInterfaces()
            .should().notBeAnnotatedWith(Transactional::class.java)
            .because("Read services are read-only and should not manage transactions")
}
