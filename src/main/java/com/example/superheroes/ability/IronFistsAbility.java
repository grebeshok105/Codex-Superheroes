package com.example.superheroes.ability;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.effect.IronFistsController;
import com.example.superheroes.hero.Hero;
import com.example.superheroes.hero.Heroes;
import com.example.superheroes.network.ModNetworking;
import com.example.superheroes.resource.EnergyLocks;
import com.example.superheroes.sound.ModSounds;
import com.example.superheroes.transform.HeroData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class IronFistsAbility implements Ability {
	public static final int DURATION_TICKS = 200;
	public static final float MELEE_DAMAGE = 5.0f;
	public static final double MELEE_KNOCKBACK = 2.5;

	@Override
	public ResourceLocation getId() {
		return AbilityIds.IRON_FISTS;
	}

	@Override
	public boolean isToggle() {
		return true;
	}

	@Override
	public float costOnActivate() {
		return 0f;
	}

	@Override
	public float costPerTick() {
		return 0f;
	}

	@Override
	public boolean canActivate(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		if (!data.hasHero()) return false;
		Hero hero = Heroes.get(data.heroId());
		if (hero == null) return false;
		return data.energy() >= hero.getEnergyMax() - 0.001f;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
		HeroData drained = data.withResources(0f, data.mana());
		player.setAttached(ModAttachments.HERO_DATA, drained);
		ModNetworking.syncResources(player, drained);

		EnergyLocks.lockTicks(player, DURATION_TICKS);
		IronFistsController.markActivated(player);

		ServerLevel level = player.serverLevel();
		Vec3 p = player.position();
		level.playSound(null, p.x, p.y, p.z, ModSounds.HOMELANDER_IRON_FISTS_IMPACT,
				SoundSource.PLAYERS, 1.2f, 0.9f);
		level.playSound(null, p.x, p.y, p.z, ModSounds.HOMELANDER_IRON_FISTS_CHARGE,
				SoundSource.PLAYERS, 1.0f, 1.0f);
		level.sendParticles(ParticleTypes.FLASH, p.x, p.y + 1.0, p.z, 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.END_ROD, p.x, p.y + 1.2, p.z, 30, 0.6, 0.6, 0.6, 0.05);
		return true;
	}

	@Override
	public void onTickActive(ServerPlayer player) {
		IronFistsController.tickActive(player);
	}

	@Override
	public void onDeactivate(ServerPlayer player) {
		IronFistsController.markDeactivated(player);
		ServerLevel level = player.serverLevel();
		Vec3 p = player.position();
		level.sendParticles(ParticleTypes.END_ROD, p.x, p.y + 1.0, p.z,
				24, 0.5, 0.5, 0.5, 0.08);
	}
}
