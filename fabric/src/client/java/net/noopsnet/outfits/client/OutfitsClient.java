// SPDX-License-Identifier: MPL-2.0
package net.noopsnet.outfits.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.noopsnet.outfits.Outfit;
import net.noopsnet.outfits.OutfitPayloads;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class OutfitsClient implements ClientModInitializer {
    private static final Map<UUID, Outfit> OUTFITS = new HashMap<>();

    public static Outfit get(UUID player) {
        return OUTFITS.getOrDefault(player, Outfit.EMPTY);
    }

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(
                OutfitPayloads.Sync.ID,
                (payload, context) -> {
                    OUTFITS.put(payload.player(), payload.outfit());
                    if (context.client().player != null
                            && context.client().player.getUuid().equals(payload.player())
                            && context.client().currentScreen instanceof OutfitScreen screen) {
                        screen.update(payload.outfit());
                    }
                });
        ClientPlayNetworking.registerGlobalReceiver(
                OutfitPayloads.Open.ID,
                (payload, context) ->
                        context.client().setScreen(new OutfitScreen(payload.outfit())));
        ClientPlayConnectionEvents.JOIN.register(
                (handler, sender, client) -> {
                    // Request after play-channel registration, including on integrated servers.
                    if (ClientPlayNetworking.canSend(OutfitPayloads.Request.ID)) {
                        ClientPlayNetworking.send(new OutfitPayloads.Request());
                    }
                });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> OUTFITS.clear());
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
                (type, renderer, helper, context) -> {
                    if (renderer instanceof PlayerEntityRenderer playerRenderer) {
                        helper.register(new OutfitFeatureRenderer(playerRenderer));
                    }
                });
    }
}
