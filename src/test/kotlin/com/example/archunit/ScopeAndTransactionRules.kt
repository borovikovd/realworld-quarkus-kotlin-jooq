package com.example.archunit

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods
import jakarta.transaction.Transactional

@AnalyzeClasses(
    packages = ["com.example"],
    importOptions = [ImportOption.DoNotIncludeTests::class, ImportOption.DoNotIncludeJars::class],
)
class ScopeAndTransactionRules {
    @ArchTest
    val `transactional methods belong on application services` =
        methods()
            .that().areAnnotatedWith(Transactional::class.java)
            .should().beDeclaredInClassesThat().resideInAPackage("..application.service..")
            .because("@Transactional belongs to application service implementations only")

    @ArchTest
    val `repositories should not be transactional` =
        classes()
            .that().haveSimpleNameEndingWith("Repository")
            .and().areNotInterfaces()
            .should().notBeAnnotatedWith(Transactional::class.java)
            .because("Repositories should not manage transactions - application services do that")
}
