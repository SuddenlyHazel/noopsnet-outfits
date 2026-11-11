// SPDX-License-Identifier: MPL-2.0
package net.noopsnet.outfits.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.util.Identifier;
import net.noopsnet.outfits.Design;
import net.noopsnet.outfits.NoopsnetOutfits;
import net.noopsnet.outfits.Outfit;

/** Small torso-attached quads; no armor slots, mixins, entities, or render-time network work. */
public final class OutfitFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
    public OutfitFeatureRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> context) {
        super(context);
    }

    public static Identifier texture(String slot, Design design) {
        return Identifier.of(NoopsnetOutfits.ID, "textures/accessory/" + slot + "/" + design.id() + ".png");
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light,
                       AbstractClientPlayerEntity player, float limbAngle, float limbDistance,
                       float tickDelta, float animationProgress, float headYaw, float headPitch) {
        if (player.isInvisible() || player.isSpectator() || !getContextModel().body.visible) return;
        Outfit outfit = OutfitsClient.get(player.getUuid());
        if (outfit.equals(Outfit.EMPTY)) return;
        matrices.push();
        getContextModel().body.rotate(matrices);
        // Normal chest front is z=-2px. Armor extends it to about -3px.
        float z = player.getEquippedStack(EquipmentSlot.CHEST).getItem() instanceof ArmorItem ? -3.2f : -2.3f;
        if (outfit.pin() != Design.NONE) {
            draw(matrices, consumers, light, texture("pin", outfit.pin()), 0.6f, 2.3f, 2.6f, 2.6f, z);
        }
        if (outfit.patch() != Design.NONE) {
            draw(matrices, consumers, light, texture("patch", outfit.patch()), -3.5f, 5.6f, 3.8f, 2.85f, z);
        }
        matrices.pop();
    }

    private static void draw(MatrixStack matrices, VertexConsumerProvider consumers, int light,
                             Identifier texture, float x, float y, float width, float height, float z) {
        VertexConsumer vertices = consumers.getBuffer(RenderLayer.getEntityCutoutNoCull(texture));
        MatrixStack.Entry entry = matrices.peek();
        vertex(vertices, entry, x / 16, y / 16, z / 16, 0, 0, light);
        vertex(vertices, entry, x / 16, (y + height) / 16, z / 16, 0, 1, light);
        vertex(vertices, entry, (x + width) / 16, (y + height) / 16, z / 16, 1, 1, light);
        vertex(vertices, entry, (x + width) / 16, y / 16, z / 16, 1, 0, light);
    }

    private static void vertex(VertexConsumer vertices, MatrixStack.Entry entry, float x, float y, float z,
                               float u, float v, int light) {
        vertices.vertex(entry.getPositionMatrix(), x, y, z).color(0xFFFFFFFF).texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV).light(light).normal(entry, 0, 0, -1);
    }
}
