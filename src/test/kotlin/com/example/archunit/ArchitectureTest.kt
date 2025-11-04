package com.example.archunit

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses

@AnalyzeClasses(
    packages = ["com.example"],
    importOptions = [
        ImportOption.DoNotIncludeTests::class,
        ImportOption.DoNotIncludeJars::class,
    ],
)
abstract class ArchitectureTest
