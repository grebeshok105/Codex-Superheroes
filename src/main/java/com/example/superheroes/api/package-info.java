/**
 * Public API surface of the Superheroes mod for use by addon mods.
 *
 * <p>Anything in this package is considered a stable API surface. The mod will
 * make a best effort to keep these classes/methods backward-compatible across
 * minor versions and to call out breakages in major-version changelogs.
 *
 * <p>Anything outside this package — including {@code com.example.superheroes.hero},
 * {@code .ability}, {@code .transform}, {@code .resource}, {@code .effect},
 * {@code .item}, {@code .network}, {@code .attachment}, {@code .mixin} — is
 * <strong>internal</strong>. Addon mods may import the {@code Hero} and
 * {@code Ability} interfaces from those packages because they are required
 * to implement custom heroes/abilities, but they should NOT call into other
 * internal classes (controllers, attachments, networking, registries) directly.
 *
 * <h2>Typical addon usage</h2>
 * <pre>{@code
 * public final class MyAddonMod implements ModInitializer {
 *     @Override
 *     public void onInitialize() {
 *         HeroApi.register(new MyCustomHero());
 *         AbilityApi.register(new MyCustomAbility());
 *
 *         ItemGroupEvents.modifyEntriesEvent(CreativeTabIds.SUPERHEROES_TAB)
 *             .register(entries -> entries.accept(MY_ITEM));
 *     }
 * }
 * }</pre>
 *
 * <p>See {@code docs/api.md} in the Superheroes mod repo for the full guide.
 */
package com.example.superheroes.api;
