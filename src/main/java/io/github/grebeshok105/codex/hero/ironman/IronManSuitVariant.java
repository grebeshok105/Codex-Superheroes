package io.github.grebeshok105.codex.hero.ironman;

import com.mojang.serialization.Codec;
import io.github.grebeshok105.codex.ModId;
import io.github.grebeshok105.codex.core.attachment.AttachmentRegistrar;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record IronManSuitVariant(int index, String name, String nameRu, ResourceLocation texture,
		float damageMultiplier, float speedMultiplier, float energyRegenMultiplier,
		float armorBonus, boolean stealthParticles) {

	public static final List<IronManSuitVariant> ALL = List.of(
			new IronManSuitVariant(0, "Default", "Стандартный",
					ModId.of("textures/entity/hero/ironman.png"),
					1.0f, 1.0f, 1.0f, 0f, false),
			new IronManSuitVariant(1, "Mark 85", "Mark 85",
					ModId.of("textures/entity/hero/ironman_mark_85.png"),
					1.10f, 1.0f, 1.0f, 0f, false),
			new IronManSuitVariant(2, "Mark 1", "Mark 1",
					ModId.of("textures/entity/hero/ironman_mark_1.png"),
					1.0f, 0.90f, 1.0f, 4f, false),
			new IronManSuitVariant(3, "Stealth", "Стелс",
					ModId.of("textures/entity/hero/ironman_mark_stealth.png"),
					0.95f, 1.0f, 1.0f, 0f, true),
			new IronManSuitVariant(4, "Rescue", "Рескью",
					ModId.of("textures/entity/hero/ironman_mark_rescue.png"),
					0.90f, 1.0f, 1.20f, 0f, false),
			new IronManSuitVariant(5, "War Machine", "Военная Машина",
					ModId.of("textures/entity/hero/ironman_mark_war_machine.png"),
					1.20f, 0.85f, 1.0f, 2f, false)
	);

	public static int count() {
		return ALL.size();
	}

	public static IronManSuitVariant get(int index) {
		if (index < 0 || index >= ALL.size()) return ALL.get(0);
		return ALL.get(index);
	}

	/**
	 * Player attachment holding the equipped suit-variant index ({@code superheroes:suit_variant},
	 * 0 initializer + persistent + copyOnDeath — byte-identical to the old {@code ModAttachments} row).
	 * Created eagerly at class-init — see {@link IronManAttachments#init()}.
	 */
	public static final AttachmentType<Integer> ATTACHMENT =
			AttachmentRegistrar.FABRIC.persistent("suit_variant", Codec.INT, true, () -> 0);

	public static int nextIndex(int current) {
		return (current + 1) % ALL.size();
	}

	/**
	 * Returns suit variants available for Legion drones (all except the given one).
	 */
	public static List<IronManSuitVariant> legionVariants(int excludeIndex) {
		return ALL.stream().filter(v -> v.index() != excludeIndex).toList();
	}
}
