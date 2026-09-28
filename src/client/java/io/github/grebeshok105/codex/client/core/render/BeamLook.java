package io.github.grebeshok105.codex.client.core.render;

/**
 * Visual parameters of one cross-beam draw: core/glow widths in blocks,
 * packed ARGB colors, and {@code noise} — a 0..1 wobble factor the pattern
 * layer applies before delegating to the renderer. Lives next to
 * {@link CrossBeamRenderer} (like {@link BeamStyle}) so the renderer stays
 * free of pattern-package imports and no package cycle forms.
 */
public record BeamLook(float coreWidth, float glowWidth, int coreArgb, int glowArgb, float noise) {
}
