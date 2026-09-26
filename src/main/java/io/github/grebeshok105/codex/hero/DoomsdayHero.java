package io.github.grebeshok105.codex.hero;

import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.ability.AbilityIds;
import io.github.grebeshok105.codex.core.ability.AbilityAvailability;
import io.github.grebeshok105.codex.core.hero.AttributeModifierSet;
import io.github.grebeshok105.codex.core.hero.BleedProfile;
import io.github.grebeshok105.codex.core.hero.Hero;
import io.github.grebeshok105.codex.core.hero.HeroHudConfig;
import io.github.grebeshok105.codex.core.hero.HeroTheme;
import io.github.grebeshok105.codex.core.hero.ImpactStyle;
import io.github.grebeshok105.codex.core.hero.JarvisThreatClass;
import io.github.grebeshok105.codex.core.hero.LandingImpact;
import io.github.grebeshok105.codex.core.hero.PassiveGlyph;
import io.github.grebeshok105.codex.core.transform.HeroData;
import io.github.grebeshok105.codex.physics.ShockwaveUtil;
import io.github.grebeshok105.codex.core.resource.ResourceKind;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class DoomsdayHero implements Hero {
        public static final ResourceLocation ID = ModId.of("doomsday");
        public static final ResourceLocation SKIN = ModId.of("textures/entity/hero/doomsday.png");

        public static final HeroTheme THEME = new HeroTheme(
                        0xE0140A0A,
                        0xD00A0606,
                        0x88B23A1A,
                        0x33D86238,
                        0xFFE7C7A8,
                        0xFF3A0E0E,
                        0xFFD83A1A,
                        0x55D86238,
                        0xFFD83A1A,
                        0xFF1A0A1F,
                        0xFFB23ACC,
                        0x55B23ACC,
                        0xFFB23ACC,
                        0x55D83A1A,
                        0xFFE7C7A8,
                        0xFFE7C7A8,
                        0xFFFFE0C0,
                        0x55D83A1A
        );

        private static final HeroHudConfig HUD = new HeroHudConfig("hud.superheroes.energy.rage", HeroHudConfig.EnergyIconType.SKULL, true, "DOOM GRIP");

        private static final AttributeModifierSet PASSIVES = AbilityScopedModifiers.DOOMSDAY;

        @Override
        public ResourceLocation getId() {
                return ID;
        }

        @Override
        public float getEnergyMax() {
                return 200f;
        }

        @Override
        public float getEnergyRegenPerTick() {
                return 1.0f;
        }

        @Override
        public float getManaMax() {
                return 0f;
        }

        @Override
        public EntityDimensions getDimensions(Pose pose) {
                return switch (pose) {
                        case CROUCHING -> EntityDimensions.scalable(0.6f, 1.5f).withEyeHeight(1.27f);
                        case SWIMMING, FALL_FLYING, SPIN_ATTACK -> EntityDimensions.scalable(0.6f, 0.6f).withEyeHeight(0.4f);
                        default -> EntityDimensions.scalable(0.6f, 1.8f).withEyeHeight(1.62f);
                };
        }

        @Override
        public List<ResourceLocation> getAbilities() {
                return List.of(
                                AbilityIds.DOOMSDAY_SMASH,
                                AbilityIds.DOOMSDAY_ROAR,
                                AbilityIds.DOOMSDAY_BONE_SPIKE,
                                AbilityIds.DOOMSDAY_CHARGE_TACKLE,
                                AbilityIds.DOOMSDAY_BERSERK,
                                AbilityIds.DOOMSDAY_DOOM_GRIP);
        }

        @Override
        public ResourceKind getDefaultBinding(ResourceLocation abilityId) {
                return ResourceKind.ENERGY;
        }

        @Override
        public AttributeModifierSet passiveAttributes() {
                return PASSIVES;
        }

        @Override
        public void applyPassives(Player player) {
                int tier = getTier(player);
                PASSIVES.remove(player);
                AbilityScopedModifiers.buildDoomsdayTierSet(tier).apply(player);
                applyTierEffects(player, tier);
                if (player instanceof ServerPlayer sp) {
                        io.github.grebeshok105.codex.effect.DoomsdayAdaptationController.reapplyDamageBonus(sp);
                        io.github.grebeshok105.codex.effect.DoomsdayTierController.sync(sp);
                }
        }

        public static void applyTierEffects(Player player, int tier) {
                if (tier >= 7) {
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, -1, 1, true, false, true));
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, -1, 1, true, false, true));
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, -1, 1, true, false, true));
                        player.addEffect(new MobEffectInstance(MobEffects.JUMP, -1, 1, true, false, true));
                        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, -1, 0, true, false, true));
                } else {
                        player.removeEffect(MobEffects.DAMAGE_BOOST);
                        player.removeEffect(MobEffects.JUMP);
                        player.removeEffect(MobEffects.FIRE_RESISTANCE);
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, -1, 0, true, false, true));
                        if (tier >= 5) {
                                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, -1, 0, true, false, true));
                        } else {
                                player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
                        }
                }
        }

        public static int getTier(Player player) {
                io.github.grebeshok105.codex.effect.DoomsdayProgress p = player.getAttachedOrCreate(
                                io.github.grebeshok105.codex.attachment.ModAttachments.DOOMSDAY_PROGRESS);
                return Math.max(1, Math.min(7, p.tier()));
        }

        public boolean isAbilityUnlocked(Player player, ResourceLocation abilityId) {
                return isUnlockedAtTier(getTier(player), abilityId);
        }

        @Override
        public AbilityAvailability.Visibility visibility(ServerPlayer player, ResourceLocation abilityId) {
                return isAbilityUnlocked(player, abilityId)
                                ? AbilityAvailability.Visibility.AVAILABLE
                                : AbilityAvailability.Visibility.HIDDEN;
        }

        /**
         * Tier-gated ability table — shared by the server gate ({@link #isAbilityUnlocked},
         * which reads the live tier from {@code DOOMSDAY_PROGRESS}) and the owner HUD
         * ({@link #visibility}, synced through the {@code ability_availability} attachment).
         */
        public static boolean isUnlockedAtTier(int tier, ResourceLocation abilityId) {
                if (AbilityIds.DOOMSDAY_SMASH.equals(abilityId)) return tier >= 2;
                if (AbilityIds.DOOMSDAY_ROAR.equals(abilityId)) return tier >= 3;
                if (AbilityIds.DOOMSDAY_BONE_SPIKE.equals(abilityId)) return tier >= 4;
                if (AbilityIds.DOOMSDAY_CHARGE_TACKLE.equals(abilityId)) return tier >= 5;
                if (AbilityIds.DOOMSDAY_BERSERK.equals(abilityId)) return tier >= 6;
                if (AbilityIds.DOOMSDAY_DOOM_GRIP.equals(abilityId)) return tier >= 7;
                return false;
        }

        /**
         * Death is the adaptation loop: {@code DoomsdayTierController}'s death hook tiers
         * him up, and {@code removePassives} would reset that progress — so respawn
         * re-applies passives without removing and death never untransforms him.
         */
        @Override
        public boolean keepsHeroOnDeath() {
                return true;
        }

        @Override
        public void reapplyPassivesAfterRespawn(ServerPlayer player) {
                applyPassives(player);
        }

        @Override
        public void removePassives(Player player) {
                PASSIVES.remove(player);
                player.removeEffect(MobEffects.REGENERATION);
                player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
                player.removeEffect(MobEffects.DAMAGE_BOOST);
                player.removeEffect(MobEffects.JUMP);
                player.removeEffect(MobEffects.FIRE_RESISTANCE);
                if (player instanceof ServerPlayer sp) {
                        io.github.grebeshok105.codex.effect.DoomsdayAdaptationController.clear(sp);
                        io.github.grebeshok105.codex.effect.DoomsdayEffectAdaptationController.clear(sp);
                        io.github.grebeshok105.codex.ability.DoomsdayBerserkAbility.clearBuff(sp);
                        io.github.grebeshok105.codex.effect.DoomsdayTierController.resetProgress(sp);
                        io.github.grebeshok105.codex.ability.ChargeTackleAbility.clear(sp);
                        io.github.grebeshok105.codex.effect.DoomGripController.clear(sp);
                }
        }

        @Override
        public boolean cancelsFallDamage(Player player) {
                return true;
        }

        @Override
        public ResourceLocation getSkinTexture() {
                return SKIN;
        }

        @Override
        public HeroTheme getTheme() {
                return THEME;
        }

        @Override
        public void onLanded(ServerPlayer player, LandingImpact impact) {
                float intensity = impact.intensity();
                float scale = 0.40f + intensity * 1.30f;
                double radius = 3.5 + scale * 8.5;
                float damage = 5.0f + scale * 11.0f;
                ShockwaveUtil.detonate(player, player.position(), radius, damage, false);

                ServerLevel level = player.serverLevel();
                double cx = player.getX();
                double cy = player.getY();
                double cz = player.getZ();

                switch (impact.tier()) {
                        case WEAK -> {
                                level.playSound(null, cx, cy, cz, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9f, 0.7f);
                                level.sendParticles(ParticleTypes.LARGE_SMOKE, cx, cy + 0.1, cz, 16, 0.8, 0.1, 0.8, 0.05);
                        }
                        case NORMAL -> {
                                level.playSound(null, cx, cy, cz, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.4f, 0.55f);
                                level.playSound(null, cx, cy, cz, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.9f, 0.55f);
                                level.sendParticles(ParticleTypes.LARGE_SMOKE, cx, cy + 0.1, cz, 36, radius * 0.4, 0.2, radius * 0.4, 0.08);
                                level.sendParticles(ParticleTypes.POOF, cx, cy + 0.1, cz, 32, radius * 0.4, 0.15, radius * 0.4, 0.06);
                        }
                        case EPIC -> {
                                level.playSound(null, cx, cy, cz, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.0f, 0.4f);
                                level.playSound(null, cx, cy, cz, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.4f, 0.5f);
                                level.sendParticles(ParticleTypes.LARGE_SMOKE, cx, cy + 0.1, cz, 80, radius * 0.5, 0.3, radius * 0.5, 0.10);
                                level.sendParticles(ParticleTypes.POOF, cx, cy + 0.1, cz, 60, radius * 0.5, 0.2, radius * 0.5, 0.08);
                                level.sendParticles(ParticleTypes.EXPLOSION, cx, cy + 0.6, cz, 4, 0.6, 0.3, 0.6, 0.0);
                        }
                }
        }

	@Override
	public HeroHudConfig getHudConfig() {
		return HUD;
	}
	@Override
	public io.github.grebeshok105.codex.core.hero.ImpactStyle getImpactStyle() {
		return io.github.grebeshok105.codex.core.hero.ImpactStyle.BRUTAL;
	}
	@Override
	public double getImpactPower() {
		return 1.34;
	}
	@Override
	public JarvisThreatClass getThreatClass() {
		return JarvisThreatClass.S;
	}

        @Override
        public boolean canUseAbility(ServerPlayer player, io.github.grebeshok105.codex.core.transform.HeroData data,
                        ResourceLocation abilityId) {
                return isAbilityUnlocked(player, abilityId);
        }

	@Override
	public List<PassiveGlyph> getPassiveGlyphs() {
		return List.of(PassiveGlyph.FIST, PassiveGlyph.STAR, PassiveGlyph.HEART, PassiveGlyph.BOLT, PassiveGlyph.SKULL);
	}

	@Override
	public boolean canSuperJump() {
		return true;
	}

	@Override
	public @Nullable BleedProfile getMeleeBleed(ServerPlayer attacker) {
		return getTier(attacker) >= 3 ? new BleedProfile(0.50f, 1) : null;
	}

}
