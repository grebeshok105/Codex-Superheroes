package io.github.grebeshok105.codex.content.command;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.core.net.VfxEventS2CPayload;
import io.github.grebeshok105.codex.core.net.VfxFx;
import io.github.grebeshok105.codex.core.transform.HeroDataStore;
import io.github.grebeshok105.codex.core.vfx.VfxShowcases;
import io.github.grebeshok105.codex.content.admin.AdminAbilityDebug;
import io.github.grebeshok105.codex.content.admin.AdminAttachments;
import io.github.grebeshok105.codex.content.admin.AdminBuildSyncController;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.Heroes;
import io.github.grebeshok105.codex.item.ModItemGroups;
import io.github.grebeshok105.codex.core.model.HeroData;
import io.github.grebeshok105.codex.core.transform.HeroTransformService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

final class SuperheroesCommands {
	private static final SuggestionProvider<CommandSourceStack> HERO_SUGGESTIONS =
			(ctx, builder) -> SharedSuggestionProvider.suggestResource(Heroes.all().keySet(), builder);

	private static final SuggestionProvider<CommandSourceStack> HORDE_TYPE_SUGGESTIONS =
			(ctx, builder) -> SharedSuggestionProvider.suggest(
					io.github.grebeshok105.codex.content.horde.entity.HordeEntities.SPAWNABLE.keySet(), builder);

	private static final SuggestionProvider<CommandSourceStack> VFX_SCENE_SUGGESTIONS =
			(ctx, builder) -> SharedSuggestionProvider.suggestResource(VfxShowcases.ids(), builder);

	private static final ResourceLocation DEBUG_HUD_EFFECT = ModId.of("debug/hud");
	/** The stress scene — the heaviest registered scene; a literal since content must not import hero. */
	private static final ResourceLocation STRESS_SCENE = ModId.of("homelander/combat");
	private static final int STRESS_CAP = 64;
	private static final double VFX_PLAY_RANGE = 160.0;
	private static final double VFX_BROADCAST_RADIUS = 160.0;

	private SuperheroesCommands() {
	}

