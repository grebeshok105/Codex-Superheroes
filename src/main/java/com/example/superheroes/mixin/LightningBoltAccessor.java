package com.example.superheroes.mixin;

import net.minecraft.world.entity.LightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LightningBolt.class)
public interface LightningBoltAccessor {
	@Accessor("seed")
	long superheroes$getSeed();

	@Accessor("life")
	int superheroes$getLife();
}
