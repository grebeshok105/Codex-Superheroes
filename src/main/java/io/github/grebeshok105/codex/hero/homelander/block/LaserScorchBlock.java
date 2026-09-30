package io.github.grebeshok105.codex.hero.homelander.block;

import com.mojang.serialization.MapCodec;
import io.github.grebeshok105.codex.core.content.ModContent;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.MultifaceSpreader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Eye-laser burn mark: a scorched face decal that lives in the air cell in
 * front of the block the beam hit, exactly like glow lichen. It is the
 * persistence mechanism by design — a plain block state, so marks survive
 * chunk unload and world restarts with zero ticking overhead, and vanilla
 * {@code MultifaceBlock#updateShape} peels a face off when its support block
 * goes away. Breaks instantly (no loot) since it is residue, not terrain.
 */
public class LaserScorchBlock extends MultifaceBlock {
	public static final LaserScorchBlock INSTANCE = ModContent.block("laser_scorch",
			new LaserScorchBlock(BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_BLACK)
					.noCollission()
					.instabreak()
					.replaceable()
					.noLootTable()
					.pushReaction(PushReaction.DESTROY)
					.sound(SoundType.GLOW_LICHEN)));

	public static final MapCodec<LaserScorchBlock> CODEC = simpleCodec(LaserScorchBlock::new);

	private final MultifaceSpreader spreader = new MultifaceSpreader(this);

	public LaserScorchBlock(Properties properties) {
		super(properties);
		BlockState state = defaultBlockState();
		for (Direction direction : Direction.values()) {
			state = state.setValue(getFaceProperty(direction), false);
		}
		registerDefaultState(state);
	}

	@Override
	protected MapCodec<? extends MultifaceBlock> codec() {
		return CODEC;
	}

	@Override
	public MultifaceSpreader getSpreader() {
		return spreader;
	}
}
