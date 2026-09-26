package io.github.grebeshok105.codex.mechanic.targeting;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** Area scans for living targets; the {@link TargetFilter} owns the rules. */
public final class Targeting {
	private Targeting() {
	}

	public static List<LivingEntity> living(ServerLevel level, AABB box, TargetFilter filter) {
		return level.getEntitiesOfClass(LivingEntity.class, box, filter::test);
	}
}
