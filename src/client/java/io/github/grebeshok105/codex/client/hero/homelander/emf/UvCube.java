package io.github.grebeshok105.codex.client.hero.homelander.emf;

import io.github.grebeshok105.codex.client.mixin.ModelPartCubeAccessor;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A {@link ModelPart.Cube} whose six faces get explicit UV rectangles instead
 * of the vanilla box-uv region — the model.json format carries per-face
 * {@code [u1,v1,u2,v2]} rects (Blockbench per-face uv). Vertex layout mirrors
 * {@code ModelPart.Cube}'s constructor exactly so winding and UV assignment
 * match vanilla rendering.
 */
final class UvCube extends ModelPart.Cube {
	/**
	 * @param faces vanilla-space face name → {@link EmfModelData.Face} rect
	 *              (the caller already applied {@link EmfModelConversion#faceName}
	 *              and dropped faces the cube does not declare)
	 */
	UvCube(float minX, float minY, float minZ,
			float sizeX, float sizeY, float sizeZ,
			Map<String, EmfModelData.Face> faces,
			float textureWidth, float textureHeight) {
		super(0, 0, minX, minY, minZ, sizeX, sizeY, sizeZ, 0f, 0f, 0f,
				false, textureWidth, textureHeight, java.util.Set.of());
		// Replace the box-uv polygons with the declared per-face rects.
		float maxX = minX + sizeX;
		float maxY = minY + sizeY;
		float maxZ = minZ + sizeZ;
		ModelPart.Vertex v19 = vertex(minX, minY, minZ);
		ModelPart.Vertex v20 = vertex(maxX, minY, minZ);
		ModelPart.Vertex v21 = vertex(maxX, maxY, minZ);
		ModelPart.Vertex v22 = vertex(minX, maxY, minZ);
		ModelPart.Vertex v23 = vertex(minX, minY, maxZ);
		ModelPart.Vertex v24 = vertex(maxX, minY, maxZ);
		ModelPart.Vertex v25 = vertex(maxX, maxY, maxZ);
		ModelPart.Vertex v26 = vertex(minX, maxY, maxZ);
		// Vanilla face vertex groups + direction (javap'd ModelPart.Cube order).
		record FaceDef(String key, ModelPart.Vertex[] verts, Direction direction) {
		}
		List<FaceDef> defs = List.of(
				new FaceDef("down", new ModelPart.Vertex[]{v24, v23, v19, v20}, Direction.DOWN),
				new FaceDef("up", new ModelPart.Vertex[]{v21, v22, v26, v25}, Direction.UP),
				new FaceDef("west", new ModelPart.Vertex[]{v19, v23, v26, v22}, Direction.WEST),
				new FaceDef("north", new ModelPart.Vertex[]{v20, v19, v22, v21}, Direction.NORTH),
				new FaceDef("east", new ModelPart.Vertex[]{v23, v24, v25, v26}, Direction.EAST),
				new FaceDef("south", new ModelPart.Vertex[]{v24, v20, v21, v25}, Direction.SOUTH));
		List<ModelPart.Polygon> polygons = new ArrayList<>(defs.size());
		for (FaceDef def : defs) {
			EmfModelData.Face face = faces.get(def.key());
			if (face == null) {
				continue;
			}
			polygons.add(new ModelPart.Polygon(def.verts(),
					face.u1(), face.v1(), face.u2(), face.v2(),
					textureWidth, textureHeight, false, def.direction()));
		}
		((ModelPartCubeAccessor) (Object) this).superheroes$setPolygons(
				polygons.toArray(new ModelPart.Polygon[0]));
	}

	private static ModelPart.Vertex vertex(float x, float y, float z) {
		return new ModelPart.Vertex(x, y, z, 0f, 0f);
	}
}
