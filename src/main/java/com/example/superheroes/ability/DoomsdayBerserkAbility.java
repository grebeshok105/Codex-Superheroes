package com.example.superheroes.ability;

import com.example.superheroes.hero.HeroAttributes;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class DoomsdayBerserkAbility implements Ability {
	@Override
	public ResourceLocation getId() {
		return AbilityIds.DOOMSDAY_BERSERK;
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
		return 1.0f;
	}

	@Override
	public boolean tryActivate(ServerPlayer player) {
		applyBuff(player);
		ServerLevel level = player.serverLevel();
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				com.example.superheroes.sound.ModSounds.DOOMSDAY_ROAR, SoundSource.PLAYERS, 1.2f, 0.85f);
		return true;
	}

	@Override
	public void onTickActive(ServerPlayer player) {
		if (player.tickCount % 4 == 0) {
			ServerLevel level = player.serverLevel();
			level.sendParticles(ParticleTypes.ANGRY_VILLAGER,
					player.getX(), player.getY() + player.getBbHeight() * 0.85, player.getZ(),
					1, 0.3, 0.1, 0.3, 0.0);
			level.sendParticles(ParticleTypes.SMALL_FLAME,
					player.getX(), player.getY() + 0.4, player.getZ(),
					2, 0.5, 0.2, 0.5, 0.01);
		}
	}

	@Override
	public void onDeactivate(ServerPlayer player) {
		clearBuff(player);
	}

	private static void applyBuff(ServerPlayer player) {
		modify(player, Attributes.ATTACK_DAMAGE, HeroAttributes.DOOMSDAY_BERSERK_DAMAGE,
				12.0, AttributeModifier.Operation.ADD_VALUE);
		modify(player, Attributes.ARMOR, HeroAttributes.DOOMSDAY_BERSERK_ARMOR,
				-15.0, AttributeModifier.Operation.ADD_VALUE);
		modify(player, Attributes.MOVEMENT_SPEED, HeroAttributes.DOOMSDAY_BERSERK_SPEED,
				0.15, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
	}

	public static void clearBuff(ServerPlayer player) {
		remove(player, Attributes.ATTACK_DAMAGE, HeroAttributes.DOOMSDAY_BERSERK_DAMAGE);
		remove(player, Attributes.ARMOR, HeroAttributes.DOOMSDAY_BERSERK_ARMOR);
		remove(player, Attributes.MOVEMENT_SPEED, HeroAttributes.DOOMSDAY_BERSERK_SPEED);
	}

	private static void modify(ServerPlayer player, Holder<Attribute> attribute,
			ResourceLocation id, double amount, AttributeModifier.Operation op) {
		AttributeInstance inst = player.getAttribute(attribute);
		if (inst == null) return;
		inst.addOrReplacePermanentModifier(new AttributeModifier(id, amount, op));
	}

	private static void remove(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id) {
		AttributeInstance inst = player.getAttribute(attribute);
		if (inst == null) return;
		inst.removeModifier(id);
	}
}
