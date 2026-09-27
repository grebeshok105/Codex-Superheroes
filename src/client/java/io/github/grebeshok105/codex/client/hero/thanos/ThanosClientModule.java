package io.github.grebeshok105.codex.client.hero.thanos;

import io.github.grebeshok105.codex.client.core.module.HeroClientContext;
import io.github.grebeshok105.codex.client.core.module.HeroClientModule;
import io.github.grebeshok105.codex.client.hero.thanos.hud.ThanosStoneBadge;
import io.github.grebeshok105.codex.client.hero.thanos.state.ClientThanosState;
import io.github.grebeshok105.codex.client.render.CosmicBeamRenderer;
import io.github.grebeshok105.codex.core.transform.TransformationItem;
import io.github.grebeshok105.codex.hero.thanos.ThanosAbilities;
import io.github.grebeshok105.codex.hero.thanos.ThanosHero;
import io.github.grebeshok105.codex.hero.thanos.item.InfinityStoneType;
import io.github.grebeshok105.codex.hero.thanos.item.InfinityStones;
import io.github.grebeshok105.codex.network.ThanosCosmicBeamS2CPayload;
import io.github.grebeshok105.codex.hero.thanos.net.ThanosStonesS2CPayload;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public record ThanosClientModule() implements HeroClientModule {
	@Override
	public ResourceLocation heroId() {
		return ThanosHero.ID;
	}

	@Override
	public void register(HeroClientContext ctx) {
		ctx.skin(new ThanosSkinProvider());
		ctx.receive(ThanosCosmicBeamS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> CosmicBeamRenderer.add(payload.start(), payload.end())));
		ctx.receive(ThanosStonesS2CPayload.TYPE, (payload, context) ->
				context.client().execute(() -> ClientThanosState.update(payload.playerId(), payload.bitmask())));

		ThanosStoneBadge badge = new ThanosStoneBadge();
		for (ResourceLocation id : ThanosAbilities.ALL) {
			ctx.abilityDecoration(id, badge);
		}

		// The "contains an Infinity Stone" tail used to be emitted inside each item's
		// appendHoverText via TooltipFrame.containsStone — that would make core tooltip
		// code depend on hero content, so the lines are appended here instead.
		ItemTooltipCallback.EVENT.register(ThanosClientModule::appendStoneLine);
	}

	private static void appendStoneLine(ItemStack stack, Item.TooltipContext context,
			TooltipFlag flag, List<Component> lines) {
		if (!(stack.getItem() instanceof TransformationItem item)) {
			return;
		}
		InfinityStoneType stone = InfinityStones.rewardFor(item.getHeroId());
		if (stone == null || lines.isEmpty()) {
			return;
		}
		// Insert before the last divider, not lines.getLast(): with F3+H advanced
		// tooltips vanilla appends the item-id line after hover lines, and other
		// ItemTooltipCallback handlers may append too — getLast() isn't the divider.
		int divider = -1;
		for (int i = lines.size() - 1; i >= 0; i--) {
			if (lines.get(i).getContents() instanceof PlainTextContents text
					&& !text.text().isEmpty()
					&& text.text().chars().allMatch(c -> c == '━')) {
				divider = i;
				break;
			}
		}
		if (divider < 0) {
			return;
		}
		Component stoneName = Component.translatable(stone.getStoneNameKey())
				.withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
		lines.add(divider, Component.empty());
		lines.add(divider + 1, Component.literal("◆ ").withStyle(ChatFormatting.LIGHT_PURPLE)
				.append(Component.translatable("tooltip.superheroes.contains_stone").withStyle(ChatFormatting.GRAY))
				.append(Component.literal(" "))
				.append(stoneName));
	}
}
