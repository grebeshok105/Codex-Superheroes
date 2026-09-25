# План 1 — архитектурные guardrails и чистка: Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Поставить механический гейт на архитектурную связанность, сделать полноту героя проверяемой и до начала миграции убрать мёртвый код, следы Doctor Strange и фиктивный `api/`.

**Architecture:** ArchUnit-правила в `src/test` проверяют скомпилированные классы main и client. Текущие нарушения заморожены в `archunit_store`: новые ломают `qualityGate`, а исправленные нужно закоммитить (ratchet только вниз). Циклы пакетов ведёт собственный ratchet-тест. Полнота героя проверяется GameTest'ом. Чистка удаляет только код без владельца, persisted id не трогает.

**Tech Stack:** Java 21, Minecraft 1.21.1 (Mojang mappings), Fabric Loader 0.19.2, Fabric API 0.116.12+1.21.1, Fabric Loom 1.16-SNAPSHOT, JUnit 5.10.2, Fabric GameTest API, ArchUnit 1.5.1 (новая test-зависимость).

## Global Constraints

- Mod id: `superheroes` — не меняется никогда.
- Персистентные идентификаторы не меняются: id способностей (`superheroes:<ability>`), предметов, сущностей, звуков (`sounds.json`), MobEffect, типов урона, attachments (`hero_data`, `reinhard_state`, `doomsday_progress`, `regulus_madness`, `regulus_bonus_life`, `admin_build`, `suit_variant`, `nano_form`, `control_lock_shadow`, `pandora_revived` и др.), data component `superheroes:bound_weapon`, **ResourceLocation-ы attribute-модификаторов** (`modifiers/<hero>/<stat>` — permanent-пассивки лежат в NBT игроков), имена `KeyMapping` (`key.superheroes.*` — лежат в `options.txt`), lang-ключи.
- Id сетевых payload'ов не персистентны: их можно менять только при семантически оправданном слиянии (стадия L2).
- Геймплей и баланс не меняются в архитектурных PR. Любое изменение поведения — отдельный коммит с пометкой `behavior:` в сообщении и отдельным пунктом в описании PR.
- Server-authoritative: всё клиентское — в `src/client`; `src/main` загружается выделенным сервером.
- Только публичные API: `net.fabricmc.fabric.api.*`, никакого `net.fabricmc.fabric.impl.*`.
- Сеть — типизированные `CustomPacketPayload` + `StreamCodec`.
- `HeroDataStore` — единственный писатель `HERO_DATA` (`ProjectSanityTest.assertHeroDataHasSingleWriter`).
- Lifecycle-очистка — только через `PlayerLifecycle` (серверный поток), никогда через `ServerPlayConnectionEvents.DISCONNECT`.
- Mixins: узкие, `@Unique` для внедрённых членов, `@WrapOperation` предпочтительнее `@Redirect`.
- Звуки только OGG Vorbis; перед новыми ассетами проверять `art-source/`.
- `en_us.json` и `ru_ru.json` меняются вместе.
- `src/main/generated/` — только через `./gradlew runDatagen --no-daemon`; diff обязан быть пустым, если стадия не меняет данные намеренно.
- Финишный гейт каждого PR: `./gradlew qualityGate --no-daemon` зелёный.
- Runtime-изменения (ввод, рендер, HUD, сеть, VFX, сущности) — `./gradlew runClient --no-daemon`; если окружение не может запустить клиент, PR прямо это говорит.
- Никаких Gradle-модулей на героя, никакого big-bang rewrite, никакой перестановки файлов без смены владения.
- PR: заголовок на русском, тело начинается с «Для игрока», ниже — полная техническая часть; коммиты `feat(scope):`/`fix(scope):`/`refactor(scope):`/`test(scope):`/`docs(scope):` на английском.
- `SESSION.md` обновляется в каждом PR этого плана; статус стадии отмечается в таблице «Статус стадий» этого файла.

---

## Как исполнять этот план

- Карта всей миграции, исходное состояние, целевая архитектура и сквозные политики — в [`00-overview.md`](00-overview.md). Другие планы для этой работы читать не нужно.
- Одна стадия = один PR. A1, A2 расписаны по шагам TDD с кодом. N1–N3 заданы паспортами, и их первый шаг — **«Сверка»**: перечитать перечисленные файлы на актуальном `main` и обновить список касаний в описании PR.
- Если на актуальном коде проблема уже решена иначе — используем существующее решение и фиксируем это в PR и в разделе «Решения» этого плана. Откатывать рабочее решение ради буквального соответствия плану нельзя.
- Пути: `M/` = `src/main/java/com/example/superheroes/`, `C/` = `src/client/java/com/example/superheroes/client/`, `T/` = `src/test/java/com/example/superheroes/`, `G/` = `src/gametest/java/com/example/superheroes/gametest/`.
- Шаблон паспорта: **Цель · Почему · Зависит от · Затрагивает · Создаётся · Мигрируется · Удаляется · Старые пути, которых больше нет · Нельзя менять · Тесты · Runtime · Acceptance · Риски · Страховка.** Global Constraints в паспортах не повторяются.

Каждая стадия заканчивается одинаково (шаги не повторяются в каждой задаче):

