package io.github.grebeshok105.codex.client.mixin;

import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Lets {@code UvCube} install hand-built {@link ModelPart.Polygon} faces —
 * {@code Cube.polygons} is private-final because vanilla cubes always derive
 * them from a box-uv region. Same mechanism EMF uses for its own EMFCube.
 */
@Mixin(ModelPart.Cube.class)
public interface ModelPartCubeAccessor {
	@Accessor("polygons")
	@Mutable
	void superheroes$setPolygons(ModelPart.Polygon[] polygons);
}
