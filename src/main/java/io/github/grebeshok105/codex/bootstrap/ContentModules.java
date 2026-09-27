package io.github.grebeshok105.codex.bootstrap;

import io.github.grebeshok105.codex.core.module.ContentModule;
import io.github.grebeshok105.codex.core.module.ContentModuleContext;
import io.github.grebeshok105.codex.damage.DamageTypeSpec;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.damagesource.DamageType;

import java.util.ArrayList;
import java.util.List;

/** Composition root: the only place that names content modules. One line per content slice. */
public final class ContentModules {
	public static final List<ContentModule> ALL = List.of(
			new io.github.grebeshok105.codex.content.horde.HordeModule(),
			new io.github.grebeshok105.codex.content.boss.homelander.HomelanderBossModule()
	);

	private ContentModules() {
	}

	public static void bootstrap(ContentModuleContext ctx) {
		for (ContentModule module : ALL) {
			module.register(ctx);
		}
	}

	/** Bootstraps every module-owned damage type (datagen side; modules never run here). */
	public static void bootstrapDamageTypes(BootstrapContext<DamageType> context) {
		for (DamageTypeSpec spec : damageTypeSpecs()) {
			context.register(spec.key(), spec.type());
		}
	}

	/** Every module-owned damage type spec, in module order — datagen + tag providers enumerate this. */
	public static List<DamageTypeSpec> damageTypeSpecs() {
		List<DamageTypeSpec> specs = new ArrayList<>();
		for (ContentModule module : ALL) {
			specs.addAll(module.damageTypes());
		}
		return specs;
	}
}
