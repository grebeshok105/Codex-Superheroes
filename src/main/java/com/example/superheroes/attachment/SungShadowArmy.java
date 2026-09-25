package com.example.superheroes.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.List;
import java.util.UUID;

/**
 * Persistent per-player record of Sung Jin-Woo's shadow army (audit B18).
 *
 * Shadow entities already serialize their own slot/variant/grounded/owner data;
 * this attachment only needs the entity UUIDs so a server restart re-links the
 * surviving army instead of summoning a duplicate one next to it. {@code summoned}
 * records that the initial ten-shadow summon already happened (shadows are
 * one-time — they never respawn for free); {@code phase2} is the Monarch's
 * Domain escalation flag.
 *
 * Not copyOnDeath: death untransforms the hero, so the army starts empty.
 */
public record SungShadowArmy(List<UUID> shadowIds, boolean summoned, boolean phase2) {
	public static final SungShadowArmy EMPTY = new SungShadowArmy(List.of(), false, false);

	public static final Codec<SungShadowArmy> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			UUIDUtil.CODEC.listOf().fieldOf("shadow_ids").forGetter(SungShadowArmy::shadowIds),
			Codec.BOOL.fieldOf("summoned").forGetter(SungShadowArmy::summoned),
			Codec.BOOL.fieldOf("phase2").forGetter(SungShadowArmy::phase2)
	).apply(instance, SungShadowArmy::new));

	public SungShadowArmy withShadows(List<UUID> ids) {
		return new SungShadowArmy(List.copyOf(ids), summoned, phase2);
	}

	public SungShadowArmy withSummoned(boolean value) {
		return new SungShadowArmy(shadowIds, value, phase2);
	}

	public SungShadowArmy withPhase2(boolean value) {
		return new SungShadowArmy(shadowIds, summoned, value);
	}
}
