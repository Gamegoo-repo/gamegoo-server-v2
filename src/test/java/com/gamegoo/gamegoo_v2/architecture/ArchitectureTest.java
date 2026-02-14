package com.gamegoo.gamegoo_v2.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.INTERFACES;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * ArchUnit을 사용한 아키텍처 규칙 검증 테스트
 *
 * 이 테스트는 프로젝트의 아키텍처 규칙을 자동으로 검증합니다.
 */
class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setup() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
                .withImportOption(location -> !location.contains("/test/"))
                .importPackages("com.gamegoo.gamegoo_v2");
    }

    @Test
    @DisplayName("레이어 아키텍처 규칙: Controller -> Service -> Repository 의존성 방향 검증")
    void layerDependencyRuleTest() {
        ArchRule rule = layeredArchitecture()
                .consideringAllDependencies()
                .layer("Controller").definedBy("..controller..")
                .layer("Service").definedBy("..service..")
                .layer("Repository").definedBy("..repository..")

                .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
                .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller", "Service")
                .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")

                // Security, Validator, 인프라 컴포넌트는 예외 처리
                .ignoreDependency(resideInAPackage("..security.."), resideInAPackage("..repository.."))
                .ignoreDependency(resideInAPackage("..annotation.resolver.."), resideInAPackage("..repository.."))
                .ignoreDependency(resideInAPackage("..dto.."), resideInAPackage("..service.."))
                .ignoreDependency(resideInAPackage("..validator.."), resideInAPackage("..repository.."))
                .ignoreDependency(resideInAPackage("..validator.."), resideInAPackage("..service.."))
                .ignoreDependency(resideInAPackage("..batch.."), resideInAPackage("..service.."))
                .ignoreDependency(resideInAPackage("..event.listener.."), resideInAPackage("..service.."))
                .ignoreDependency(resideInAPackage("..scheduler.."), resideInAPackage("..repository.."))
                .ignoreDependency(resideInAPackage("..scheduler.."), resideInAPackage("..service.."))
                .ignoreDependency(resideInAPackage("..scripts.."), resideInAPackage("..repository.."))
                .ignoreDependency(resideInAPackage("..test_support.."), resideInAPackage("..repository.."))
                .ignoreDependency(resideInAPackage("..test_support.."), resideInAPackage("..service.."));

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Service 클래스는 Service 또는 FacadeService로 끝나야 함")
    void serviceNamingConventionTest() {
        ArchRule rule = classes()
                .that().resideInAPackage("..service..")
                .and().areNotInterfaces()
                .and().areNotEnums()
                .and().areTopLevelClasses()
                .and().doNotHaveSimpleName("ServiceTestUtil")
                .and().haveSimpleNameNotContaining("Test")
                .should().haveSimpleNameEndingWith("Service")
                .orShould().haveSimpleNameEndingWith("FacadeService")
                .orShould().haveSimpleNameEndingWith("Validator")
                .orShould().haveSimpleNameEndingWith("Calculator")
                .orShould().haveSimpleNameEndingWith("Processor")
                .orShould().haveSimpleNameEndingWith("Builder");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Controller는 Controller로 끝나야 함")
    void controllerNamingConventionTest() {
        ArchRule rule = classes()
                .that().resideInAPackage("..controller..")
                .and().areNotInterfaces()
                .should().haveSimpleNameEndingWith("Controller");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Repository는 Repository로 끝나야 함")
    void repositoryNamingConventionTest() {
        ArchRule rule = classes()
                .that().resideInAPackage("..repository..")
                .and().areNotInterfaces()
                .and().doNotHaveSimpleName("JpaConfig")
                .and().areTopLevelClasses()
                .should().haveSimpleNameEndingWith("Repository")
                .orShould().haveSimpleNameEndingWith("RepositoryImpl")
                .orShould().haveSimpleNameEndingWith("CustomImpl");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Controller는 Service에 의존해야 하고, Repository에 직접 의존하면 안됨")
    void controllerShouldNotDependOnRepositoryTest() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..controller..")
                .should().dependOnClassesThat().resideInAPackage("..repository..");

        rule.check(importedClasses);
    }

    @Test
    @DisplayName("Service는 @Service 또는 @Component 어노테이션을 가져야 함")
    void serviceShouldBeAnnotatedTest() {
        ArchRule rule = classes()
                .that().resideInAPackage("..service..")
                .and().haveSimpleNameEndingWith("Service")
                .and().areNotInterfaces()
                .should().beAnnotatedWith(org.springframework.stereotype.Service.class)
                .orShould().beAnnotatedWith(org.springframework.stereotype.Component.class);

        rule.check(importedClasses);
    }
/**
    @Test
    @DisplayName("FacadeService가 아닌 일반 Service는 다른 Service를 호출하면 안됨")
    void nonFacadeServiceShouldNotCallOtherServicesTest() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..service..")
                .and().haveSimpleNameEndingWith("Service")
                .and().haveSimpleNameNotEndingWith("FacadeService")
                .and().haveSimpleNameNotEndingWith("Validator")
                .and().haveSimpleNameNotEndingWith("Calculator")
                .and().haveSimpleNameNotEndingWith("Processor")
                .and().haveSimpleNameNotEndingWith("Builder")
                .and().areNotInterfaces()
                .should().dependOnClassesThat(
                        resideInAPackage("..service..")
                                .and(simpleNameEndingWith("Service"))
                                .and(not(INTERFACES))
                )
                .because("일반 Service는 Repository만 호출해야 합니다. " +
                        "여러 Service를 조율해야 한다면 FacadeService로 만들어야 합니다.");

        rule.check(importedClasses);
    }
**/
    @Test
    @DisplayName("FacadeService는 다른 FacadeService를 호출하면 안됨")
    void facadeServiceShouldNotCallOtherFacadeServicesTest() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..service..")
                .and().haveSimpleNameEndingWith("FacadeService")
                .and().areNotInterfaces()
                .should().dependOnClassesThat(
                        resideInAPackage("..service..")
                                .and(simpleNameEndingWith("FacadeService"))
                                .and(not(INTERFACES))
                )
                .because("FacadeService끼리 호출하면 순환 의존성과 책임 불명확 문제가 발생합니다. " +
                        "FacadeService는 DomainService만 조율해야 합니다.");

        rule.check(importedClasses);
    }
}
