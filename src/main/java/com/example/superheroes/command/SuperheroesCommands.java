package com.example.superheroes.command;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.debug.AdminAbilityDebug;
import com.example.superheroes.effect.BattleBeastCurseController;
import com.example.superheroes.effect.DoomsdayTierController;
import com.example.superheroes.hero.BattleBeastHero;
import com.example.superheroes.hero.DoomsdayHero;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.Heroes;
import com.example.superheroes.item.ModItemGroups;
import com.example.superheroes.network.ModNetworking;
import com.example.superheroes.transform.HeroData;
import com.example.superheroes.transform.HeroTransformService;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class SuperheroesCommands {
	private static final SuggestionProvider<CommandSourceStack> HERO_SUGGESTIONS =
			(ctx, builder) -> SharedSuggestionProvider.suggestResource(Heroes.all().keySet(), builder);

	private SuperheroesCommands() {
	}

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) ->
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
						.then(Commands.literal("doomsday")
								.then(Commands.literal("tier")
										.then(Commands.argument("tier", IntegerArgumentType.integer(1, 7))
												.executes(ctx -> setDoomsdayTier(ctx, playerOrNull(ctx),
														IntegerArgumentType.getInteger(ctx, "tier"))))
										.then(Commands.argument("target", EntityArgument.player())
												.then(Commands.argument("tier", IntegerArgumentType.integer(1, 7))
														.executes(SuperheroesCommands::setDoomsdayTierForTarget)))))
						.then(Commands.literal("battle_beast")
								.then(Commands.literal("stage")
										.then(Commands.argument("stage", IntegerArgumentType.integer(0, BattleBeastCurseController.MAX_STAGE))
												.executes(ctx -> setBattleBeastStage(ctx, playerOrNull(ctx),
														IntegerArgumentType.getInteger(ctx, "stage"))))
										.then(Commands.argument("target", EntityArgument.player())
												.then(Commands.argument("stage", IntegerArgumentType.integer(0, BattleBeastCurseController.MAX_STAGE))
														.executes(SuperheroesCommands::setBattleBeastStageForTarget))))
								.then(Commands.literal("tier")
										.then(Commands.argument("tier", IntegerArgumentType.integer(0, BattleBeastCurseController.MAX_STAGE))
												.executes(ctx -> setBattleBeastStage(ctx, playerOrNull(ctx),
														IntegerArgumentType.getInteger(ctx, "tier"))))
										.then(Commands.argument("target", EntityArgument.player())
												.then(Commands.argument("tier", IntegerArgumentType.integer(0, BattleBeastCurseController.MAX_STAGE))
														.executes(SuperheroesCommands::setBattleBeastTierForTarget)))))
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
						.then(Commands.literal("abilities")
								.executes(SuperheroesCommands::listAbilities))
						.then(Commands.literal("info")
								.executes(SuperheroesCommands::info))));
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
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (!data.hasHero()) {
			ctx.getSource().sendFailure(Component.translatable("commands.superheroes.no_hero"));
			return 0;
		}
		Hero hero = Heroes.get(data.heroId());
		float clamped = hero == null ? amount : Math.min(amount, hero.getEnergyMax());
		HeroData updated = data.withEnergy(clamped);
		player.setAttached(ModAttachments.HERO_DATA, updated);
		ModNetworking.syncResources(player, updated);
		ctx.getSource().sendSuccess(() -> Component.translatable("commands.superheroes.energy.set",
				String.format("%.1f", clamped)), false);
		return (int) clamped;
	}

	private static int setMana(CommandContext<CommandSourceStack> ctx, float amount) {
		ServerPlayer player = playerOrNull(ctx);
		if (player == null) {
			return 0;
		}
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (!data.hasHero()) {
			ctx.getSource().sendFailure(Component.translatable("commands.superheroes.no_hero"));
			return 0;
		}
		Hero hero = Heroes.get(data.heroId());
		float clamped = hero == null ? amount : Math.min(amount, hero.getManaMax());
		HeroData updated = data.withMana(clamped);
		player.setAttached(ModAttachments.HERO_DATA, updated);
		ModNetworking.syncResources(player, updated);
		ctx.getSource().sendSuccess(() -> Component.translatable("commands.superheroes.mana.set",
				String.format("%.1f", clamped)), false);
		return (int) clamped;
	}

	private static int setDoomsdayTierForTarget(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		return setDoomsdayTier(ctx, EntityArgument.getPlayer(ctx, "target"),
				IntegerArgumentType.getInteger(ctx, "tier"));
	}

	private static int setDoomsdayTier(CommandContext<CommandSourceStack> ctx, ServerPlayer target, int tier) {
		if (target == null) {
			return 0;
		}
		HeroData data = target.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (!data.hasHero() || !DoomsdayHero.ID.equals(data.heroId())) {
			ctx.getSource().sendFailure(Component.translatable("commands.superheroes.doomsday.not_doomsday",
					target.getScoreboardName()));
			return 0;
		}
		DoomsdayTierController.setTier(target, tier);
		ctx.getSource().sendSuccess(() -> Component.translatable("commands.superheroes.doomsday.tier.set",
				target.getScoreboardName(), String.valueOf(tier)), true);
		return tier;
	}

	private static int setBattleBeastStageForTarget(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		return setBattleBeastStage(ctx, EntityArgument.getPlayer(ctx, "target"),
				IntegerArgumentType.getInteger(ctx, "stage"));
	}

	private static int setBattleBeastTierForTarget(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		return setBattleBeastStage(ctx, EntityArgument.getPlayer(ctx, "target"),
				IntegerArgumentType.getInteger(ctx, "tier"));
	}

	private static int setBattleBeastStage(CommandContext<CommandSourceStack> ctx, ServerPlayer target, int stage) {
		if (target == null) {
			return 0;
		}
		HeroData data = target.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (!data.hasHero() || !BattleBeastHero.ID.equals(data.heroId())) {
			ctx.getSource().sendFailure(Component.literal("Target is not Battle Beast: "
					+ target.getScoreboardName()));
			return 0;
		}
		int applied = BattleBeastCurseController.setStage(target, stage);
		ctx.getSource().sendSuccess(() -> Component.literal("Set Battle Beast curse stage for "
				+ target.getScoreboardName() + " to " + applied), true);
		return applied;
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
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
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
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		String heroId = data.hasHero() ? data.heroId().toString() : "<none>";
		ctx.getSource().sendSuccess(() -> Component.translatable("commands.superheroes.info",
				heroId, String.format("%.1f", data.energy()), String.format("%.1f", data.mana()),
				data.activeAbilities().size()), false);
		return 1;
	}

	private static int toggleAdminBuild(CommandContext<CommandSourceStack> ctx) {
		ServerPlayer player = playerOrNull(ctx);
		if (player == null) return 0;
		boolean current = player.getAttachedOrCreate(ModAttachments.ADMIN_BUILD);
		return setAdminBuild(ctx, !current);
	}

	private static int setAdminBuild(CommandContext<CommandSourceStack> ctx, boolean enabled) {
		ServerPlayer player = playerOrNull(ctx);
		if (player == null) return 0;
		player.setAttached(ModAttachments.ADMIN_BUILD, enabled);
		if (enabled) {
			ctx.getSource().sendSuccess(() -> Component.literal("§6[ADMIN BUILD: ON] §fАдмин-контент разблокирован. Используй /superheroes admin give для получения предметов."), false);
		} else {
			ctx.getSource().sendSuccess(() -> Component.literal("§7[ADMIN BUILD: OFF] §fАдмин-контент скрыт."), false);
		}
		return enabled ? 1 : 0;
	}

	private static int giveAllAdminItems(CommandContext<CommandSourceStack> ctx) {
		ServerPlayer player = playerOrNull(ctx);
		if (player == null) return 0;
		boolean adminEnabled = player.getAttachedOrCreate(ModAttachments.ADMIN_BUILD);
		if (!adminEnabled) {
			ctx.getSource().sendFailure(Component.literal("§cАдмин-билд выключен. Сначала включи: /superheroes admin on"));
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
		ctx.getSource().sendSuccess(() -> Component.literal("§6[ADMIN] §fВыдано " + finalCount + " админ-предметов."), false);
		return count;
	}

	private static ServerPlayer playerOrNull(CommandContext<CommandSourceStack> ctx) {
		if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
			ctx.getSource().sendFailure(Component.translatable("commands.superheroes.not_a_player"));
			return null;
		}
		return player;
	}
}
