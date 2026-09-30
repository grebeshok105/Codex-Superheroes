package io.github.grebeshok105.codex.client.hero.homelander.emf;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import traben.entity_model_features.EMF;
import traben.entity_model_features.models.animation.EMFAnimationEntityContext;
import traben.entity_model_features.models.animation.EMFAnimationHandler;
import traben.entity_model_features.models.animation.math.asm.ASMParser;
import traben.entity_model_features.models.animation.math.asm.ASMVariableHandler;
import traben.entity_model_features.models.animation.math.expression_tree.MathComponent;
import traben.entity_model_features.models.animation.math.expression_tree.MathExpressionParser;
import traben.entity_model_features.models.animation.math.expression_tree.OldEMFAnimationHandler;
import traben.entity_model_features.models.animation.math.variables.EMFModelOrRenderVariable;
import traben.entity_model_features.models.animation.state.EMFEntityRenderStateViaReference;
import traben.entity_model_features.models.parts.EMFModelPart;
import traben.entity_model_features.EMFManager;
import traben.entity_model_features.models.animation.AnimSetupContext;
import traben.entity_model_features.utils.EMFEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The compiled EMF runtime for the Homelander model: the model's part tree,
 * the generated expression lines and the {@link EMFAnimationHandler} EMF built
 * from them (ASM-compiled when the method fits, the interpreted fallback
 * otherwise — EMF's own two-level machinery, mirroring
 * {@code EMFManager.setupAnimationsFromJemToModel}).
 *
 * <p>Per entity state lives in EMF's entity variable map
 * ({@code var.<clip>_w/_t} + lean vars, written by
 * {@link HomelanderEmfRuntime}); {@link #render} installs the entity context,
 * evaluates every line once, and draws the model — the exact flow EMF uses for
 * vanilla entity models.
 */
public final class HomelanderEmfEngine {
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final String MODEL_NAME = "homelander";

	private final HomelanderEmfModel model;
	private final EMFAnimationHandler handler;
	private final ResourceLocation renderTexture;

	private HomelanderEmfEngine(HomelanderEmfModel model, EMFAnimationHandler handler) {
		this.model = model;
		this.handler = handler;
		this.renderTexture = model.mainTexture();
	}

	public static @Nullable HomelanderEmfEngine compile(EmfModelData modelData,
			List<EmfExpressionBuilder.Line> lines) {
		try {
			HomelanderEmfModel model = HomelanderEmfModel.build(modelData);
			OldEMFAnimationHandler oldHandler = new OldEMFAnimationHandler(MODEL_NAME);
			Map<String, EMFModelPart> parts = new HashMap<>(model.parts());
			for (EmfExpressionBuilder.Line line : lines) {
				int dot = line.animKey().lastIndexOf('.');
				EMFModelPart part = model.part(line.animKey().substring(0, dot));
				EMFModelOrRenderVariable applier =
						EMFModelOrRenderVariable.get(line.animKey().substring(dot + 1));
				if (part == null || applier == null) {
					LOGGER.warn("[emf] cannot resolve line {}", line.animKey());
					continue;
				}
				oldHandler.addAnimLineData(new EMFAnimationHandler.AnimLineData(
						line.animKey(), line.expression(), part, applier));
			}
			AnimSetupContext context = new AnimSetupContext(MODEL_NAME, oldHandler,
					new HashMap<>(parts));
			EMFManager.getInstance().isAnimationValidationPhase = true;
			try {
				for (EMFAnimationHandler.AnimLineData line : oldHandler.lines()) {
					context.animKey = line.animKey;
					MathComponent component = MathExpressionParser.getOptimizedExpression(
							line.expression, false, context);
					context.animKey = null;
					oldHandler.addParsedLine(line, component);
				}
			} finally {
				EMFManager.getInstance().isAnimationValidationPhase = false;
			}
			EMFAnimationHandler handler = oldHandler;
			if (EMF.config().getConfig().asmMaths) {
				ASMVariableHandler variableHandler = new ASMVariableHandler();
				ASMParser.ASMExecutor executor = ASMParser.compileOrNull(oldHandler, variableHandler);
				if (executor != null) {
					handler = new traben.entity_model_features.models.animation.math.asm.ASMAnimationHandler(
							executor, variableHandler, context);
				}
			}
			handler.finishAndValidate();
			return new HomelanderEmfEngine(model, handler);
		} catch (Throwable t) {
			LOGGER.error("[emf] failed to compile Homelander EMF engine", t);
			return null;
		}
	}

	public HomelanderEmfModel model() {
		return model;
	}

	public @Nullable ResourceLocation renderTexture() {
		return renderTexture;
	}

	/**
	 * Evaluates every animation line for {@code entity} and renders the model.
	 * Must be called on the render thread inside the player's render pass with
	 * the pose stack already at the model-space origin.
	 */
	public void render(com.mojang.blaze3d.vertex.PoseStack poseStack,
			net.minecraft.client.renderer.MultiBufferSource bufferSource,
			int packedLight, Entity entity, float partialTicks) {
		traben.entity_model_features.models.animation.state.EMFEntityRenderState state =
				new EMFEntityRenderStateViaReference((EMFEntity) entity);
		state.setLayerFactory(RenderType::entityCutout);
		EMFAnimationEntityContext.setCurrentEntityNoIteration(state);
		try {
			handler.animate(EMFAnimationEntityContext.getEntityPartsAnimPaused());
		} catch (Throwable t) {
			if (Minecraft.getInstance().level != null
					&& Minecraft.getInstance().level.getGameTime() % 200 == 0) {
				LOGGER.error("[emf] animation evaluation failed for {}", entity.getName().getString(), t);
			}
		}
		if (renderTexture == null) {
			EMFAnimationEntityContext.reset();
			return;
		}
		EMFAnimationEntityContext.setLayerFactory(RenderType::entityTranslucent);
		var consumer = bufferSource.getBuffer(RenderType.entityTranslucent(renderTexture));
		model.root().render(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
		EMFAnimationEntityContext.reset();
	}
}
