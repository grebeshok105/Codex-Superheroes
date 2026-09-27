package io.github.grebeshok105.codex.hero.doomsday;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.core.transform.HeroData;
import io.github.grebeshok105.codex.hero.doomsday.runtime.DoomsdayTierController;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /superheroes doomsday tier ...} — registered from the module via
 * {@code CommandRegistrationCallback} (BattleBeast precedent). The "superheroes" root
 * literal merges with the tree {@code SuperheroesCommands} registers, whichever order
 * the callbacks fire in.
 */
final class DoomsdayCommands {
	private DoomsdayCommands() {
	}

	static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("superheroes")
				.requires(src -> src.hasPermission(2))
				.then(Commands.literal("doomsday")
						.then(Commands.literal("tier")
								.then(Commands.argument("tier", IntegerArgumentType.integer(1, 7))
										.executes(ctx -> setDoomsdayTier(ctx, playerOrNull(ctx),
												IntegerArgumentType.getInteger(ctx, "tier"))))
								.then(Commands.argument("target", EntityArgument.player())
										.then(Commands.argument("tier", IntegerArgumentType.integer(1, 7))
												.executes(DoomsdayCommands::setDoomsdayTierForTarget))))));
	}

	private static int setDoomsdayTierForTarget(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		return setDoomsdayTier(ctx, EntityArgument.getPlayer(ctx, "target"),
				IntegerArgumentType.getInteger(ctx, "tier"));
	}

	private static int setDoomsdayTier(CommandContext<CommandSourceStack> ctx, ServerPlayer target, int tier) {
		if (target == null) {
			return 0;
		}
		HeroData data = target.getAttachedOrCreate(CoreAttachments.HERO_DATA);
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

	private static ServerPlayer playerOrNull(CommandContext<CommandSourceStack> ctx) {
		if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
			ctx.getSource().sendFailure(Component.translatable("commands.superheroes.not_a_player"));
			return null;
		}
		return player;
	}
}
