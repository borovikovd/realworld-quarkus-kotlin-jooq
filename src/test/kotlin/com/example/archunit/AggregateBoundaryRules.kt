package com.example.archunit

import com.example.shared.architecture.AggregateRoot
import com.example.shared.domain.Entity
import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
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
 */
@AnalyzeClasses(
    packages = ["com.example"],
    importOptions = [ImportOption.DoNotIncludeTests::class, ImportOption.DoNotIncludeJars::class],
)
class AggregateBoundaryRules {
    companion object {
        /**
         * Returns the aggregate package for a given class (e.g., "com.example.article").
         * Returns null if the class is not in an aggregate package.
         */
        private fun getAggregatePackage(javaClass: JavaClass): String? {
            val pkg = javaClass.packageName
            // Match packages like com.example.article, com.example.user, etc.
            val match = Regex("(com\\.example\\.\\w+)(?:\\..*)?").find(pkg)
            return match?.groupValues?.get(1)?.takeIf {
                // Only consider it an aggregate if it's not in shared/api/jooq/exceptions
                !it.endsWith(".shared") &&
                    !it.endsWith(".api") &&
                    !it.endsWith(".jooq") &&
                    !it.endsWith(".exceptions")
            }
        }

        /**
         * Checks if a class is part of an aggregate (not in shared/framework packages).
         */
        private fun isInAggregate(javaClass: JavaClass): Boolean = getAggregatePackage(javaClass) != null

        /**
         * Checks if two classes are in the same aggregate.
         */
        private fun inSameAggregate(
            class1: JavaClass,
            class2: JavaClass,
        ): Boolean = getAggregatePackage(class1) == getAggregatePackage(class2)

        /**
         * Checks if a class is a framework/library class that's allowed everywhere.
         */
        private fun isFrameworkClass(javaClass: JavaClass): Boolean {
            val pkg = javaClass.packageName
            return pkg.startsWith("java.") ||
                pkg.startsWith("kotlin.") ||
                pkg.startsWith("jakarta.") ||
                pkg.startsWith("org.eclipse.") ||
                pkg.startsWith("io.quarkus.") ||
                pkg.startsWith("org.jooq.") ||
                pkg.startsWith("com.example.shared") ||
                pkg.startsWith("com.example.api") ||
                pkg.startsWith("com.example.jooq")
        }
    }

    @ArchTest
    val `aggregate roots should only reference other aggregates by ID` =
        classes()
            .that().areAnnotatedWith(AggregateRoot::class.java)
            .should(
                object : ArchCondition<JavaClass>("only access same aggregate or framework classes") {
                    override fun check(
                        item: JavaClass,
                        events: ConditionEvents,
                    ) {
                        item.directDependenciesFromSelf.forEach { dependency ->
                            val target = dependency.targetClass

                            // Allow framework classes
                            if (isFrameworkClass(target)) return@forEach

                            // Allow accessing other aggregate roots (for ID references)
                            if (target.isAnnotatedWith(AggregateRoot::class.java)) return@forEach

                            // Allow accessing classes in the same aggregate
                            if (inSameAggregate(item, target)) return@forEach

                            // Violation: accessing another aggregate's internal class
                            if (isInAggregate(target)) {
                                val message =
                                    "Aggregate root ${item.simpleName} in ${getAggregatePackage(item)} " +
                                        "accesses ${target.simpleName} from ${getAggregatePackage(target)} aggregate"
                                events.add(SimpleConditionEvent.violated(item, message))
                            }
                        }
                    }
                },
            ).because("Aggregate roots should only reference other aggregates by ID, not import their entities")

    @ArchTest
    val `only aggregate roots should be publicly accessible` =
        classes()
            .that().areAnnotatedWith(AggregateRoot::class.java)
            .should().bePublic()
            .because("Aggregate roots are the entry points to aggregates and should be publicly accessible")

    @ArchTest
    val `shared package should not depend on aggregates` =
        noClasses()
            .that().resideInAPackage("..shared..")
            .should(
                object : ArchCondition<JavaClass>("not depend on any aggregate") {
                    override fun check(
                        item: JavaClass,
                        events: ConditionEvents,
                    ) {
                        item.directDependenciesFromSelf.forEach { dependency ->
                            val target = dependency.targetClass
                            if (isInAggregate(target)) {
                                val message =
                                    "Shared class ${item.simpleName} depends on " +
                                        "${target.simpleName} from ${getAggregatePackage(target)} aggregate"
                                events.add(SimpleConditionEvent.violated(item, message))
                            }
                        }
                    }
                },
            ).because("Shared package provides utilities and should not depend on any aggregate")
}
