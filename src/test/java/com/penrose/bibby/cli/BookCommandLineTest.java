package com.penrose.bibby.cli;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;


class CliArchitectureTest {

    private static final JavaClasses importedClasses =
            new ClassFileImporter().importPackages("com.penrose.bibby");

    @Test
    void cli_should_not_depend_on_infrastructure() {
        noClasses()
                .that().resideInAPackage("..cli..")
                .and().haveSimpleNameNotEndingWith("Test") // ✅ correct ArchUnit predicate
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "..infrastructure..",
                        "..external.."
                )
                .check(importedClasses);
    }
}
