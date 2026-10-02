package com.crabmods.algocraft.client;

import com.crabmods.algocraft.AlgoCraft;
import com.crabmods.algocraft.TrophyBlock;
import com.crabmods.algocraft.TrophyBlockEntity;
import com.crabmods.algocraft.logic.ModTrophyBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = AlgoCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class TrophyBlockRenderer implements BlockEntityRenderer<TrophyBlockEntity> {
    private final ItemRenderer items;
    public TrophyBlockRenderer(BlockEntityRendererProvider.Context context) { items = context.getItemRenderer(); }
    @Override public void render(TrophyBlockEntity trophy, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5, 0.3, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - trophy.getBlockState().getValue(TrophyBlock.FACING).toYRot()));
        pose.scale(0.6F, 0.6F, 0.6F);
        items.renderStatic(trophy.getTrophy(), ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, pose, buffers, trophy.getLevel(), trophy.getBlockPos().hashCode());
        pose.popPose();
    }
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModTrophyBlocks.TROPHY_ENTITY.get(), TrophyBlockRenderer::new);
    }
}
