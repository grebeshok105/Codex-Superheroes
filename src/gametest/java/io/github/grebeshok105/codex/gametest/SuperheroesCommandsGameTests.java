package io.github.grebeshok105.codex.gametest;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.attachment.ModAttachments;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.debug.AdminAbilityDebug;
import io.github.grebeshok105.codex.item.ModItemGroups;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * IC3 phase-1 characterization: pins the observable behavior of the shared
 * {@code /superheroes} command root (transform/untransform, energy/mana,
 * debug mob-targets, admin build, horde subcommands, permission gating,
 * feedback message keys) so the {@code command/} + {@code debug/} →
 * {@code content/command/} + {@code content/admin/} move is verified
 * byte-equivalent. Commands run through the real dispatcher with a
 * player-backed source at the given permission level.
 */
public final class SuperheroesCommandsGameTests implements FabricGameTest {
	private static final String SCORPION = "superheroes:scorpion";
	private static final String HOMELANDER = "superheroes:homelander";

	// ───────────────────────────── hero / untransform ─────────────────────

	@GameTest(template = EMPTY_STRUCTURE)
	public void heroCommandTransformsAndReports(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "cmdhero");
		List<Component> messages = runAs(player, 2, "superheroes hero " + SCORPION);

		helper.assertTrue(HeroDataStore.get(player).hasHero(), "hero command must transform the player");
		helper.assertTrue(HeroDataStore.get(player).heroId().equals(ModId.of("scorpion")),
				"hero must be scorpion, got " + HeroDataStore.get(player).heroId());
		helper.assertTrue(hasKey(messages, "commands.superheroes.hero.success"),
				"must report hero.success, got " + messages);
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void heroCommandRejectsUnknownId(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "cmdunkhero");
		List<Component> messages = runAs(player, 2, "superheroes hero superheroes:no_such_hero");

