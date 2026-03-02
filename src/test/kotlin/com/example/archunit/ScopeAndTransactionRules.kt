package com.example.archunit

import com.example.shared.architecture.ApplicationService
import com.example.shared.architecture.DataService
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
    val `only SecurityContext should be RequestScoped` =
        classes()
            .that().areAnnotatedWith(RequestScoped::class.java)
            .should().haveSimpleName("SecurityContext")
            .because("Only SecurityContext should be @RequestScoped - all other beans should be @ApplicationScoped")

    @ArchTest
    val `services should use DDD stereotype annotations` =
        classes()
            .that().haveSimpleNameEndingWith("Service")
            .and().resideOutsideOfPackage("..shared..")
            .and().resideOutsideOfPackage("..api..")
            .and().resideOutsideOfPackage("..jooq..")
            .and().resideOutsideOfPackage("..exceptions..")
            .should().beAnnotatedWith(ApplicationService::class.java)
            .orShould().beAnnotatedWith(DataService::class.java)
            .because("Service classes should be annotated with @ApplicationService or @DataService")

    @ArchTest
    val `only application services can have transactional methods` =
        methods()
            .that().areAnnotatedWith(Transactional::class.java)
            .should().beDeclaredInClassesThat().areAnnotatedWith(ApplicationService::class.java)
            .because("@Transactional should only be on @ApplicationService methods, not Repositories or Resources")

    @ArchTest
    val `repositories should not be transactional` =
        classes()
            .that().haveSimpleNameEndingWith("Repository")
            .and().areNotInterfaces()
            .should().notBeAnnotatedWith(Transactional::class.java)
            .because("Repositories should not manage transactions - @ApplicationService does that")

    @ArchTest
    val `data services should not be transactional` =
        classes()
            .that().areAnnotatedWith(DataService::class.java)
            .should().notBeAnnotatedWith(Transactional::class.java)
            .because("@DataService classes are read-only and should not manage transactions")
}
