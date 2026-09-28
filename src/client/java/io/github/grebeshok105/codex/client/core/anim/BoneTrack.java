package io.github.grebeshok105.codex.client.core.anim;

/** Rotation (degrees) and position (model pixels) channels of one animated bone. */
public record BoneTrack(Keyframes rotation, Keyframes position) {
}