	/**
	 * Adds the shared {@code superheroes} root literal to the dispatcher. Hero-owned
	 * subcommands register their own {@code superheroes} literal (e.g.
	 * {@code BattleBeastCommands}); Brigadier merges same-named root children, so the
	 * trees coexist whichever order the registration callbacks fire in.
	 */
	static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("superheroes")
						.requires(src -> src.hasPermission(2))
						.then(Commands.literal("hero")
								.then(Commands.argument("id", ResourceLocationArgument.id())
										.suggests(HERO_SUGGESTIONS)
										.executes(SuperheroesCommands::setHero)))
						.then(Commands.literal("untransform")
								.executes(SuperheroesCommands::untransform))
						.then(Commands.literal("energy")
								.then(Commands.argument("amount", FloatArgumentType.floatArg(0f))
										.executes(ctx -> setEnergy(ctx, FloatArgumentType.getFloat(ctx, "amount")))))
						.then(Commands.literal("mana")
								.then(Commands.argument("amount", FloatArgumentType.floatArg(0f))
										.executes(ctx -> setMana(ctx, FloatArgumentType.getFloat(ctx, "amount")))))
						.then(Commands.literal("admin")
								.executes(SuperheroesCommands::toggleAdminBuild)
								.then(Commands.literal("on")
										.executes(ctx -> setAdminBuild(ctx, true)))
								.then(Commands.literal("off")
										.executes(ctx -> setAdminBuild(ctx, false)))
								.then(Commands.literal("give")
										.executes(SuperheroesCommands::giveAllAdminItems)))
						.then(Commands.literal("debug")
								.then(Commands.literal("mob-targets")
										.executes(SuperheroesCommands::toggleMobTargets)
										.then(Commands.literal("on")
												.executes(ctx -> setMobTargets(ctx, true)))
										.then(Commands.literal("off")
												.executes(ctx -> setMobTargets(ctx, false)))
										.then(Commands.literal("status")
												.executes(SuperheroesCommands::mobTargetsStatus))))
						.then(Commands.literal("horde")
								.then(Commands.literal("start")
										.executes(SuperheroesCommands::hordeStart))
								.then(Commands.literal("stop")
										.executes(SuperheroesCommands::hordeStop))
								.then(Commands.literal("clear")
										.executes(SuperheroesCommands::hordeClear))
								.then(Commands.literal("next")
										.executes(SuperheroesCommands::hordeNext))
								.then(Commands.literal("status")
										.executes(SuperheroesCommands::hordeStatus))
								.then(Commands.literal("overlay")
										.executes(SuperheroesCommands::hordeOverlayToggle)
										.then(Commands.literal("on")
												.executes(ctx -> hordeOverlaySet(ctx, true)))
										.then(Commands.literal("off")
												.executes(ctx -> hordeOverlaySet(ctx, false))))
								.then(Commands.literal("spawn")
										.then(Commands.argument("type", com.mojang.brigadier.arguments.StringArgumentType.word())
												.suggests(HORDE_TYPE_SUGGESTIONS)
												.executes(ctx -> hordeSpawn(ctx, 1))
												.then(Commands.argument("count", IntegerArgumentType.integer(1, 50))
														.executes(ctx -> hordeSpawn(ctx, IntegerArgumentType.getInteger(ctx, "count")))))))
						.then(Commands.literal("vfx")
								.then(Commands.literal("play")
										.then(Commands.argument("effect", ResourceLocationArgument.id())
												.executes(ctx -> vfxPlay(ctx, 1f))
												.then(Commands.argument("scale", FloatArgumentType.floatArg(0.01f))
														.executes(ctx -> vfxPlay(ctx,
																FloatArgumentType.getFloat(ctx, "scale"))))))
								.then(Commands.literal("scene")
										.then(Commands.argument("id", ResourceLocationArgument.id())
												.suggests(VFX_SCENE_SUGGESTIONS)
												.executes(ctx -> vfxScene(ctx, 1))
												.then(Commands.argument("count", IntegerArgumentType.integer(1, STRESS_CAP))
														.executes(ctx -> vfxScene(ctx,
																IntegerArgumentType.getInteger(ctx, "count"))))))
								.then(Commands.literal("stress")
										.then(Commands.argument("count", IntegerArgumentType.integer(0))
												.executes(ctx -> vfxStress(ctx,
														IntegerArgumentType.getInteger(ctx, "count")))))
								.then(Commands.literal("hud")
										.then(Commands.literal("on")
												.executes(ctx -> vfxHud(ctx, true)))
										.then(Commands.literal("off")
												.executes(ctx -> vfxHud(ctx, false)))))
						.then(Commands.literal("abilities")
								.executes(SuperheroesCommands::listAbilities))
						.then(Commands.literal("info")
								.executes(SuperheroesCommands::info)));
	}

	// ─────────────────────────────────────────── horde commands ───────────

	private static int hordeStart(CommandContext<CommandSourceStack> ctx) {
		ServerPlayer player = playerOrNull(ctx);
		if (player == null) {
			ctx.getSource().sendFailure(Component.translatable("commands.superheroes.not_a_player"));
			return 0;
		}
		net.minecraft.server.level.ServerLevel level = player.serverLevel();
		io.github.grebeshok105.codex.content.horde.HordeManager.startHorde(level, player.position(), player);
		ctx.getSource().sendSuccess(() -> Component.translatable("commands.superheroes.horde.started"), true);
		return 1;
	}

	private static int hordeStop(CommandContext<CommandSourceStack> ctx) {
		ServerPlayer player = playerOrNull(ctx);
		if (player == null) return 0;
		boolean ok = io.github.grebeshok105.codex.content.horde.HordeManager.stopHorde(player.serverLevel());
		ctx.getSource().sendSuccess(() -> Component.translatable(ok ? "commands.superheroes.horde.stopped" : "commands.superheroes.horde.not_active"), true);
		return ok ? 1 : 0;
	}

	private static int hordeClear(CommandContext<CommandSourceStack> ctx) {
		ServerPlayer player = playerOrNull(ctx);
		if (player == null) return 0;
		int n = io.github.grebeshok105.codex.content.horde.HordeManager.clearMobs(player.serverLevel());
		ctx.getSource().sendSuccess(() -> Component.translatable("commands.superheroes.horde.mobs_removed", n), true);
		return n;
	}

	private static int hordeNext(CommandContext<CommandSourceStack> ctx) {
		ServerPlayer player = playerOrNull(ctx);
		if (player == null) return 0;
		boolean ok = io.github.grebeshok105.codex.content.horde.HordeManager.forceNextWave(player.serverLevel());
		ctx.getSource().sendSuccess(() -> Component.translatable(ok ? "commands.superheroes.horde.wave_started" : "commands.superheroes.horde.not_active"), true);
		return ok ? 1 : 0;
	}

	private static int hordeStatus(CommandContext<CommandSourceStack> ctx) {
		ServerPlayer player = playerOrNull(ctx);
		if (player == null) return 0;
		String status = io.github.grebeshok105.codex.content.horde.HordeManager.getDebugStatus(player.serverLevel());
		ctx.getSource().sendSuccess(() -> Component.literal(status), false);
		return 1;
	}

	private static int hordeOverlayToggle(CommandContext<CommandSourceStack> ctx) {
		ServerPlayer player = playerOrNull(ctx);
		if (player == null) return 0;
		boolean on = io.github.grebeshok105.codex.content.horde.HordeManager.toggleOverlay(player);
		ctx.getSource().sendSuccess(() -> Component.translatable("commands.superheroes.horde.debug_overlay",
					Component.translatable(on ? "commands.superheroes.state.on" : "commands.superheroes.state.off")), false);
		return 1;
	}

	private static int hordeOverlaySet(CommandContext<CommandSourceStack> ctx, boolean enabled) {
		ServerPlayer player = playerOrNull(ctx);
		if (player == null) return 0;
		io.github.grebeshok105.codex.content.horde.HordeManager.setOverlay(player, enabled);
		ctx.getSource().sendSuccess(() -> Component.translatable("commands.superheroes.horde.debug_overlay",
					Component.translatable(enabled ? "commands.superheroes.state.on" : "commands.superheroes.state.off")), false);
		return 1;
	}

	private static int hordeSpawn(CommandContext<CommandSourceStack> ctx, int count) {
		ServerPlayer player = playerOrNull(ctx);
		if (player == null) return 0;
		String type = com.mojang.brigadier.arguments.StringArgumentType.getString(ctx, "type");
		net.minecraft.world.entity.EntityType<?> entityType =
				io.github.grebeshok105.codex.content.horde.entity.HordeEntities.SPAWNABLE.get(type);
		if (entityType == null) {
			ctx.getSource().sendFailure(Component.translatable("commands.superheroes.horde.unknown_type", type));
			return 0;
		}
		int n = io.github.grebeshok105.codex.content.horde.HordeManager.spawnSingle(
				player.serverLevel(), entityType, player.position(), count);
		ctx.getSource().sendSuccess(() -> Component.translatable("commands.superheroes.horde.spawned", n, type), true);
		return n;
	}

	private static int setHero(CommandContext<CommandSourceStack> ctx) {
		ServerPlayer player = playerOrNull(ctx);
		if (player == null) {
			return 0;
		}
		ResourceLocation heroId = ResourceLocationArgument.getId(ctx, "id");
		Hero hero = Heroes.get(heroId);
		if (hero == null) {
			ctx.getSource().sendFailure(Component.translatable("commands.superheroes.hero.unknown", heroId.toString()));
			return 0;
		}
		boolean ok = HeroTransformService.transform(player, heroId);
		if (!ok) {
			ctx.getSource().sendFailure(Component.translatable("commands.superheroes.hero.failed", heroId.toString()));
			return 0;
		}
		ctx.getSource().sendSuccess(() -> Component.translatable("commands.superheroes.hero.success",
				heroId.toString(), player.getScoreboardName()), true);
		return 1;
	}

	private static int untransform(CommandContext<CommandSourceStack> ctx) {
		ServerPlayer player = playerOrNull(ctx);
		if (player == null) {
			return 0;
		}
		boolean ok = HeroTransformService.untransform(player);
		if (!ok) {
			ctx.getSource().sendFailure(Component.translatable("commands.superheroes.untransform.no_hero"));
			return 0;
		}
		ctx.getSource().sendSuccess(() -> Component.translatable("commands.superheroes.untransform.success",
				player.getScoreboardName()), true);
		return 1;
	}

	private static int setEnergy(CommandContext<CommandSourceStack> ctx, float amount) {
		ServerPlayer player = playerOrNull(ctx);
		if (player == null) {
			return 0;
		}
		HeroData data = player.getAttachedOrCreate(CoreAttachments.HERO_DATA);
		if (!data.hasHero()) {
			ctx.getSource().sendFailure(Component.translatable("commands.superheroes.no_hero"));
			return 0;
		}
		Hero hero = Heroes.get(data.heroId());
		float clamped = hero == null ? amount : Math.min(amount, hero.getEnergyMax());
		HeroDataStore.update(player, d -> d.withEnergy(clamped));
		ctx.getSource().sendSuccess(() -> Component.translatable("commands.superheroes.energy.set",
				String.format("%.1f", clamped)), false);
		return (int) clamped;
	}

	private static int setMana(CommandContext<CommandSourceStack> ctx, float amount) {
		ServerPlayer player = playerOrNull(ctx);
		if (player == null) {
			return 0;
		}
		HeroData data = player.getAttachedOrCreate(CoreAttachments.HERO_DATA);
		if (!data.hasHero()) {
			ctx.getSource().sendFailure(Component.translatable("commands.superheroes.no_hero"));
			return 0;
		}
		Hero hero = Heroes.get(data.heroId());
		float clamped = hero == null ? amount : Math.min(amount, hero.getManaMax());
		HeroDataStore.update(player, d -> d.withMana(clamped));
		ctx.getSource().sendSuccess(() -> Component.translatable("commands.superheroes.mana.set",
				String.format("%.1f", clamped)), false);
		return (int) clamped;
	}


	private static int toggleMobTargets(CommandContext<CommandSourceStack> ctx) {
		boolean enabled = AdminAbilityDebug.togglePlayerOnlyAbilitiesTargetMobs();
		sendMobTargetsStatus(ctx, enabled);
		return enabled ? 1 : 0;
	}

	private static int setMobTargets(CommandContext<CommandSourceStack> ctx, boolean enabled) {
		AdminAbilityDebug.setPlayerOnlyAbilitiesTargetMobs(enabled);
		sendMobTargetsStatus(ctx, enabled);
		return enabled ? 1 : 0;
	}

	private static int mobTargetsStatus(CommandContext<CommandSourceStack> ctx) {
		boolean enabled = AdminAbilityDebug.playerOnlyAbilitiesTargetMobs();
		sendMobTargetsStatus(ctx, enabled);
		return enabled ? 1 : 0;
	}

	private static void sendMobTargetsStatus(CommandContext<CommandSourceStack> ctx, boolean enabled) {
		ctx.getSource().sendSuccess(() -> Component.translatable(enabled
				? "commands.superheroes.debug.mob_targets.enabled"
				: "commands.superheroes.debug.mob_targets.disabled"), true);
	}

	private static int listAbilities(CommandContext<CommandSourceStack> ctx) {
		ServerPlayer player = playerOrNull(ctx);
		if (player == null) {
			return 0;
		}
		HeroData data = player.getAttachedOrCreate(CoreAttachments.HERO_DATA);
		if (!data.hasHero()) {
			ctx.getSource().sendFailure(Component.translatable("commands.superheroes.no_hero"));
			return 0;
		}
		Hero hero = Heroes.get(data.heroId());
		if (hero == null) {
			return 0;
		}
		ctx.getSource().sendSuccess(() -> Component.translatable("commands.superheroes.abilities.header",
				hero.getId().toString()), false);
		for (ResourceLocation abilityId : hero.getAbilities()) {
			boolean active = data.isActive(abilityId);
			ctx.getSource().sendSuccess(() -> Component.translatable("commands.superheroes.abilities.row",
					abilityId.toString(), data.binding(abilityId, hero.getDefaultBinding(abilityId)).name(),
					active ? "ON" : "OFF"), false);
		}
		return hero.getAbilities().size();
	}

	private static int info(CommandContext<CommandSourceStack> ctx) {
		ServerPlayer player = playerOrNull(ctx);
		if (player == null) {
			return 0;
		}
		HeroData data = player.getAttachedOrCreate(CoreAttachments.HERO_DATA);
		String heroId = data.hasHero() ? data.heroId().toString() : "<none>";
		ctx.getSource().sendSuccess(() -> Component.translatable("commands.superheroes.info",
				heroId, String.format("%.1f", data.energy()), String.format("%.1f", data.mana()),
				data.activeAbilities().size()), false);
		return 1;
	}

	private static int toggleAdminBuild(CommandContext<CommandSourceStack> ctx) {
		ServerPlayer player = playerOrNull(ctx);
		if (player == null) return 0;
		boolean current = player.getAttachedOrCreate(AdminAttachments.ADMIN_BUILD);
		return setAdminBuild(ctx, !current);
	}

	private static int setAdminBuild(CommandContext<CommandSourceStack> ctx, boolean enabled) {
		ServerPlayer player = playerOrNull(ctx);
		if (player == null) return 0;
		player.setAttached(AdminAttachments.ADMIN_BUILD, enabled);
		AdminBuildSyncController.send(player);
		if (enabled) {
			ctx.getSource().sendSuccess(() -> Component.translatable("commands.superheroes.admin_build.on"), false);
		} else {
			ctx.getSource().sendSuccess(() -> Component.translatable("commands.superheroes.admin_build.off"), false);
		}
		return enabled ? 1 : 0;
	}

	private static int giveAllAdminItems(CommandContext<CommandSourceStack> ctx) {
		ServerPlayer player = playerOrNull(ctx);
		if (player == null) return 0;
		boolean adminEnabled = player.getAttachedOrCreate(AdminAttachments.ADMIN_BUILD);
		if (!adminEnabled) {
			ctx.getSource().sendFailure(Component.translatable("commands.superheroes.admin_build.required"));
			return 0;
		}
		int count = 0;
		for (Item item : ModItemGroups.ADMIN_ONLY_ITEMS) {
			ItemStack stack = new ItemStack(item);
			if (!player.getInventory().add(stack)) {
				player.drop(stack, false);
			}
			count++;
		}
		int finalCount = count;
		ctx.getSource().sendSuccess(() -> Component.translatable("commands.superheroes.admin_build.given", finalCount), false);
		return count;
	}

	// ───────────────────────────── vfx showcase / debug ─────────────────

	/** Broadcasts the requested effect at the caller's look target so every nearby client sees it. */
	private static int vfxPlay(CommandContext<CommandSourceStack> ctx, float scale) {
		ServerPlayer player = playerOrNull(ctx);
		if (player == null) {
			return 0;
		}
		ResourceLocation effect = ResourceLocationArgument.getId(ctx, "effect");
		Vec3 target = player.pick(VFX_PLAY_RANGE, 0f, false).getLocation();
		VfxFx.eventAround(player.serverLevel(), effect, target, target, scale, VFX_BROADCAST_RADIUS);
		ctx.getSource().sendSuccess(() -> Component.translatable("commands.superheroes.vfx.play",
				effect.toString(), String.format("%.2f", scale)), true);
		return 1;
	}

	private static int vfxScene(CommandContext<CommandSourceStack> ctx, int count) {
		ServerPlayer player = playerOrNull(ctx);
		if (player == null) {
			return 0;
		}
		ResourceLocation id = ResourceLocationArgument.getId(ctx, "id");
		VfxShowcases.VfxShowcase scene = VfxShowcases.get(id);
		if (scene == null) {
			ctx.getSource().sendFailure(Component.translatable(
					"commands.superheroes.vfx.scene.unknown", id.toString()));
			return 0;
		}
		scene.run(player, count);
		ctx.getSource().sendSuccess(() -> Component.translatable(
				"commands.superheroes.vfx.scene.done", id.toString(), count), true);
		return count;
	}

	private static int vfxStress(CommandContext<CommandSourceStack> ctx, int count) {
		ServerPlayer player = playerOrNull(ctx);
		if (player == null) {
			return 0;
		}
		if (count > STRESS_CAP) {
			ctx.getSource().sendFailure(Component.translatable(
					"commands.superheroes.vfx.stress.too_many", count));
			return 0;
		}
		ResourceLocation id = VfxShowcases.get(STRESS_SCENE) != null ? STRESS_SCENE
				: VfxShowcases.ids().stream().min(Comparator.comparing(ResourceLocation::toString))
						.orElse(null);
		if (id == null) {
			ctx.getSource().sendFailure(Component.translatable(
					"commands.superheroes.vfx.scene.unknown", STRESS_SCENE.toString()));
			return 0;
		}
		VfxShowcases.get(id).run(player, count);
		final ResourceLocation ran = id;
		ctx.getSource().sendSuccess(() -> Component.translatable(
				"commands.superheroes.vfx.stress.done", ran.toString(), count), true);
		return count;
	}

	/** Sends the {@code superheroes:debug/hud} one-shot to the caller — the client factory flips the HUD. */
	private static int vfxHud(CommandContext<CommandSourceStack> ctx, boolean enabled) {
		ServerPlayer player = playerOrNull(ctx);
		if (player == null) {
			return 0;
		}
		Vec3 pos = player.position();
		ServerPlayNetworking.send(player, new VfxEventS2CPayload(DEBUG_HUD_EFFECT,
				VfxEventS2CPayload.NO_SOURCE, pos, pos, enabled ? 1f : 0f,
				player.level().random.nextInt()));
		ctx.getSource().sendSuccess(() -> Component.translatable("commands.superheroes.vfx.hud",
				Component.translatable(enabled
						? "commands.superheroes.state.on"
						: "commands.superheroes.state.off")), false);
		return enabled ? 1 : 0;
	}

	private static ServerPlayer playerOrNull(CommandContext<CommandSourceStack> ctx) {
		if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
			ctx.getSource().sendFailure(Component.translatable("commands.superheroes.not_a_player"));
			return null;
		}
		return player;
	}
}
