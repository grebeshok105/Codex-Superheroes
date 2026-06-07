package com.example.superheroes.hero;

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

	private AttributeModifierSet(List<Entry> entries) {
		this.entries = List.copyOf(entries);
	}

	public static Builder builder() {
		return new Builder();
	}

	public void apply(LivingEntity entity) {
		for (Entry e : entries) {
			AttributeInstance instance = entity.getAttribute(e.attribute);
			if (instance != null) {
				instance.addOrReplacePermanentModifier(new AttributeModifier(e.id, e.amount, e.operation));
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

		private Builder() {
		}

		public Builder add(Holder<Attribute> attribute, ResourceLocation id, double amount,
				AttributeModifier.Operation operation) {
			entries.add(new Entry(attribute, id, amount, operation));
			return this;
		}

		public AttributeModifierSet build() {
			return new AttributeModifierSet(entries);
		}
	}
}
