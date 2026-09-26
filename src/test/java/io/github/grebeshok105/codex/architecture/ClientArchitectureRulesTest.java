package io.github.grebeshok105.codex.architecture;

import com.tngtech.archunit.library.freeze.FreezingArchRule;
import org.junit.jupiter.api.Test;

import static io.github.grebeshok105.codex.architecture.ArchitectureRulesTest.*;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

class ClientArchitectureRulesTest {
	@Test
	void sharedClientCodeDoesNotDependOnConcreteHeroes() {
		FreezingArchRule.freeze(noClasses().that().resideInAPackage(ROOT + ".client..").and(not(IN_CLIENT_HERO_MODULE)).and(not(COMPOSITION_ROOT))
				.should().dependOnClassesThat(CONCRETE_HERO)
				.as("shared client code reads the hero registry and hooks, never a concrete hero")).check(CodexClasses.mainAndClient());
	}

	@Test
	void clientCoreDoesNotKnowHeroModules() {
		noClasses().that().resideInAPackage(ROOT + ".client.core..")
				.should().dependOnClassesThat(IN_CLIENT_HERO_MODULE).orShould().dependOnClassesThat(IN_HERO_MODULE)
				.orShould().dependOnClassesThat(CONCRETE_HERO).orShould().dependOnClassesThat().resideInAPackage(ROOT + ".client.bootstrap..")
				.allowEmptyShould(true).check(CodexClasses.mainAndClient());
	}

	@Test
	void clientHeroModulesDoNotDependOnEachOther() {
		slices().matching(ROOT + ".client.hero.(*)..").should().notDependOnEachOther().allowEmptyShould(true).check(CodexClasses.mainAndClient());
	}

	@Test
	void clientHeroModulesAreReferencedOnlyByThemselvesAndTheModuleList() {
		classes().that(IN_CLIENT_HERO_MODULE).should(onlyReferencedByOwnModuleOr(ROOT + ".client.hero.", HERO_CLIENT_MODULES))
				.allowEmptyShould(true).check(CodexClasses.mainAndClient());
	}
}
