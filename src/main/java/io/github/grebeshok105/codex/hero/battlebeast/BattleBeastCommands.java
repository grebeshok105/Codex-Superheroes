package io.github.grebeshok105.codex.hero.battlebeast;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.grebeshok105.codex.core.attachment.CoreAttachments;
import io.github.grebeshok105.codex.core.model.HeroData;
import io.github.grebeshok105.codex.hero.battlebeast.runtime.BattleBeastCurseController;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /superheroes battle_beast stage|tier ...} — registered from the module until the
 * commands() context seam lands in I4d. The "superheroes" root literal merges with the tree
 * {@code SuperheroesCommands} registers, whichever order the callbacks fire in.
 */
final class BattleBeastCommands {
	private BattleBeastCommands() {
	}

	static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("superheroes")
				.requires(src -> src.hasPermission(2))
				.then(Commands.literal("battle_beast")
						.then(Commands.literal("stage")
								.then(Commands.argument("stage", IntegerArgumentType.integer(0, BattleBeastCurseController.MAX_STAGE))
										.executes(ctx -> setBattleBeastStage(ctx, playerOrNull(ctx),
												IntegerArgumentType.getInteger(ctx, "stage"))))
								.then(Commands.argument("target", EntityArgument.player())
										.then(Commands.argument("stage", IntegerArgumentType.integer(0, BattleBeastCurseController.MAX_STAGE))
												.executes(BattleBeastCommands::setBattleBeastStageForTarget))))
						.then(Commands.literal("tier")
								.then(Commands.argument("tier", IntegerArgumentType.integer(0, BattleBeastCurseController.MAX_STAGE))
										.executes(ctx -> setBattleBeastStage(ctx, playerOrNull(ctx),
												IntegerArgumentType.getInteger(ctx, "tier"))))
								.then(Commands.argument("target", EntityArgument.player())
										.then(Commands.argument("tier", IntegerArgumentType.integer(0, BattleBeastCurseController.MAX_STAGE))
												.executes(BattleBeastCommands::setBattleBeastTierForTarget))))));
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
		HeroData data = target.getAttachedOrCreate(CoreAttachments.HERO_DATA);
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

	private static ServerPlayer playerOrNull(CommandContext<CommandSourceStack> ctx) {
		if (!(ctx.getSource().getEntity() instanceof ServerPlayer player)) {
			ctx.getSource().sendFailure(Component.translatable("commands.superheroes.not_a_player"));
			return null;
		}
		return player;
	}
}
