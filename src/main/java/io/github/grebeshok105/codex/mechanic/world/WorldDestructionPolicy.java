package io.github.grebeshok105.codex.mechanic.world;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.ability.Ability;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Single chokepoint for ability-driven world changes. Every block removal or
 * terrain modification caused by a hero ability or boss behaviour must go
 * through here so protection rules apply uniformly:
 *
 * <ul>
 *   <li>unbreakable blocks ({@code destroySpeed < 0}) and anything tagged
 *   {@code superheroes:ability_immune} are never touched;</li>
 *   <li>player-caused removals honour {@link ServerLevel#mayInteract} (spawn
 *   protection, world border) and Fabric's {@link PlayerBlockBreakEvents}
 *   lifecycle, which is what claim/protection mods hook into;</li>
 *   <li>non-player causes (mobs, projectiles, environment) honour
 *   {@link GameRules#RULE_MOBGRIEFING}, matching vanilla mob-griefing
 *   semantics.</li>
 * </ul>
 */
public final class WorldDestructionPolicy {
	/** Blocks abilities never touch, beyond the vanilla unbreakable check. */
	public static final TagKey<Block> ABILITY_IMMUNE =
			TagKey.create(Registries.BLOCK, ModId.of("ability_immune"));

	private WorldDestructionPolicy() {
	}

	/** Whether an ability may remove this block at all, ignoring who caused it. */
	public static boolean mayDestroy(ServerLevel level, BlockPos pos, BlockState state) {
		if (state.isAir() || state.is(ABILITY_IMMUNE)) {
			return false;
		}
		return state.getDestroySpeed(level, pos) >= 0f;
	}

	/**
	 * Same gate plus a caller's own hardness cap (exclusive upper bound,
	 * matching the call-site conventions this replaces).
	 */
	public static boolean mayDestroy(
			ServerLevel level, BlockPos pos, BlockState state, float maxHardness) {
		return mayDestroy(level, pos, state)
				&& state.getDestroySpeed(level, pos) < maxHardness;
	}

	/**
	 * Vanilla-consistent break: plays break FX, honours the drop flag, and emits
	 * the block-destroy game event via {@code Level.destroyBlock}.
	 */
	public static boolean tryBreak(
			ServerLevel level, BlockPos pos, boolean drop, @Nullable Entity cause) {
		BlockState state = level.getBlockState(pos);
		if (!mayDestroy(level, pos, state)) {
			return false;
		}
		BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
		if (!allowsBreak(level, pos, state, blockEntity, cause)) {
			return false;
		}
		boolean broken = level.destroyBlock(pos, drop, cause);
		if (broken) {
			notifyBreak(level, pos, state, blockEntity, cause);
		}
		return broken;
	}

	/**
	 * Quiet carve for paths that intentionally suppress drops and break FX
	 * (crater shaping, boss projectile pickup). Still runs the full protection
	 * checks so claim/protection mods can veto.
	 */
	public static boolean tryCarve(ServerLevel level, BlockPos pos, @Nullable Entity cause) {
		BlockState state = level.getBlockState(pos);
		if (!mayDestroy(level, pos, state)) {
			return false;
		}
		BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
		if (!allowsBreak(level, pos, state, blockEntity, cause)) {
			return false;
		}
		boolean carved = level.setBlock(pos, Blocks.AIR.defaultBlockState(),
				Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
		if (carved) {
			notifyBreak(level, pos, state, blockEntity, cause);
		}
		return carved;
	}

	/**
	 * Ability terrain modification that does not remove a block (e.g. placing
	 * fire). Fires no break events — only the interaction / mob-griefing gates
	 * apply.
	 */
	public static boolean tryPlace(
			ServerLevel level, BlockPos pos, BlockState newState, @Nullable Entity cause) {
		if (!allowsModify(level, pos, cause)) {
			return false;
		}
		return level.setBlockAndUpdate(pos, newState);
	}

	private static boolean allowsBreak(ServerLevel level, BlockPos pos, BlockState state,
			@Nullable BlockEntity blockEntity, @Nullable Entity cause) {
		if (cause instanceof Player player) {
			if (!level.mayInteract(player, pos)) {
				return false;
			}
			if (!PlayerBlockBreakEvents.BEFORE.invoker()
					.beforeBlockBreak(level, player, pos, state, blockEntity)) {
				PlayerBlockBreakEvents.CANCELED.invoker()
						.onBlockBreakCanceled(level, player, pos, state, blockEntity);
				return false;
			}
			return true;
		}
		return level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
	}

	private static boolean allowsModify(ServerLevel level, BlockPos pos, @Nullable Entity cause) {
		if (cause instanceof Player player) {
			return level.mayInteract(player, pos);
		}
		return level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
	}

	private static void notifyBreak(ServerLevel level, BlockPos pos, BlockState state,
			@Nullable BlockEntity blockEntity, @Nullable Entity cause) {
		if (cause instanceof Player player) {
			PlayerBlockBreakEvents.AFTER.invoker()
					.afterBlockBreak(level, player, pos, state, blockEntity);
		}
	}
}
