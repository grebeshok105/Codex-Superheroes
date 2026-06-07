package com.example.superheroes.item.infinity;

import com.example.superheroes.ModId;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.Nullable;

public enum InfinityStoneType {
	POWER("power", 0xFFB44CFF,
			Attributes.ATTACK_DAMAGE, 8.0, AttributeModifier.Operation.ADD_VALUE),
	SPACE("space", 0xFF4AA6FF,
			Attributes.MOVEMENT_SPEED, 0.30, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
	REALITY("reality", 0xFFE03030,
			Attributes.ARMOR, 12.0, AttributeModifier.Operation.ADD_VALUE),
	SOUL("soul", 0xFFFFA040,
			Attributes.MAX_HEALTH, 30.0, AttributeModifier.Operation.ADD_VALUE),
	TIME("time", 0xFF40D87A,
			Attributes.ATTACK_SPEED, 1.5, AttributeModifier.Operation.ADD_VALUE),
	MIND("mind", 0xFFFFE048,
			Attributes.KNOCKBACK_RESISTANCE, 0.5, AttributeModifier.Operation.ADD_VALUE);

	private final String id;
	private final int color;
	private final Holder<Attribute> attribute;
	private final double amount;
	private final AttributeModifier.Operation operation;
	private final ResourceLocation modifierId;

	InfinityStoneType(String id, int color,
			Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
		this.id = id;
		this.color = color;
		this.attribute = attribute;
		this.amount = amount;
		this.operation = operation;
		this.modifierId = ModId.of("modifiers/thanos/stone_" + id);
	}

	public String getId() {
		return id;
	}

	public String getItemRegistryName() {
		return id + "_stone";
	}

	public int getColor() {
		return color;
	}

	public Holder<Attribute> getAttribute() {
		return attribute;
	}

	public double getAmount() {
		return amount;
	}

	public AttributeModifier.Operation getOperation() {
		return operation;
	}

	public ResourceLocation getModifierId() {
		return modifierId;
	}

	public String getStoneNameKey() {
		return "item.superheroes." + id + "_stone";
	}

	@Nullable
	public static InfinityStoneType byId(String id) {
		if (id == null) return null;
		for (InfinityStoneType t : values()) {
			if (t.id.equals(id)) return t;
		}
		return null;
	}
}
