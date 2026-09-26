package io.github.grebeshok105.codex.core.attachment;

import io.github.grebeshok105.codex.ModId;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import java.util.function.Supplier;

/**
 * Narrow attachment-registration seam handed to hero modules through
 * {@link io.github.grebeshok105.codex.core.module.HeroModuleContext#attachments()}.
 * Bodies are plain {@link AttachmentRegistry#create} calls, like {@code ModAttachments}.
 */
public interface AttachmentRegistrar {
	AttachmentRegistrar FABRIC = new AttachmentRegistrar() {
		@Override
		public <A> AttachmentType<A> persistent(String path, Codec<A> codec, boolean copyOnDeath) {
			return AttachmentRegistry.create(ModId.of(path), b -> {
				b.persistent(codec);
				if (copyOnDeath) b.copyOnDeath();
			});
		}

		@Override
		public <A> AttachmentType<A> persistent(String path, Codec<A> codec, boolean copyOnDeath, Supplier<A> initializer) {
			return AttachmentRegistry.create(ModId.of(path), b -> {
				b.initializer(initializer);
				b.persistent(codec);
				if (copyOnDeath) b.copyOnDeath();
			});
		}

		@Override
		public <A> AttachmentType<A> transientType(String path) {
			return AttachmentRegistry.create(ModId.of(path), b -> {
			});
		}
	};

	<A> AttachmentType<A> persistent(String path, Codec<A> codec, boolean copyOnDeath);

	/**
	 * Same as {@link #persistent(String, Codec, boolean)} plus the default initializer, for
	 * attachments that callers populate via {@code getAttachedOrCreate}.
	 */
	<A> AttachmentType<A> persistent(String path, Codec<A> codec, boolean copyOnDeath, Supplier<A> initializer);

	<A> AttachmentType<A> transientType(String path);
}
