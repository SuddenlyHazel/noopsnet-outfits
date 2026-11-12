// SPDX-License-Identifier: MPL-2.0
package net.noopsnet.outfits.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.noopsnet.outfits.NoopsnetOutfits;
import net.noopsnet.outfits.Outfit;
import net.noopsnet.outfits.OutfitPayloads;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Only this entrypoint installs client handlers and player rendering layers. */
@Mod(value = NoopsnetOutfits.ID, dist = Dist.CLIENT)
public final class OutfitsClient {
    private static final Map<UUID, Outfit> OUTFITS = new HashMap<>();

    public static Outfit get(UUID player) {
        return OUTFITS.getOrDefault(player, Outfit.EMPTY);
    }

    public OutfitsClient(IEventBus modBus) {
        NoopsnetOutfits.clientSync = OutfitsClient::sync;
        NoopsnetOutfits.clientOpen =
                payload -> Minecraft.getInstance().setScreen(new OutfitScreen(payload.outfit()));
        modBus.addListener(OutfitsClient::addLayers);
        NeoForge.EVENT_BUS.addListener(OutfitsClient::loggedIn);
        NeoForge.EVENT_BUS.addListener(OutfitsClient::loggedOut);
    }

    private static void sync(OutfitPayloads.Sync payload) {
        Minecraft client = Minecraft.getInstance();
        if (payload.outfit().equals(Outfit.EMPTY)) OUTFITS.remove(payload.player());
        else OUTFITS.put(payload.player(), payload.outfit());
        if (client.player != null
                && client.player.getUUID().equals(payload.player())
                && client.screen instanceof OutfitScreen screen) screen.update(payload.outfit());
    }

    private static void loggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null && connection.hasChannel(OutfitPayloads.Request.ID)) {
            PacketDistributor.sendToServer(new OutfitPayloads.Request());
        }
    }

    private static void loggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        OUTFITS.clear();
    }

    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) renderer.addLayer(new OutfitFeatureRenderer(renderer));
        }
    }
}
