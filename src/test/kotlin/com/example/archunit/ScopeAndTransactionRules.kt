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
    val `transactional methods belong on the command side` =
        methods()
            .that().areAnnotatedWith(Transactional::class.java)
            .should().beDeclaredInClassesThat().resideInAPackage("..application.command..")
            .because("@Transactional belongs to command-side application classes only")

    @ArchTest
    val `repositories should not be transactional` =
        classes()
            .that().haveSimpleNameEndingWith("Repository")
            .and().areNotInterfaces()
            .should().notBeAnnotatedWith(Transactional::class.java)
            .because("Repositories should not manage transactions - commands do that")

    @ArchTest
    val `queries should not be transactional` =
        classes()
            .that().haveSimpleNameEndingWith("Queries")
            .and().areNotInterfaces()
            .should().notBeAnnotatedWith(Transactional::class.java)
            .because("Queries are read-only and should not manage transactions")
}