- [ ] `./gradlew qualityGate --no-daemon` → `BUILD SUCCESSFUL`.
- [ ] Если стадия трогала datagen-источники: `./gradlew runDatagen --no-daemon` и `git diff --stat src/main/generated` → пусто (или только намеренные изменения, перечисленные в PR).
- [ ] Обновить «Статус стадий» этого плана, `SESSION.md` и при необходимости раздел «Решения».
- [ ] PR по правилам AGENTS.md §12; в технической части — baseline-метрики (`00-overview.md` §3.3) «до/после» для затронутых строк.

### Статус стадий

| Стадия | Статус | PR |
| :-- | :-- | :-- |
| A1 ArchUnit guardrails | ✅ | #54 |
| A2 полнота героя | ✅ | |
| N1 мёртвый код | ✅ | |
| N2 следы Doctor Strange | ⏳ | |
| N3 фиктивный `api/` | ✅ | |

## Контекст

- Seams bugfix-pass, на которые опирается план: GameTest lane (`src/gametest`, `runGametest` в `qualityGate`), `G/TestPlayers` (включая `clearSpawnInvulnerability`), `ProjectSanityTest` (честный grep по исходникам и JSON: `assertHeroDataHasSingleWriter`, `assertNoHeroTypeDispatch`, `assertClientStatesRegisterReset`, `assertWorldMutationsGoThroughPolicy`, `assertNoCyrillicLiterals`, `assertEntityLangNames` и др.; структурные правила сюда не добавляются).
- Внешние зависимости: База (обзор §2.1) — все PR bugfix-pass (#37–#40, #42–#49) и PR #41 с планами влиты в `main`. Перед влитием стека см. обзор §3.3 (конфликты и `RemoteHeroSkins`).
- Composition roots и правила слоёв — решение R16 обзора; план 1 их вводит.
- Этот план не трогает production-логику героев. Данные героя — П2, bootstrap и тики — П3, клиент — П4.

### Решения

| # | Вопрос / расхождение | Что говорит код сейчас | Решение |
| :-- | :-- | :-- | :-- |
| R8 | Модульный: `ProjectSanityTest` + source-check импортов; SESSION: «ArchUnit намеренно не портирован» | Код массово использует FQN-ссылки (`com.example.superheroes.effect.X.init()`), grep по `import` их не видит; сам `ProjectSanityTest` велит структурные правила выносить в отдельный инструмент | Архитектурные правила — ArchUnit (`src/test`) с `FreezingArchRule`: текущие нарушения заморожены в `src/test/resources/archunit_store/`, новые ломают гейт, исправленные автоматически уходят из store; `qualityGate` требует, чтобы store был закоммичен (ratchet только вниз). `ProjectSanityTest` остаётся честным grep по исходникам/JSON. |
| R13 | `api/` — «фикция» (структурный S12) | `docs/api.md` удалён ревайвлом; единственный потребитель — `RepulsorChargeController` | `api/` удаляется в `N3`. Публичный аддон-API проектируется позже поверх `HeroModule` (Fabric entrypoint), когда появится реальный потребитель. |

## Зависимости стадий

```mermaid
flowchart LR
  BASE[База: BF-стек и #41 в main] --> A1[A1 ArchUnit + TestHeroes]
  A1 --> A2[A2 полнота героя]
  A1 --> N1[N1 мёртвый код]
  A1 --> N2[N2 Doctor Strange]
  A1 --> N3[N3 api]
```

После A1 стадии A2, N1, N2, N3 независимы и идут параллельно. Выход плана: A2 нужна П2 B1, N1 — П3 D1, N2 — П2 B3 (обзор §2.1).

## File Structure

| Файл | Ответственность | Стадия |
| :-- | :-- | :-- |
| `T/architecture/ArchitectureRulesTest.java` | ArchUnit-правила main (замороженные и строгие) | A1 |
| `T/architecture/ClientArchitectureRulesTest.java` | ArchUnit-правила client | A1 |
| `T/architecture/CodexClasses.java` | импорт классов main/client из путей, переданных Gradle | A1 |
| `src/test/resources/archunit.properties` | конфиг freeze store | A1 |
| `src/test/resources/archunit_store/**` | замороженный baseline нарушений | A1 |
| `T/architecture/PackageCycleRatchetTest.java`, `src/test/resources/architecture/package-cycles-baseline.txt` | ratchet двунаправленных пар пакетов | A1 |
| `G/TestHeroes.java` | трансформация в GameTests через `HeroTransformService` | A1 |
| `G/HeroCompletenessGameTests.java` | полнота героя: способности зарегистрированы, lang-ключи есть (П3 D2a-2 добавляет в него проверку списка модулей) | A2 |

N1–N3 новых production-файлов не создают. Удаляемое перечислено в их паспортах; единственный переименовываемый класс — `DoctorStrangeSuitItem` → `PandoraSuitItem` (N2).

## Стадии

### Стадия A1 — ArchUnit guardrails с замороженным baseline

- **Цель:** механически запретить новые архитектурные нарушения и сделать каждое устранённое нарушение необратимым.
- **Почему:** без гейта каждый параллельный агент может добавить ещё одну ветку `XHero.ID.equals` в shared-код (именно так Scorpion и Pandora выпали из таблиц — аудит 2, S2). grep по `import` не видит FQN-ссылок, которыми полон bootstrap (R8).
- **Зависит от:** База (§2.1 обзора): все PR bugfix-pass и PR #41 влиты в `main` — иначе baseline придётся пересобирать.
- **Затрагивает:** `build.gradle`, `src/test/**`, `src/gametest/**` (только `TestHeroes`), `AGENTS.md` §10 (одна фраза).
- **Создаётся:** `T/architecture/CodexClasses.java`, `T/architecture/ArchitectureRulesTest.java`, `T/architecture/ClientArchitectureRulesTest.java`, `T/architecture/PackageCycleRatchetTest.java`, `src/test/resources/archunit.properties`, `src/test/resources/archunit_store/` (сгенерировано), `src/test/resources/architecture/package-cycles-baseline.txt` (сгенерировано), Gradle-задача `verifyArchitectureBaseline`, `G/TestHeroes.java`.
- **Мигрируется / удаляется:** ничего в production-коде.
- **Нельзя менять:** production-код; существующие проверки `ProjectSanityTest`.
- **Тесты:** сами правила; негативные пробы в A1.5.
- **Runtime:** не нужен.
- **Acceptance:** `qualityGate` зелёный; store и baseline циклов закоммичены; все негативные пробы A1.5 падают на своём правиле; удаление одной замороженной зависимости меняет store, и `verifyArchitectureBaseline` падает, пока изменение не закоммичено.
- **Проверено прототипом** (обзор §10): весь код этой стадии компилируется и выполняется на сводной базе BF1–BF12 с ArchUnit 1.5.1 и Gradle 9.4.1; baseline — 32 пары циклов; негативные пробы ловятся.
- **Риски:** baseline снимается с `main` в момент стадии — числа из обзора §3.4 могут отличаться; это нормально, в PR записываются фактические.
- **Страховка:** стадия не трогает production; откат — revert PR.

#### Task A1.1: подключить ArchUnit и импорт классов main/client

**Files:**
- Modify: `build.gradle` (блок `dependencies`, конфигурация задачи `test`)
- Create: `T/architecture/CodexClasses.java`, `src/test/resources/archunit.properties`

**Interfaces:**
- Produces: `CodexClasses.ROOT` (`String`), `CodexClasses.main()` → `JavaClasses` (только `src/main`), `CodexClasses.mainAndClient()` → `JavaClasses`.

- [ ] **Step 1: Зависимость и проброс путей в `build.gradle`**

В `dependencies` рядом с JUnit:

```groovy
	testImplementation "com.tngtech.archunit:archunit-junit5:1.5.1"
```

После блока `tasks.withType(Test).configureEach { … }`:

```groovy
tasks.named('test') {
	dependsOn tasks.named('clientClasses')
	systemProperty 'codex.mainClasses', sourceSets.main.output.classesDirs.asPath
	systemProperty 'codex.clientClasses', sourceSets.client.output.classesDirs.asPath
	// One-off baseline creation: -Darchunit.freeze.store.default.allowStoreCreation=true -Dcodex.writeCycleBaseline=true
	systemProperties System.properties.findAll { it.key.toString().startsWith('archunit.') || it.key.toString().startsWith('codex.write') }
	// fileTree tolerates a missing directory; inputs.dir(...).optional() does not (Gradle 9.4.1 fails before JUnit starts).
	inputs.files(fileTree('src/test/resources/archunit_store')).withPropertyName('archunitStore')
}
```

- [ ] **Step 2: `CodexClasses`**

```java
package com.example.superheroes.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class CodexClasses {
	static final String ROOT = "com.example.superheroes";
	private static JavaClasses main;
	private static JavaClasses mainAndClient;

	private CodexClasses() {
	}

	static synchronized JavaClasses main() {
		if (main == null) main = importDirs("codex.mainClasses");
		return main;
	}

	static synchronized JavaClasses mainAndClient() {
		if (mainAndClient == null) mainAndClient = importDirs("codex.mainClasses", "codex.clientClasses");
		return mainAndClient;
	}

	private static JavaClasses importDirs(String... properties) {
		List<Path> dirs = new ArrayList<>();
		for (String property : properties) {
			String value = System.getProperty(property);
			if (value == null || value.isBlank()) throw new IllegalStateException(property + " is not set");
			for (String entry : value.split(File.pathSeparator)) {
				Path dir = Path.of(entry);
				if (Files.isDirectory(dir)) dirs.add(dir);
			}
		}
		if (dirs.isEmpty()) throw new IllegalStateException("no class directories");
		return new ClassFileImporter().importPaths(dirs);
	}
}
```

- [ ] **Step 3: `src/test/resources/archunit.properties`**

```properties
freeze.store.default.path=src/test/resources/archunit_store
freeze.store.default.allowStoreCreation=false
freeze.store.default.allowStoreUpdate=true
freeze.refreeze=false
archRule.failOnEmptyShould=true
resolveMissingDependenciesFromClassPath=false
```

- [ ] **Step 4: Commit**

```bash
git add build.gradle src/test/java/com/example/superheroes/architecture/CodexClasses.java src/test/resources/archunit.properties
git commit -m "test(arch): import main and client classes for ArchUnit rules"
```

#### Task A1.2: правила main — замороженные и строгие

**Files:**
- Create: `T/architecture/ArchitectureRulesTest.java`
- Create: `src/test/resources/archunit_store/**` (генерируется)

**Interfaces:**
- Consumes: `CodexClasses`.
- Produces: `ArchitectureRulesTest.ROOT`, `HERO_MODULES` (`"<root>.bootstrap.HeroModules"`), `HERO_CLIENT_MODULES` (`"<root>.client.bootstrap.HeroClientModules"`), предикаты `IN_HERO_MODULE`, `IN_CLIENT_HERO_MODULE`, `CONCRETE_HERO`, `COMPOSITION_ROOT`, `heroId(String, String)`, `onlyReferencedByOwnModuleOr(String modulePrefix, String... allowed)` — их используют `ClientArchitectureRulesTest`, `PackageCycleRatchetTest` и правила П5 F.4.

Composition roots (R16 обзора): `SuperheroesMod`, `client.SuperheroesClient`, пакеты `bootstrap..` и `client.bootstrap..`. Им можно зависеть от всего; от них — никому. Контракты модулей лежат в `core.module`, списки — в `bootstrap`, поэтому строгое правило ядра не противоречит списку модулей.

- [ ] **Step 1: Правила**

```java
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
```

Замороженные правила (`FreezingArchRule`) фиксируют текущий долг; строгие пустые сейчас (`allowEmptyShould(true)`) и начинают проверять реальный код, как только появляются `core..`, `mechanic..`, `bootstrap..`, `hero.<id>..`. `everyHeroModuleIsConstructedInTheModuleList` проверяет вызов конструктора модуля именно из статического инициализатора `HeroModules` (поле `ALL = List.of(new …())`): упоминание `ScorpionModule.class` или создание модуля в другом месте правило не удовлетворяет.

- [ ] **Step 2: Без store замороженные правила падают**

Run: `./gradlew test --no-daemon --tests '*ArchitectureRulesTest*'`
Expected: FAIL — `Creating new violation store is disabled` в 5 замороженных правилах; строгие правила и `importsTheWholeMainSourceSet` — PASS.

#### Task A1.3: правила client

**Files:**
- Create: `T/architecture/ClientArchitectureRulesTest.java`

- [ ] **Step 1: Правила**

```java
package com.example.superheroes.architecture;

import com.tngtech.archunit.library.freeze.FreezingArchRule;
import org.junit.jupiter.api.Test;

import static com.example.superheroes.architecture.ArchitectureRulesTest.*;
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
```

#### Task A1.4: ratchet циклов пакетов

**Files:**
- Create: `T/architecture/PackageCycleRatchetTest.java`
- Create: `src/test/resources/architecture/package-cycles-baseline.txt` (генерируется)

- [ ] **Step 1: Тест**

```java
package com.example.superheroes.architecture;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Bidirectional package pairs may only disappear. Edges that originate in composition roots
 * (entrypoints, module lists) are not counted: those classes depend on everything by design, and
 * nothingDependsOnCompositionRoots keeps them a sink.
 */
class PackageCycleRatchetTest {
	private static final Path BASELINE = Path.of("src/test/resources/architecture/package-cycles-baseline.txt");

	@Test
	void bidirectionalPackagePairsOnlyShrink() throws IOException {
		Set<String> edges = new HashSet<>();
		for (JavaClass origin : CodexClasses.mainAndClient()) {
			if (ArchitectureRulesTest.COMPOSITION_ROOT.test(origin)) {
				continue;
			}
			String from = relative(origin.getPackageName());
			if (from == null) {
				continue;
			}
			for (Dependency d : origin.getDirectDependenciesFromSelf()) {
				String to = relative(d.getTargetClass().getPackageName());
				if (to != null && !to.equals(from)) {
					edges.add(from + " -> " + to);
				}
			}
		}
		Set<String> pairs = new TreeSet<>();
		for (String edge : edges) {
			String[] p = edge.split(" -> ");
			if (edges.contains(p[1] + " -> " + p[0])) {
				pairs.add(p[0].compareTo(p[1]) < 0 ? p[0] + " <-> " + p[1] : p[1] + " <-> " + p[0]);
			}
		}
		Set<String> baseline = new TreeSet<>(Files.exists(BASELINE) ? Files.readAllLines(BASELINE) : java.util.List.of());
		baseline.removeIf(String::isBlank);
		if (Boolean.getBoolean("codex.writeCycleBaseline")) {
			Files.write(BASELINE, pairs);
			return;
		}
		assertEquals(String.join("\n", baseline), String.join("\n", pairs),
				"package 2-cycles changed: new pairs are forbidden; removed pairs must be deleted from " + BASELINE);
	}

	private static String relative(String pkg) {
		String root = CodexClasses.ROOT;
		if (pkg.equals(root)) return "(root)";
		return pkg.startsWith(root + ".") ? pkg.substring(root.length() + 1) : null;
	}
}
```

Рёбра, выходящие из composition roots, не считаются: они зависят от всего по определению, а `nothingDependsOnCompositionRoots` гарантирует, что обратных рёбер к ним нет. Без этого исключения `SuperheroesMod → core.X` и `core.X → ModId` давали бы цикл с корневым пакетом на каждой новой стадии.

- [ ] **Step 2: Создать baseline обоих механизмов**

Run: `mkdir -p src/test/resources/architecture && ./gradlew test --no-daemon --tests '*architecture*' -Darchunit.freeze.store.default.allowStoreCreation=true -Dcodex.writeCycleBaseline=true`
Expected: PASS; появились `src/test/resources/archunit_store/stored.rules`, файлы нарушений и `package-cycles-baseline.txt` (на сводной базе — 32 пары).

- [ ] **Step 3: Повторный прогон без флагов**

Run: `./gradlew test --no-daemon --tests '*architecture*'`
Expected: PASS.

- [ ] **Step 4: Записать в PR** число строк в каждом файле store (соответствие файлу — по `stored.rules`) и число пар циклов.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/example/superheroes/architecture src/test/resources/archunit_store src/test/resources/architecture
git commit -m "test(arch): freeze current coupling, enforce layering for new packages, ratchet package cycles"
```

#### Task A1.5: `verifyArchitectureBaseline` в `qualityGate` и негативные пробы

**Files:**
- Modify: `build.gradle` (задачи `qualityGate`, новая `verifyArchitectureBaseline`)
- Modify: `AGENTS.md` §10 (одна фраза)

- [ ] **Step 1: Задача**

```groovy
// ArchUnit removes fixed violations from the store while tests run; an uncommitted shrink would let the
// same violation return later, so the gate requires the store to match what is committed.
tasks.register('verifyArchitectureBaseline') {
	group = 'verification'
	description = 'Fails when the frozen ArchUnit baseline differs from the committed one'
	dependsOn tasks.named('test')
	doLast {
		def status = providers.exec {
			commandLine 'git', 'status', '--porcelain', '--', 'src/test/resources/archunit_store', 'src/test/resources/architecture'
		}.standardOutput.asText.get().trim()
		if (!status.isEmpty()) {
			throw new GradleException("Architecture baseline changed — review and commit it:\n" + status)
		}
	}
}
```

В `qualityGate` добавить `dependsOn tasks.named('verifyArchitectureBaseline')`.

- [ ] **Step 2: Негативные пробы (временные правки, не коммитить).** Внести все сразу:
  - `M/core/module/Probe.java`: `static final Object MODULE = new com.example.superheroes.hero.scorpion.ScorpionProbeModule();` и `static final Object LOG = com.example.superheroes.SuperheroesMod.LOGGER;`, где `M/hero/scorpion/ScorpionProbeModule.java` — пустой класс, реализующий `HeroModule` (интерфейс-заглушка `M/core/module/HeroModule.java` с методами `hero()` и `register(Object)` создаётся на время пробы, если D2a-1 ещё не влит);
  - в `ScorpionProbeModule` — поле, читающее `com.example.superheroes.SuperheroesMod.LOGGER`;
  - в `M/ability/AbilityRouter.java` — поле `static final Object CYCLE = new com.example.superheroes.core.module.Probe();`.

Run: `./gradlew test --no-daemon --tests '*architecture*'`
Expected: FAIL ровно в `coreDependsOnNothingAboveIt`, `nothingDependsOnCompositionRoots`, `heroModulesAreReferencedOnlyByThemselvesAndTheModuleList`, `everyHeroModuleIsConstructedInTheModuleList` (модуль не создан в `HeroModules.ALL`) и `bidirectionalPackagePairsOnlyShrink` (новая пара `ability <-> core.module`). Откатить пробы: `git checkout -- . && git clean -fd src/main`.

- [ ] **Step 3: Полный гейт** — `./gradlew qualityGate --no-daemon` → PASS.

- [ ] **Step 4: AGENTS.md §10** — к описанию `qualityGate` дописать: «…and the ArchUnit architecture rules (`src/test/.../architecture`) with a frozen, shrink-only baseline in `src/test/resources/archunit_store` and `src/test/resources/architecture` (`verifyArchitectureBaseline` fails until a shrunk baseline is committed)».

- [ ] **Step 5: Commit**

```bash
git add build.gradle AGENTS.md
git commit -m "build(arch): require a committed architecture baseline in qualityGate"
```

#### Task A1.6: помощник трансформации для GameTests

**Files:**
- Create: `G/TestHeroes.java`
- Modify: `G/BoundWeaponGameTests.java`, `G/HeroDataGameTests.java`, `G/LifecycleGameTests.java` и другие GameTests, где успешная трансформация проверяется `helper.assertTrue(HeroTransformService.transform(...), ...)`

**Interfaces:**
- Produces: `TestHeroes.transform(ServerPlayer, ResourceLocation)` — используют GameTests всех следующих планов (C2 и D1 стартуют сразу после A1).

- [ ] **Step 1: Код**

```java
package com.example.superheroes.gametest;

import com.example.superheroes.transform.HeroTransformService;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Transforms through the same service the transformation item uses. */
final class TestHeroes {
	private TestHeroes() {
	}

	static void transform(ServerPlayer player, ResourceLocation heroId) {
		if (!HeroTransformService.transform(player, heroId)) {
			throw new GameTestAssertException("could not transform " + player.getScoreboardName() + " into " + heroId);
		}
	}
}
```

- [ ] **Step 2:** Заменить `helper.assertTrue(HeroTransformService.transform(p, id), "...")` на `TestHeroes.transform(p, id)` там, где тест ожидает успех; места, где тест проверяет **отказ** трансформации (`assertFalse`), не трогать.

- [ ] **Step 3:** `./gradlew runGametest --no-daemon` → PASS. Commit:

```bash
git add src/gametest
git commit -m "test(gametest): share a transform helper across GameTests"
```

---

### Стадия A2 — полнота героя и проводки как проверяемые инварианты

- **Цель:** новый герой не может «тихо выпасть» из способностей и lang; проверка проводки контроллеров перестаёт цементировать `SuperheroesMod`.
- **Почему:** `assertControllersAreWired` требует `Name.init()` строкой в `SuperheroesMod` (аудит 2, S1) — любая миграция к модулям начинается с падения гейта; `assertEveryHeroRegistered` читает текст `Heroes.java`.
- **Зависит от:** A1.
- **Затрагивает:** `T/ProjectSanityTest.java`, `G/HeroCompletenessGameTests.java` (новый), `src/gametest/resources/fabric.mod.json`, lang-файлы (только если тест найдёт реальные пропуски).
- **Создаётся:** `HeroCompletenessGameTests`.
- **Мигрируется:** `assertControllersAreWired` принимает вызов `init()` из `SuperheroesMod` **или** из любого `M/**/*Module.java`.
- **Удаляется:** ничего (сама проверка удаляется в D2b, `assertEveryHeroRegistered` — в D2a-2).
- **Нельзя менять:** тексты существующих lang-ключей.
- **Тесты:** GameTest полноты; негативная проверка — временно убрать `register(SCORPION_SPEAR)` → тест падает.
- **Runtime:** не нужен.
- **Acceptance:** все 22 героя проходят; если найдены пропуски lang — добавлены в оба языка и перечислены в PR как content-fix.
- **Риски:** найденные пропуски потребуют текста — писать по фактическому поведению способности, помечать в PR.
- **Страховка:** revert.

#### Task A2.1: GameTest полноты героя

**Files:**
- Create: `src/gametest/java/com/example/superheroes/gametest/HeroCompletenessGameTests.java`
- Modify: `src/gametest/resources/fabric.mod.json` (entrypoint `fabric-gametest`)

**Interfaces:**
- Consumes: `Heroes.all()`, `AbilityRegistry.get(ResourceLocation)`, `Hero.getAbilities()`.
- Produces: `HeroCompletenessGameTests.lang(String)` → `JsonObject` (переиспользуется в B1).

- [ ] **Step 1: Сверка ключей** — `grep -rn '"ability\.\|"hero\.' src/client/java/com/example/superheroes/client/hud | head -40`: какие ключи HUD реально читает для способности и героя. Ожидаемо (проверено для Scorpion): `hero.<ns>.<id>`, `ability.<ns>.<ability>`, `ability.<ns>.<ability>.desc`. Если HUD читает другие — тест проверяет именно их.

- [ ] **Step 2: Написать тест**

```java
package com.example.superheroes.gametest;

import com.example.superheroes.ability.AbilityRegistry;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.Heroes;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** A hero that compiles must also be complete: registered abilities and lang in both languages. */
public final class HeroCompletenessGameTests implements FabricGameTest {
	static JsonObject lang(String code) {
		String path = "/assets/superheroes/lang/" + code + ".json";
		try (InputStream in = HeroCompletenessGameTests.class.getResourceAsStream(path)) {
			if (in == null) {
				throw new IllegalStateException("missing " + path);
			}
			return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (java.io.IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void everyListedAbilityIsRegistered(GameTestHelper helper) {
		List<String> problems = new ArrayList<>();
		for (Hero hero : Heroes.all().values()) {
			for (ResourceLocation id : hero.getAbilities()) {
				if (AbilityRegistry.get(id) == null) {
					problems.add(hero.getId() + " lists unregistered " + id);
				}
			}
		}
		helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void everyHeroAndAbilityHasLangInBothLanguages(GameTestHelper helper) {
		List<String> problems = new ArrayList<>();
		for (String code : List.of("en_us", "ru_ru")) {
			JsonObject json = lang(code);
			for (Hero hero : Heroes.all().values()) {
				ResourceLocation heroId = hero.getId();
				require(json, code, "hero." + heroId.getNamespace() + "." + heroId.getPath(), problems);
				for (ResourceLocation id : hero.getAbilities()) {
					String base = "ability." + id.getNamespace() + "." + id.getPath();
					require(json, code, base, problems);
					require(json, code, base + ".desc", problems);
				}
			}
		}
		helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
		helper.succeed();
	}

	private static void require(JsonObject json, String code, String key, List<String> problems) {
		if (!json.has(key)) {
			problems.add(code + " missing " + key);
		}
	}
}
```

Добавить `"com.example.superheroes.gametest.HeroCompletenessGameTests"` в `entrypoints.fabric-gametest`.

- [ ] **Step 3: Прогнать** — `./gradlew runGametest --no-daemon`. Expected: PASS или список реальных пропусков. Пропуски исправить в обоих lang-файлах отдельным коммитом `fix(lang): …`, повторить → PASS.

- [ ] **Step 4: Негативная проверка** — временно закомментировать `register(SCORPION_SPEAR);` в `AbilityRegistry.init()` → `runGametest` FAIL с `scorpion lists unregistered superheroes:scorpion_spear`; вернуть.

- [ ] **Step 5: Commit**

```bash
git add src/gametest
git commit -m "test(gametest): assert every hero is complete in registry and lang"
```

#### Task A2.2: проводка контроллеров допускает модули

**Files:**
- Modify: `src/test/java/com/example/superheroes/ProjectSanityTest.java` (метод `assertControllersAreWired`)

- [ ] **Step 1: Заменить источник проводки**

```java
	// Convention: a *Controller that declares `public static void init()` must have that init() invoked
	// from SuperheroesMod.onInitialize() or from a hero/shared module (*Module.java). Removed in stage D2b,
	// when ticks and lifecycle move behind HeroModuleContext and no controller keeps a static init().
	private static void assertControllersAreWired() throws IOException {
		StringBuilder wiring = new StringBuilder(Files.readString(SUPERHEROES_MOD));
		try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
			for (Path module : files.filter(p -> p.getFileName().toString().endsWith("Module.java")).toList()) {
				wiring.append('\n').append(Files.readString(module));
			}
		}
		String wiringSource = wiring.toString();
		int controllers = 0;
		try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
			for (Path file : files.filter(path -> path.getFileName().toString().endsWith("Controller.java")).toList()) {
				if (!STATIC_INIT.matcher(Files.readString(file)).find()) {
					continue;
				}
				controllers++;
				String name = file.getFileName().toString().replace(".java", "");
				assert wiringSource.contains(name + ".init()")
						: file.getFileName() + " declares public static void init() but " + name
								+ ".init() is called neither from SuperheroesMod nor from a *Module";
			}
		}
		assert controllers > 0 : "no *Controller with static init() found; this check would pass vacuously";
	}
```

- [ ] **Step 2: Прогнать** — `./gradlew testProjectSanity --no-daemon` → PASS.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/example/superheroes/ProjectSanityTest.java
git commit -m "test(sanity): accept controller wiring from modules"
```

---

### Стадия N1 — мёртвый и недостижимый код

- **Цель:** удалить код без владельца, чтобы миграция не переносила мусор.
- **Почему:** аудит 2 (S13): `ViltrumiteThunderClapAbility`, `C/hud/LowResourceVignetteHud`, `C/hud/ResourceBarHud`, `C/render/horde/HordeGeoRenderer` без ссылок; 5 `AbilityIds` без регистрации (`VILTRUMITE_THUNDER_CLAP`, `IRON_MAN_NANO_REPAIR`, `NARUTO_KURAMA_CLOAK`, `NARUTO_TAILED_BEAST_BOMB`, `NARUTO_FLYING_RAIJIN`); `METEOR_SLAM`, `SHOCKWAVE_PULSE` зарегистрированы, но ни у одного героя, при этом `MeteorSlamAbility.serverTick` работает каждый тик на каждого игрока, а `InvincibleHero` чистит его состояние.
- **Зависит от:** A1 (store фиксирует исчезновение ссылок).
- **Затрагивает:** перечисленные файлы, `AbilityIds`, `AbilityRegistry`, `SuperheroesMod` (строка `MeteorSlamAbility` в таблице `registerTickHandlers` BF11 и её `onLeave`/`onDeath`/`resetAll`), `InvincibleHero`, lang (ключи удалённых способностей — удалить в обоих файлах), текстуры иконок удалённых способностей (если есть).
- **Удаляется:** всё перечисленное. Перед удалением каждого класса — `grep -rn '<ClassName>' src/` = только сам класс.
- **Нельзя менять:** ни одного достижимого id. `HeroData.CODEC` декодирует неизвестные id способностей (проверено: `ResourceLocation.CODEC`), поэтому старые сохранения с этими id безопасны.
- **Тесты:** GameTest `HeroDataGameTests.decodesUnknownAbilityIds` — `HeroData.CODEC.parse(NbtOps.INSTANCE, <nbt с active=[superheroes:meteor_slam]>)` → success; `AbilityRouter.activate(player, METEOR_SLAM id)` для героя без неё — no-op.
- **Runtime:** не нужен (ничего достижимого).
- **Acceptance:** `grep -rn 'METEOR_SLAM\|SHOCKWAVE_PULSE\|THUNDER_CLAP\|NANO_REPAIR\|KURAMA_CLOAK\|TAILED_BEAST_BOMB\|FLYING_RAIJIN' src/` пусто (кроме удаляемых lang-ключей, которых тоже нет); ArchUnit store уменьшился; `SuperheroesMod` потерял все строки `MeteorSlamAbility` (тик, leave, death, stop).
- **Риски:** `ShockwavePulseAbility`/`MeteorSlamAbility` могут переиспользоваться другими классами как утилиты — тогда утилиту переносят к потребителю, а способность удаляют.
- **Страховка:** revert.

### Стадия N2 — следы Doctor Strange и вводящие в заблуждение имена (только Java)

- **Цель:** имена классов соответствуют владельцу, persisted id не трогаются.
- **Почему:** модульный аудит §3 (Pandora): `DoctorStrangeSuitItem`, `STRANGE_HP`, неиспользуемая `textures/entity/hero/doctor_strange.png`, комментарий `// Doctor Strange` в `AbilityIds`.
- **Зависит от:** A1.
- **Мигрируется:** поле `ModItems.DOCTOR_STRANGE_SUIT` → `ModItems.PANDORA_SUIT` (регистрационный id **остаётся** `doctor_strange_suit`, модель `models/item/doctor_strange_suit.json` и lang-ключи `item.superheroes.doctor_strange_suit*` остаются); константа `HeroAttributes.STRANGE_HP` → `PANDORA_HP` (значение ResourceLocation **не меняется**); комментарии. Класс `DoctorStrangeSuitItem` не переименовывается: у него только lore, и П2 B3 удаляет его вместе с другими lore-only подклассами.
- **Удаляется:** `textures/entity/hero/doctor_strange.png` после `grep -rn 'doctor_strange.png\|entity/hero/doctor_strange' src/` = пусто.
- **Нельзя менять:** `doctor_strange_suit`, любые `modifiers/...` id, lang-ключи.
- **Тесты:** существующий `assertItemModelsResolveToTextures`; GameTest `pandoraSuitIdIsStable`: `BuiltInRegistries.ITEM.getKey(ModItems.PANDORA_SUIT)` == `superheroes:doctor_strange_suit`.
- **Acceptance:** `grep -rni 'strange' src/main/java src/client/java` → только строковые id `doctor_strange_suit` (с комментарием «persisted id kept from the Doctor Strange era») и класс `DoctorStrangeSuitItem`, который удалит B3.
- Переименования `Madness*` (Homelander) vs `RegulusMadness*` делаются в волнах I5/I6 вместе с переносом, а не здесь (иначе два переноса одного файла).

### Стадия N3 — удаление фиктивного `api/`

- **Цель:** убрать «публичный API», у которого нет ни документации, ни внешних потребителей (R13).
- **Зависит от:** A1.
- **Затрагивает:** `M/api/{HeroApi,AbilityApi,CreativeTabIds,package-info}.java`, `M/ability/RepulsorChargeController.java` (единственный потребитель; заменить вызовы `HeroApi`/`AbilityApi` прямыми `HeroDataStore.get`/`Heroes.get`/`AbilityRegistry.get`/`ResourceController.charge`), `README.md` (если упоминает API).
- **Сверка:** `grep -rn 'superheroes.api' src/ README.md docs/` и поиск по GitHub-коду (`gh search code "com.example.superheroes.api"`) — если найден внешний потребитель, стадия останавливается и вопрос уходит владельцу.
- **Удаляется:** весь пакет `api/`.
- **Тесты:** `CreativeTabIds` может держать id вкладки — если так, id переезжает в `ModItemGroups` без изменения значения; GameTest проверяет id вкладки.
- **Acceptance:** пакета `api` нет; пара `ability <-> api` исчезла из cycle baseline.

---

## Готово, когда

- `qualityGate` включает ArchUnit-правила main и client, `PackageCycleRatchetTest`, `verifyArchitectureBaseline` и `HeroCompletenessGameTests`.
- Freeze store и `package-cycles-baseline.txt` закоммичены; после N1–N3 они только уменьшились.
- `assertControllersAreWired` принимает проводку из `*Module.java`.
- Нет мёртвых способностей и id из N1, нет пакета `api/`; `grep -rni strange src/main/java src/client/java` находит только persisted id `doctor_strange_suit` и класс, который удалит B3.

## Self-Review

- **Покрытие:** A1, A2, N1–N3 и замечания внешнего ревью к ним (правила слоёв против списка модулей, циклы через корневой пакет, `inputs.dir(...).optional()`, ложный успех проверки списка модулей, неопределённая зависимость P0) закрыты; решения R8, R13 — выше, R16 — обзор.
- **Что используют следующие планы:** `CodexClasses.ROOT/main()/mainAndClient()`; `ArchitectureRulesTest.ROOT`, `HERO_MODULES`, `HERO_CLIENT_MODULES`, `IN_HERO_MODULE`, `IN_CLIENT_HERO_MODULE`, `CONCRETE_HERO`, `COMPOSITION_ROOT`, `heroId(String, String)`, `onlyReferencedByOwnModuleOr(String modulePrefix, String... allowed)`; замороженные правила `sharedCodeDoesNotDependOnConcreteHeroes`, `mainDoesNotDependOnClientCode`, `onlyTheDispatcherRegistersServerTicks`, `lifecycleHooksAreRegisteredThroughRegistrars`, `nothingDependsOnCompositionRoots`, `sharedClientCodeDoesNotDependOnConcreteHeroes`; строгие `coreDependsOnNothingAboveIt`, `mechanicsDoNotDependUpward`, `heroModulesDoNotDependOnEachOther`, `heroModulesDoNotDependOnContentCompatOrBootstrap`, `heroModulesAreReferencedOnlyByThemselvesAndTheModuleList`, `everyHeroModuleIsConstructedInTheModuleList`, клиентские аналоги; `HeroCompletenessGameTests.lang(String)`; `TestHeroes.transform(ServerPlayer, ResourceLocation)`.
- **Проверка:** код A1 скомпилирован и прогнан прототипом на сводной базе (обзор §10); код A2 и N-стадий не компилировался.

## Execution Handoff

Исполнение: **Subagent-Driven (рекомендуется)** — свежий субагент на стадию, ревью между стадиями (superpowers:subagent-driven-development), или **Inline** с контрольными точками (superpowers:executing-plans). Начинать с A1, когда в `main` влиты стек bugfix-pass и PR #41.

При параллельном исполнении несколькими субагентами оркестратор раздаёт задачи этого плана по `00-overview.md` §11 «Оркестрация».
