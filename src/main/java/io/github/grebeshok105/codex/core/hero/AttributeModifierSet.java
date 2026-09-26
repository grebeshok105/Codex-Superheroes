package io.github.grebeshok105.codex.core.hero;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.ArrayList;
import java.util.List;

public final class AttributeModifierSet {
	private final List<Entry> entries;
	private final boolean transientModifiers;

	private AttributeModifierSet(List<Entry> entries, boolean transientModifiers) {
		this.entries = List.copyOf(entries);
		this.transientModifiers = transientModifiers;
	}

	public static Builder builder() {
		return new Builder();
	}

	public void apply(LivingEntity entity) {
		for (Entry e : entries) {
			AttributeInstance instance = entity.getAttribute(e.attribute);
			if (instance == null) {
				continue;
			}
			AttributeModifier modifier = new AttributeModifier(e.id, e.amount, e.operation);
			if (transientModifiers) {
				instance.addOrUpdateTransientModifier(modifier);
			} else {
				instance.addOrReplacePermanentModifier(modifier);
			}
		}
	}

	public void remove(LivingEntity entity) {
		for (Entry e : entries) {
			AttributeInstance instance = entity.getAttribute(e.attribute);
			if (instance != null) {
				instance.removeModifier(e.id);
			}
		}
	}

	public record Entry(Holder<Attribute> attribute, ResourceLocation id, double amount,
			AttributeModifier.Operation operation) {
	}

	public static final class Builder {
		private final List<Entry> entries = new ArrayList<>();
		private boolean transientModifiers;

		private Builder() {
		}

		public Builder add(Holder<Attribute> attribute, ResourceLocation id, double amount,
				AttributeModifier.Operation operation) {
			entries.add(new Entry(attribute, id, amount, operation));
			return this;
		}

		/**
		 * Marks the set as ability-scoped rather than hero-passive-scoped: the modifiers are
		 * applied as transient and never persisted to NBT, so they cannot leak across relog
		 * or crash when the ability's own state does not survive (audit B3). Hero passives
		 * stay permanent — a transient max-health modifier would clamp health on load.
		 */
		public Builder abilityScoped() {
			this.transientModifiers = true;
			return this;
		}

		public AttributeModifierSet build() {
			return new AttributeModifierSet(entries, transientModifiers);
		}
	}
}