		helper.assertTrue(!HeroDataStore.get(player).hasHero(), "unknown hero id must not transform");
		helper.assertTrue(hasKey(messages, "commands.superheroes.hero.unknown"),
				"must report hero.unknown, got " + messages);
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 600)
	public void untransformCommandClearsHero(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "cmduntr");
		TestHeroes.transform(player, ModId.of("scorpion"));
		// The transform service holds a 20-tick cooldown; a command inside the
		// window is silently rejected — wait it out like a real admin would.
		helper.runAfterDelay(25, () -> {
			List<Component> messages = runAs(player, 2, "superheroes untransform");

			helper.assertTrue(!HeroDataStore.get(player).hasHero(), "untransform must clear the hero");
			helper.assertTrue(hasKey(messages, "commands.superheroes.untransform.success"),
					"must report untransform.success, got " + messages);
			TestPlayers.leave(player);
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void untransformWithoutHeroFails(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "cmduntr0");
		List<Component> messages = runAs(player, 2, "superheroes untransform");

		helper.assertTrue(hasKey(messages, "commands.superheroes.untransform.no_hero"),
				"must report untransform.no_hero, got " + messages);
		TestPlayers.leave(player);
		helper.succeed();
	}

	// ───────────────────────────── debug mob-targets ──────────────────────

	@GameTest(template = EMPTY_STRUCTURE)
	public void debugMobTargetsCommandsDriveFlag(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "cmdmobs");
		// Shared static flag — reset on entry so the sequence is order-independent.
		AdminAbilityDebug.setPlayerOnlyAbilitiesTargetMobs(false);

		List<Component> on = runAs(player, 2, "superheroes debug mob-targets on");
		helper.assertTrue(AdminAbilityDebug.playerOnlyAbilitiesTargetMobs(),
				"mob-targets on must set the flag");
		helper.assertTrue(hasKey(on, "commands.superheroes.debug.mob_targets.enabled"),
				"must report mob_targets.enabled, got " + on);

		List<Component> status = runAs(player, 2, "superheroes debug mob-targets status");
		helper.assertTrue(AdminAbilityDebug.playerOnlyAbilitiesTargetMobs(),
				"status must leave the flag on");
		helper.assertTrue(hasKey(status, "commands.superheroes.debug.mob_targets.enabled"),
				"status must report mob_targets.enabled, got " + status);

		List<Component> toggle = runAs(player, 2, "superheroes debug mob-targets");
		helper.assertTrue(!AdminAbilityDebug.playerOnlyAbilitiesTargetMobs(),
				"bare mob-targets must toggle the flag off");
		helper.assertTrue(hasKey(toggle, "commands.superheroes.debug.mob_targets.disabled"),
				"must report mob_targets.disabled, got " + toggle);

		List<Component> off = runAs(player, 2, "superheroes debug mob-targets off");
		helper.assertTrue(!AdminAbilityDebug.playerOnlyAbilitiesTargetMobs(),
				"mob-targets off must keep the flag off");
		helper.assertTrue(hasKey(off, "commands.superheroes.debug.mob_targets.disabled"),
				"must report mob_targets.disabled, got " + off);

		TestPlayers.leave(player);
		helper.succeed();
	}

	// ───────────────────────────── energy / mana ──────────────────────────

	@GameTest(template = EMPTY_STRUCTURE)
	public void energyAndManaCommandsWriteHeroData(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "cmdres");
		TestHeroes.transform(player, ModId.of("homelander")); // manaMax=100, energyMax>0
		float energyMax = Heroes.get(ModId.of("homelander")).getEnergyMax();
		float manaMax = Heroes.get(ModId.of("homelander")).getManaMax();

		List<Component> e1 = runAs(player, 2, "superheroes energy 50");
		helper.assertTrue(Math.abs(HeroDataStore.get(player).energy() - Math.min(50f, energyMax)) < 0.01f,
				"energy 50 must store the value, got " + HeroDataStore.get(player).energy());
		helper.assertTrue(hasKey(e1, "commands.superheroes.energy.set"),
				"must report energy.set, got " + e1);

		runAs(player, 2, "superheroes energy 999999");
		helper.assertTrue(Math.abs(HeroDataStore.get(player).energy() - energyMax) < 0.01f,
				"energy must clamp to the hero max " + energyMax + ", got "
						+ HeroDataStore.get(player).energy());

		List<Component> m1 = runAs(player, 2, "superheroes mana 40");
		helper.assertTrue(Math.abs(HeroDataStore.get(player).mana() - Math.min(40f, manaMax)) < 0.01f,
				"mana 40 must store the value, got " + HeroDataStore.get(player).mana());
		helper.assertTrue(hasKey(m1, "commands.superheroes.mana.set"),
				"must report mana.set, got " + m1);

		runAs(player, 2, "superheroes mana 999999");
		helper.assertTrue(Math.abs(HeroDataStore.get(player).mana() - manaMax) < 0.01f,
				"mana must clamp to the hero max " + manaMax + ", got "
						+ HeroDataStore.get(player).mana());
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void energyAndManaRequireHero(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "cmdres0");
		List<Component> e = runAs(player, 2, "superheroes energy 50");
		List<Component> m = runAs(player, 2, "superheroes mana 50");

		helper.assertTrue(hasKey(e, "commands.superheroes.no_hero"),
				"energy without a hero must report no_hero, got " + e);
		helper.assertTrue(hasKey(m, "commands.superheroes.no_hero"),
				"mana without a hero must report no_hero, got " + m);
		TestPlayers.leave(player);
		helper.succeed();
	}

	// ───────────────────────────── admin build ────────────────────────────

	@GameTest(template = EMPTY_STRUCTURE)
	public void adminCommandsToggleBuildAndGive(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "cmdadmin");
		helper.assertTrue(!player.getAttachedOrCreate(ModAttachments.ADMIN_BUILD),
				"fresh player must not have admin build");

		List<Component> toggle = runAs(player, 2, "superheroes admin");
		helper.assertTrue(player.getAttachedOrCreate(ModAttachments.ADMIN_BUILD),
				"bare admin must toggle the flag on");
		helper.assertTrue(hasKey(toggle, "commands.superheroes.admin_build.on"),
				"must report admin_build.on, got " + toggle);

		List<Component> gave = runAs(player, 2, "superheroes admin give");
		helper.assertTrue(hasKey(gave, "commands.superheroes.admin_build.given"),
				"must report admin_build.given, got " + gave);
		for (Item item : ModItemGroups.ADMIN_ONLY_ITEMS) {
			helper.assertTrue(TestPlayers.count(player, item) > 0,
					"admin give must deliver " + item);
		}

		List<Component> off = runAs(player, 2, "superheroes admin off");
		helper.assertTrue(!player.getAttachedOrCreate(ModAttachments.ADMIN_BUILD),
				"admin off must clear the flag");
		helper.assertTrue(hasKey(off, "commands.superheroes.admin_build.off"),
				"must report admin_build.off, got " + off);
		TestPlayers.leave(player);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void adminGiveRequiresAdminBuild(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "cmdadm0");
		List<Component> messages = runAs(player, 2, "superheroes admin give");

		helper.assertTrue(hasKey(messages, "commands.superheroes.admin_build.required"),
				"must report admin_build.required, got " + messages);
		for (Item item : ModItemGroups.ADMIN_ONLY_ITEMS) {
			helper.assertTrue(TestPlayers.count(player, item) == 0,
					"give without admin build must deliver nothing");
		}
		TestPlayers.leave(player);
		helper.succeed();
	}

	// ─────────────────────────── permission gating ────────────────────────

	@GameTest(template = EMPTY_STRUCTURE)
	public void superheroesRootRequiresPermission2(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ServerPlayer player = TestPlayers.join(helper, "cmdperm");
		CommandSourceStack low = player.createCommandSourceStack().withPermission(0);
		CommandSourceStack op = player.createCommandSourceStack().withPermission(2);

		var root = server.getCommands().getDispatcher().getRoot().getChild("superheroes");
		helper.assertTrue(root != null, "the /superheroes root literal must be registered");
		helper.assertTrue(!root.canUse(low), "permission 0 must not pass the root requirement");
		helper.assertTrue(root.canUse(op), "permission 2 must pass the root requirement");

		AdminAbilityDebug.setPlayerOnlyAbilitiesTargetMobs(false);
		runAs(player, 0, "superheroes hero " + SCORPION);
		runAs(player, 0, "superheroes debug mob-targets on");
		runAs(player, 0, "superheroes admin");
		helper.assertTrue(!HeroDataStore.get(player).hasHero(),
				"a permission-0 player must not transform via the command");
		helper.assertTrue(!AdminAbilityDebug.playerOnlyAbilitiesTargetMobs(),
				"a permission-0 player must not flip the debug flag");
		helper.assertTrue(!player.getAttachedOrCreate(ModAttachments.ADMIN_BUILD),
				"a permission-0 player must not toggle admin build");
		TestPlayers.leave(player);
		helper.succeed();
	}

	// ───────────────────────────── info / abilities ───────────────────────

	@GameTest(template = EMPTY_STRUCTURE)
	public void abilitiesAndInfoCommandsReport(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "cmdinfo");
		TestHeroes.transform(player, ModId.of("scorpion"));

		List<Component> abilities = runAs(player, 2, "superheroes abilities");
		helper.assertTrue(hasKey(abilities, "commands.superheroes.abilities.header"),
				"must print the abilities header, got " + abilities);
		long rows = abilities.stream().filter(SuperheroesCommandsGameTests::isAbilitiesRow).count();
		helper.assertTrue(rows == Heroes.get(ModId.of("scorpion")).getAbilities().size(),
				"one row per ability, got " + rows);

		List<Component> info = runAs(player, 2, "superheroes info");
		helper.assertTrue(hasKey(info, "commands.superheroes.info"),
				"must report info, got " + info);
		TestPlayers.leave(player);
		helper.succeed();
	}

	// ────────────────────────────── horde branch ──────────────────────────

	@GameTest(template = EMPTY_STRUCTURE)
	public void hordeCommandsAnswerThroughSharedRoot(GameTestHelper helper) {
		ServerPlayer player = TestPlayers.join(helper, "cmdhord");

		// Horde tests in this batch may legitimately run a horde in the same level —
		// only pins that never mutate horde state are safe here (stop/clear/spawn are
		// level-global and would corrupt a concurrent run).
		List<Component> status = runAs(player, 2, "superheroes horde status");
		// Idle reply uses "орды" (genitive), an active horde answers "Орда | волна …" —
		// match the shared root case-insensitively so either state counts.
		helper.assertTrue(status.stream().anyMatch(c -> c.getString().toLowerCase().contains("орд")),
				"horde status must answer with the horde status text, got " + status);

		List<Component> overlay = runAs(player, 2, "superheroes horde overlay");
		helper.assertTrue(hasKey(overlay, "commands.superheroes.horde.debug_overlay"),
				"overlay toggle must report horde.debug_overlay, got " + overlay);
		// Leave the overlay off for other tests.
		runAs(player, 2, "superheroes horde overlay off");
		TestPlayers.leave(player);
		helper.succeed();
	}

	// ─────────────────────────────── internals ────────────────────────────

	/**
	 * Runs {@code command} through the real dispatcher as if {@code player} had typed it
	 * at the given permission level; returns every feedback message the command sent.
	 */
	private static List<Component> runAs(ServerPlayer player, int permission, String command) {
		List<Component> messages = new ArrayList<>();
		CommandSource capture = new CommandSource() {
			@Override
			public void sendSystemMessage(Component message) {
				messages.add(message);
			}

			@Override
			public boolean acceptsSuccess() {
				return true;
			}

			@Override
			public boolean acceptsFailure() {
				return true;
			}

			@Override
			public boolean shouldInformAdmins() {
				return true;
			}
		};
		ServerLevel level = player.serverLevel();
		CommandSourceStack stack = player.createCommandSourceStack()
				.withSource(capture)
				.withPermission(permission);
		level.getServer().getCommands().performPrefixedCommand(stack, command);
		return messages;
	}

	private static boolean hasKey(List<Component> messages, String key) {
		for (Component message : messages) {
			if (containsKey(message, key)) {
				return true;
			}
		}
		return false;
	}

	// Error feedback arrives as Component.empty().withStyle(RED).append(translatable) —
	// the key lives in the siblings, not the root contents.
	private static boolean containsKey(Component component, String key) {
		if (component.getContents() instanceof TranslatableContents t && key.equals(t.getKey())) {
			return true;
		}
		for (Component sibling : component.getSiblings()) {
			if (containsKey(sibling, key)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isAbilitiesRow(Component message) {
		return message.getContents() instanceof TranslatableContents t
				&& "commands.superheroes.abilities.row".equals(t.getKey());
	}
}
