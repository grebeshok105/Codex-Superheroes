package com.example.superheroes.item;

import com.example.superheroes.attachment.ModAttachments;
import com.example.superheroes.effect.RaidenState;
import com.example.superheroes.hero.RaidenHero;
import com.example.superheroes.particle.ModParticles;
import com.example.superheroes.transform.HeroData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Musou no Hitotachi — Yamato в фиолетовом окрасе Райден Сёгун.
 * Базовый удар — около ванильного netherite. Бонусы стэкаются от состояния:
 *   • Глаз Грозного Суда (E активен) — каждый удар вызывает цепные молнии
 *     по нескольким целям в радиусе + увеличенный урон.
 *   • Musou Shinsetsu (Q активен) — обычный удар бьёт +50% сильнее, всё в радиусе 4 блока тоже получает урон.
 */
public class MusouNoHitotachiItem extends SwordItem {
	public MusouNoHitotachiItem(Properties properties) {
		super(Tiers.NETHERITE, properties.attributes(SwordItem.createAttributes(Tiers.NETHERITE, 6, -2.4f)));
	}

	private static final float EYE_BONUS_PLAYER = 14.0f;
	private static final float EYE_BONUS_MOB = 8.0f;
	private static final double EYE_AOE_RADIUS = 5.0;
	private static final float EYE_AOE_DAMAGE = 6.0f;
	private static final int EYE_CHAIN_MAX = 4;

	private static final float BURST_BONUS_PLAYER = 12.0f;
	private static final float BURST_BONUS_MOB = 6.0f;
	private static final double BURST_AOE_RADIUS = 4.0;
	private static final float BURST_AOE_DAMAGE = 8.0f;

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		if (attacker instanceof ServerPlayer player) {
			HeroData data = player.getAttachedOrCreate(ModAttachments.HERO_DATA);
			if (RaidenHero.ID.equals(data.heroId())) {
				RaidenState state = player.getAttachedOrCreate(ModAttachments.RAIDEN_STATE);
				long now = player.serverLevel().getGameTime();
				ServerLevel level = player.serverLevel();
				boolean targetIsPlayer = target instanceof net.minecraft.world.entity.player.Player;

				if (state.eyeExpireTick() > now) {
					float bonus = targetIsPlayer ? EYE_BONUS_PLAYER : EYE_BONUS_MOB;
					target.invulnerableTime = 0;
					target.hurt(level.damageSources().playerAttack(player), bonus);

					LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
					if (bolt != null) {
						bolt.moveTo(target.getX(), target.getY(), target.getZ());
						bolt.setVisualOnly(true);
						level.addFreshEntity(bolt);
					}

					level.sendParticles(ModParticles.JIWALD_EFFECT,
							target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
							20, 0.4, 0.6, 0.4, 0.15);
					level.sendParticles(ModParticles.FULA_PARTICLE,
							target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
							12, 0.3, 0.4, 0.3, 0.1);
					level.sendParticles(ModParticles.SPARKS,
							target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
							30, 0.5, 0.7, 0.5, 0.2);
					level.playSound(null, target.getX(), target.getY(), target.getZ(),
							SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8f, 1.4f);

					double r2 = EYE_AOE_RADIUS * EYE_AOE_RADIUS;
					List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
							target.getBoundingBox().inflate(EYE_AOE_RADIUS),
							e -> e != player && e != target && e.isAlive() && !e.isSpectator()
									&& e.distanceToSqr(target) <= r2);
					int chained = 0;
					for (LivingEntity e : nearby) {
						if (chained >= EYE_CHAIN_MAX) break;
						e.invulnerableTime = 0;
						e.hurt(level.damageSources().playerAttack(player), EYE_AOE_DAMAGE);

						LightningBolt chainBolt = EntityType.LIGHTNING_BOLT.create(level);
						if (chainBolt != null) {
							chainBolt.moveTo(e.getX(), e.getY(), e.getZ());
							chainBolt.setVisualOnly(true);
							level.addFreshEntity(chainBolt);
						}
						level.sendParticles(ModParticles.JIWALD_EFFECT,
								e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(),
								10, 0.3, 0.4, 0.3, 0.1);
						chained++;
					}
				}

				if (state.burstExpireTick() > now) {
					float bonus = targetIsPlayer ? BURST_BONUS_PLAYER : BURST_BONUS_MOB;
					target.invulnerableTime = 0;
					target.hurt(level.damageSources().playerAttack(player), bonus);

					double r2 = BURST_AOE_RADIUS * BURST_AOE_RADIUS;
					List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
							target.getBoundingBox().inflate(BURST_AOE_RADIUS),
							e -> e != player && e != target && e.isAlive() && !e.isSpectator()
									&& e.distanceToSqr(target) <= r2);
					for (LivingEntity e : nearby) {
						e.invulnerableTime = 0;
						e.hurt(level.damageSources().playerAttack(player), BURST_AOE_DAMAGE);
					}

					level.sendParticles(ModParticles.MOONVEIL,
							target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
							6, 0.3, 0.3, 0.3, 0.08);
					level.sendParticles(ModParticles.SPARKS,
							target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
							30, 0.6, 0.8, 0.6, 0.25);
					level.sendParticles(ModParticles.SWORD_EXPLOSION,
							target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
							8, 0.4, 0.4, 0.4, 0.1);
					level.playSound(null, target.getX(), target.getY(), target.getZ(),
							SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9f, 0.9f);
				}
			}
		}
		return super.hurtEnemy(stack, target, attacker);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("item.superheroes.musou_no_hitotachi.lore.line1")
				.withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
		tooltip.add(Component.translatable("item.superheroes.musou_no_hitotachi.lore.line2")
				.withStyle(ChatFormatting.DARK_PURPLE));
	}
}
