package io.mywallet.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Mechanically enforces the hexagonal-architecture rule agreed during the
 * {@code /improve-architecture} review: {@code *.domain.*} packages (Order, Portfolio,
 * Strategy, ...) must never depend on Spring, JPA, or Jackson. This is what
 * {@code EventStoreRepository} and the {@code AggregateRoot}/{@code DomainEvent} split
 * exist to make possible - this test is the tripwire that keeps it true as the codebase
 * grows past the point where a reviewer can catch a stray {@code @Entity} by eye.
 */
class HexagonalBoundaryTest {

    private static final com.tngtech.archunit.core.domain.JavaClasses CLASSES = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("io.mywallet");

    @Test
    void domainPackagesDoNotDependOnSpring() {
        ArchRule rule = noClasses()
            .that(resideInAPackage("..domain.."))
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.springframework..",
                "jakarta.persistence..",
                "org.hibernate.."
            );
        rule.check(CLASSES);
    }

    @Test
    void domainPackagesDoNotDependOnJacksonAnnotationsDirectly() {
        // Records serialize fine without Jackson annotations (see JacksonDomainEventSerializer);
        // if a domain event ever "needs" a @JsonProperty, that's a signal the serialization
        // concern is leaking into the domain and belongs in the infrastructure layer instead.
        ArchRule rule = noClasses()
            .that(resideInAPackage("..domain.."))
            .should().dependOnClassesThat().resideInAPackage("com.fasterxml.jackson..");
        rule.check(CLASSES);
    }
}
