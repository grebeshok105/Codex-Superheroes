package io.github.grebeshok105.codex.content.command;

import io.github.grebeshok105.codex.core.module.ContentModule;
import io.github.grebeshok105.codex.core.module.ContentModuleContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/**
 * The shared {@code /superheroes} command root (hero/untransform/energy/mana/admin/debug/
 * horde/abilities/info). Hero-owned subcommands attach their own {@code superheroes}
 * literal via the same callback — Brigadier merges same-named root children, whichever
 * order the callbacks fire in (see {@code BattleBeastCommands}).
 */
public final class CommandModule implements ContentModule {
	@Override
	public void register(ContentModuleContext ctx) {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) ->
				SuperheroesCommands.register(dispatcher));
	}
}
