package io.github.grebeshok105.codex.core.module;

import io.github.grebeshok105.codex.core.ability.AbilityRegistry;
import io.github.grebeshok105.codex.core.attachment.AttachmentRegistrar;
import io.github.grebeshok105.codex.core.content.ContentRegistrar;
import io.github.grebeshok105.codex.core.content.CreativeTabContents;
import io.github.grebeshok105.codex.core.lifecycle.HeroTickDispatcher;
import io.github.grebeshok105.codex.core.lifecycle.LifecycleRegistrar;
import io.github.grebeshok105.codex.core.lifecycle.TickRegistrar;
import io.github.grebeshok105.codex.core.net.PayloadRegistrar;

public final class CoreModuleContext implements HeroModuleContext {
	public static final CoreModuleContext INSTANCE = new CoreModuleContext();

	private CoreModuleContext() {
	}

	@Override
	public AbilitySink abilities() {
		return AbilityRegistry::register;
	}

	@Override
	public TickRegistrar ticks() {
		return HeroTickDispatcher.registrar();
	}

	@Override
	public LifecycleRegistrar lifecycle() {
		return LifecycleRegistrar.global();
	}

	@Override
	public ContentRegistrar content() {
		return CreativeTabContents::add;
	}

	@Override
	public PayloadRegistrar payloads() {
		return PayloadRegistrar.FABRIC;
	}

	@Override
	public AttachmentRegistrar attachments() {
		return AttachmentRegistrar.FABRIC;
	}
}
