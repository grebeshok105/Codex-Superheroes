package com.example.superheroes.item.infinity;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

import java.util.ArrayList;
import java.util.List;

public final class InfinityGauntletData {
	private static final String KEY_STONES = "InsertedStones";

	private InfinityGauntletData() {
	}

	public static List<InfinityStoneType> getStones(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null) return List.of();
		CompoundTag tag = data.copyTag();
		if (!tag.contains(KEY_STONES, Tag.TAG_LIST)) return List.of();
		ListTag list = tag.getList(KEY_STONES, Tag.TAG_STRING);
		List<InfinityStoneType> out = new ArrayList<>(list.size());
		for (int i = 0; i < list.size(); i++) {
			InfinityStoneType t = InfinityStoneType.byId(list.getString(i));
			if (t != null && !out.contains(t)) {
				out.add(t);
			}
		}
		return out;
	}

	public static int getStoneCount(ItemStack stack) {
		return getStones(stack).size();
	}

	public static boolean hasStone(ItemStack stack, InfinityStoneType type) {
		return getStones(stack).contains(type);
	}

	public static boolean isFull(ItemStack stack) {
		return getStoneCount(stack) >= InfinityStoneType.values().length;
	}

	public static boolean tryInsert(ItemStack stack, InfinityStoneType type) {
		List<InfinityStoneType> current = new ArrayList<>(getStones(stack));
		if (current.contains(type)) return false;
		if (current.size() >= InfinityStoneType.values().length) return false;
		current.add(type);
		write(stack, current);
		return true;
	}

	public static InfinityStoneType ejectLast(ItemStack stack) {
		List<InfinityStoneType> current = new ArrayList<>(getStones(stack));
		if (current.isEmpty()) return null;
		InfinityStoneType removed = current.remove(current.size() - 1);
		write(stack, current);
		return removed;
	}

	private static void write(ItemStack stack, List<InfinityStoneType> stones) {
		CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
		CompoundTag tag = existing == null ? new CompoundTag() : existing.copyTag();
		ListTag list = new ListTag();
		for (InfinityStoneType t : stones) {
			list.add(StringTag.valueOf(t.getId()));
		}
		tag.put(KEY_STONES, list);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(stones.size()));
	}
}
