package com.example.superheroes.architecture;

import com.example.superheroes.hero.Hero;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaStaticInitializer;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleName;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameStartingWith;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureRulesTest {
	static final String ROOT = CodexClasses.ROOT;
	static final String HERO_MODULES = ROOT + ".bootstrap.HeroModules";
	static final String HERO_CLIENT_MODULES = ROOT + ".client.bootstrap.HeroClientModules";

	static DescribedPredicate<JavaClass> inPackagePrefix(String prefix, String description) {
		return DescribedPredicate.describe(description, c -> c.getPackageName().startsWith(prefix));
	}

	static final DescribedPredicate<JavaClass> IN_HERO_MODULE = inPackagePrefix(ROOT + ".hero.", "reside in a hero module");
	static final DescribedPredicate<JavaClass> IN_CLIENT_HERO_MODULE = inPackagePrefix(ROOT + ".client.hero.", "reside in a client hero module");
	static final DescribedPredicate<JavaClass> CONCRETE_HERO = DescribedPredicate.describe("are concrete heroes",
			c -> c.isAssignableTo(Hero.class) && !c.isInterface() && !c.getModifiers().contains(JavaModifier.ABSTRACT));
	/** Entrypoints and module lists: allowed to depend on everything; nothing may depend on them. */
	static final DescribedPredicate<JavaClass> COMPOSITION_ROOT = DescribedPredicate.describe("are composition roots",
			c -> c.getName().equals(ROOT + ".SuperheroesMod") || c.getName().equals(ROOT + ".client.SuperheroesClient")
					|| c.getPackageName().startsWith(ROOT + ".bootstrap") || c.getPackageName().startsWith(ROOT + ".client.bootstrap"));

	static String heroId(String packageName, String prefix) {
		String rest = packageName.substring(prefix.length());
		int dot = rest.indexOf('.');
		return dot < 0 ? rest : rest.substring(0, dot);
	}

	static ArchCondition<JavaClass> onlyReferencedByOwnModuleOr(String modulePrefix, String... allowed) {
		Set<String> allowedNames = Set.of(allowed);
		return new ArchCondition<>("be referenced only by the same hero module or " + allowedNames) {
			@Override
			public void check(JavaClass target, ConditionEvents events) {
				String id = heroId(target.getPackageName(), modulePrefix);
				for (Dependency d : target.getDirectDependenciesToSelf()) {
					String pkg = d.getOriginClass().getPackageName();
					boolean own = pkg.equals(ROOT + ".hero." + id) || pkg.startsWith(ROOT + ".hero." + id + ".")
							|| pkg.equals(ROOT + ".client.hero." + id) || pkg.startsWith(ROOT + ".client.hero." + id + ".");
					if (!own && !allowedNames.contains(d.getOriginClass().getName())) {
						events.add(SimpleConditionEvent.violated(d, d.getDescription()));
					}
				}
			}
		};
	}

	@Test
	void importsTheWholeMainSourceSet() {
		assertTrue(CodexClasses.main().size() > 400, "main classes: " + CodexClasses.main().size());
	}

	// ---- frozen: current debt may only shrink

	@Test
	void sharedCodeDoesNotDependOnConcreteHeroes() {
		FreezingArchRule.freeze(noClasses().that(not(IN_HERO_MODULE)).and(not(COMPOSITION_ROOT))
				.should().dependOnClassesThat(CONCRETE_HERO)
				.as("shared code asks the hero registry and hooks, never a concrete hero")).check(CodexClasses.main());
	}

	@Test
	void mainDoesNotDependOnClientCode() {
		FreezingArchRule.freeze(noClasses().should().dependOnClassesThat().resideInAnyPackage(
				"net.minecraft.client..", "com.mojang.blaze3d..", "net.fabricmc.fabric.api.client..", ROOT + ".client..")
				.as("src/main loads on a dedicated server")).check(CodexClasses.main());
	}

	@Test
	void onlyTheDispatcherRegistersServerTicks() {
		String events = "net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents";
		FreezingArchRule.freeze(noClasses().that().doNotHaveSimpleName("HeroTickDispatcher").and().doNotHaveSimpleName("HeroDataStore")
				.should().accessField(events, "END_SERVER_TICK").orShould().accessField(events, "START_SERVER_TICK")
				.orShould().accessField(events, "END_WORLD_TICK").orShould().accessField(events, "START_WORLD_TICK")
				.as("server ticks go through HeroTickDispatcher")).check(CodexClasses.main());
	}

	@Test
	void lifecycleHooksAreRegisteredThroughRegistrars() {
		FreezingArchRule.freeze(noClasses().that().resideOutsideOfPackages(ROOT + ".lifecycle..", ROOT + ".core..")
				.should().callMethodWhere(target(owner(simpleName("PlayerLifecycle"))).and(target(nameStartingWith("on"))))
				.orShould().callMethodWhere(target(owner(simpleName("HeroLifecycle"))).and(target(nameStartingWith("on"))))
				.as("lifecycle hooks are registered through a module's LifecycleRegistrar")).check(CodexClasses.main());
	}

	@Test
	void nothingDependsOnCompositionRoots() {
		FreezingArchRule.freeze(noClasses().that(not(COMPOSITION_ROOT)).should().dependOnClassesThat(COMPOSITION_ROOT)
				.as("entrypoints and module lists sit on top; use LoggerFactory.getLogger(ModId.MOD_ID) instead of SuperheroesMod.LOGGER"))
				.check(CodexClasses.mainAndClient());
	}

	@Test
	void abilitiesDoNotCheckTheirOwnCooldown() {
		FreezingArchRule.freeze(noClasses().that().implement(com.example.superheroes.ability.Ability.class)
				.should().callMethod(com.example.superheroes.ability.AbilityCooldowns.class, "isOnCooldown",
						net.minecraft.server.level.ServerPlayer.class, net.minecraft.resources.ResourceLocation.class)
				.as("AbilityRouter owns the cooldown check; an ability may only check another ability's cooldown"))
				.check(CodexClasses.main());
	}

	// ---- strict: empty today, enforced from the first class

	@Test
	void coreDependsOnNothingAboveIt() {
		noClasses().that().resideInAPackage(ROOT + ".core..")
				.should().dependOnClassesThat().resideInAnyPackage(ROOT + ".mechanic..", ROOT + ".content..", ROOT + ".compat..",
						ROOT + ".bootstrap..", ROOT + ".client..")
				.orShould().dependOnClassesThat(IN_HERO_MODULE).orShould().dependOnClassesThat(CONCRETE_HERO)
				.allowEmptyShould(true).check(CodexClasses.main());
	}

	@Test
	void mechanicsDoNotDependUpward() {
		noClasses().that().resideInAPackage(ROOT + ".mechanic..")
				.should().dependOnClassesThat().resideInAnyPackage(ROOT + ".content..", ROOT + ".compat..", ROOT + ".bootstrap..")
				.orShould().dependOnClassesThat(IN_HERO_MODULE).orShould().dependOnClassesThat(CONCRETE_HERO)
				.allowEmptyShould(true).check(CodexClasses.main());
	}

	@Test
	void heroModulesDoNotDependOnEachOther() {
		slices().matching(ROOT + ".hero.(*)..").should().notDependOnEachOther().allowEmptyShould(true).check(CodexClasses.main());
	}

	@Test
	void heroModulesDoNotDependOnContentCompatOrBootstrap() {
		noClasses().that(IN_HERO_MODULE)
				.should().dependOnClassesThat().resideInAnyPackage(ROOT + ".content..", ROOT + ".compat..", ROOT + ".bootstrap..")
				.allowEmptyShould(true).check(CodexClasses.main());
	}

	@Test
	void heroModulesAreReferencedOnlyByThemselvesAndTheModuleList() {
		classes().that(IN_HERO_MODULE).should(onlyReferencedByOwnModuleOr(ROOT + ".hero.", HERO_MODULES))
				.allowEmptyShould(true).check(CodexClasses.mainAndClient());
	}

	@Test
	void everyHeroModuleIsConstructedInTheModuleList() {
		classes().that().implement(ROOT + ".core.module.HeroModule")
				.should(new ArchCondition<>("be constructed by HeroModules' static initializer") {
					@Override
					public void check(JavaClass module, ConditionEvents events) {
						boolean listed = false;
						for (JavaConstructorCall call : module.getConstructorCallsToSelf()) {
							if (call.getOrigin() instanceof JavaStaticInitializer
									&& call.getOriginOwner().getName().equals(HERO_MODULES)) {
								listed = true;
							}
						}
						if (!listed) {
							events.add(SimpleConditionEvent.violated(module, module.getName() + " is not constructed in HeroModules.ALL"));
						}
					}
				}).allowEmptyShould(true).check(CodexClasses.main());
	}
}
