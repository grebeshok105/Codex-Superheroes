package io.github.grebeshok105.codex.datagen;

import io.github.grebeshok105.codex.damage.ModDamageTypes;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;

import java.util.concurrent.CompletableFuture;

public final class ModDamageTypeTagProvider extends FabricTagProvider<DamageType> {
	public ModDamageTypeTagProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
		super(output, net.minecraft.core.registries.Registries.DAMAGE_TYPE, registriesFuture);
	}

	@Override
	protected void addTags(HolderLookup.Provider registries) {
		getOrCreateTagBuilder(DamageTypeTags.BYPASSES_ARMOR).add(ModDamageTypes.EYE_LASER);

		// Аудит B20: урон способностей не должен резаться i-frames (invulnerableTime).
		// Исключения — атаки мобов ближнего боя, которые следуют ванильным правилам.
		getOrCreateTagBuilder(DamageTypeTags.BYPASSES_COOLDOWN).add(
				ModDamageTypes.EYE_LASER,
				ModDamageTypes.REPULSOR,
				ModDamageTypes.UNIBEAM,
				ModDamageTypes.COUNTER_STRIKE,
				ModDamageTypes.LION_ROAR,
				ModDamageTypes.DOOMSDAY_SMASH,
				ModDamageTypes.DOOMSDAY_ROAR,
				ModDamageTypes.DOOMSDAY_BONE_SPIKE,
				ModDamageTypes.DOOMSDAY_CHARGE_TACKLE,
				ModDamageTypes.DOOMSDAY_DOOM_GRIP,
				ModDamageTypes.GOKU_KAMEHAMEHA,
				ModDamageTypes.GOKU_INSTANT_STRIKE,
				ModDamageTypes.GOKU_SPIRIT_BOMB,
				ModDamageTypes.NARUTO_RASENGAN,
				ModDamageTypes.NARUTO_RASENSHURIKEN,
				ModDamageTypes.NARUTO_BIJUUDAMA,
				ModDamageTypes.KRATOS_BLADE,
				ModDamageTypes.KRATOS_LEVIATHAN,
				ModDamageTypes.LOKI_CHAOS,
				ModDamageTypes.THANOS_SNAP,
				ModDamageTypes.THANOS_COSMIC_SLAM,
				ModDamageTypes.THANOS_MIND_PULSE,
				ModDamageTypes.THANOS_REALITY_TEAR,
				ModDamageTypes.CAP_SHIELD_THROW,
				ModDamageTypes.CAP_SHIELD_SLAM,
				ModDamageTypes.HOMELANDER_EYE_LASER,
				ModDamageTypes.HOMELANDER_HEAT_VISION,
				ModDamageTypes.HOMELANDER_HAND_CLAP,
				ModDamageTypes.HOMELANDER_SONIC_SLAM,
				ModDamageTypes.HOMELANDER_SHOCKWAVE_DIVE,
				ModDamageTypes.HOMELANDER_LIGHTNING_CALL,
				ModDamageTypes.HOMELANDER_ROAR_BOSS,
				ModDamageTypes.SPACE_CRUSH
		);
	}
}
