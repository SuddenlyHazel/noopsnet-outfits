// SPDX-License-Identifier: MPL-2.0
package net.noopsnet.outfits.client;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.model.PlayerModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.resources.ResourceLocation;
import net.noopsnet.outfits.Design;
import net.noopsnet.outfits.NoopsnetOutfits;
import net.noopsnet.outfits.Outfit;

/** Small torso-attached quads; no armor slots, mixins, entities, or render-time network work. */
public final class OutfitFeatureRenderer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public OutfitFeatureRenderer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> context) {
        super(context);
    }

    public static ResourceLocation texture(String slot, Design design) {
        return ResourceLocation.fromNamespaceAndPath(NoopsnetOutfits.ID, "textures/accessory/" + slot + "/" + design.id() + ".png");
    }

    @Override
    public void render(PoseStack matrices, MultiBufferSource consumers, int light,
                       AbstractClientPlayer player, float limbAngle, float limbDistance,
                       float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (player.isInvisible() || player.isSpectator() || !getParentModel().body.visible) return;
        Outfit outfit = OutfitsClient.get(player.getUUID());
        if (outfit.equals(Outfit.EMPTY)) return;
        matrices.pushPose();
        getParentModel().body.translateAndRotate(matrices);
        // Normal chest front is z=-2px. Armor extends it to about -3px.
        float z = player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ArmorItem ? -3.2f : -2.3f;
        if (outfit.pin() != Design.NONE) {
            draw(matrices, consumers, light, texture("pin", outfit.pin()), 0.6f, 2.3f, 2.6f, 2.6f, z);
        }
        if (outfit.patch() != Design.NONE) {
            draw(matrices, consumers, light, texture("patch", outfit.patch()), -3.5f, 5.6f, 3.8f, 2.85f, z);
        }
        matrices.popPose();
    }

    private static void draw(PoseStack matrices, MultiBufferSource consumers, int light,
                             ResourceLocation texture, float x, float y, float width, float height, float z) {
        VertexConsumer vertices = consumers.getBuffer(RenderType.entityCutoutNoCull(texture));
        PoseStack.Pose entry = matrices.last();
        vertex(vertices, entry, x / 16, y / 16, z / 16, 0, 0, light);
        vertex(vertices, entry, x / 16, (y + height) / 16, z / 16, 0, 1, light);
        vertex(vertices, entry, (x + width) / 16, (y + height) / 16, z / 16, 1, 1, light);
        vertex(vertices, entry, (x + width) / 16, y / 16, z / 16, 1, 0, light);
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose entry, float x, float y, float z,
                               float u, float v, int light) {
        vertices.addVertex(entry.pose(), x, y, z).setColor(0xFFFFFFFF).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(entry, 0, 0, -1);
    }
}
