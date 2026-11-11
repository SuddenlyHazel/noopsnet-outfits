// SPDX-License-Identifier: MPL-2.0
package net.noopsnet.outfits;

import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NoopsnetOutfits implements ModInitializer {
    public static final String ID = "noopsnet_outfits";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    private final Map<UUID, Long> lastSelection = new HashMap<>();
    private final Map<UUID, Long> lastSnapshot = new HashMap<>();

    public static OutfitState state(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(OutfitState.TYPE, ID);
    }

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(OutfitPayloads.Select.ID, OutfitPayloads.Select.CODEC);
        PayloadTypeRegistry.playC2S().register(OutfitPayloads.Request.ID, OutfitPayloads.Request.CODEC);
        PayloadTypeRegistry.playS2C().register(OutfitPayloads.Sync.ID, OutfitPayloads.Sync.CODEC);
        PayloadTypeRegistry.playS2C().register(OutfitPayloads.Open.ID, OutfitPayloads.Open.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(OutfitPayloads.Request.ID, (payload, context) -> {
            ServerPlayerEntity viewer = context.player();
            if (!ServerPlayNetworking.canSend(viewer, OutfitPayloads.Sync.ID)) return;
            long now = context.server().getOverworld().getTime();
            Long previous = lastSnapshot.get(viewer.getUuid());
            if (previous != null && now - previous < 20) return;
            lastSnapshot.put(viewer.getUuid(), now);
            OutfitState state = state(context.server());
            for (ServerPlayerEntity online : context.server().getPlayerManager().getPlayerList()) {
                send(viewer, OutfitPayloads.Sync.of(online.getUuid(), state.get(online.getUuid())));
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(OutfitPayloads.Select.ID, (payload, context) -> {
            // Fabric dispatches play receivers on the server thread. Never accept a target UUID.
            ServerPlayerEntity player = context.player();
            if (payload.slot() < 0 || payload.slot() > 1 || Design.fromWire(payload.design()).isEmpty()) return;
            long now = player.getServerWorld().getTime();
            Long previous = lastSelection.get(player.getUuid());
            if (previous != null && now - previous < 4) {
                send(player, OutfitPayloads.Sync.of(player.getUuid(), state(context.server()).get(player.getUuid())));
                return;
            }
            lastSelection.put(player.getUuid(), now);
            equip(player, payload.slot(), Design.fromWire(payload.design()).orElseThrow());
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            OutfitState state = state(server);
            for (ServerPlayerEntity online : server.getPlayerManager().getPlayerList()) {
                send(handler.player, OutfitPayloads.Sync.of(online.getUuid(), state.get(online.getUuid())));
            }
            broadcast(server, OutfitPayloads.Sync.of(handler.player.getUuid(), state.get(handler.player.getUuid())));
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            lastSelection.remove(handler.player.getUuid());
            lastSnapshot.remove(handler.player.getUuid());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            lastSelection.clear();
            lastSnapshot.clear();
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> {
            var root = CommandManager.literal("outfits").executes(context -> open(context.getSource()));
            for (int slot = 0; slot < 2; slot++) {
                final int selectedSlot = slot;
                root.then(CommandManager.literal(slot == 0 ? "pin" : "patch")
                        .then(CommandManager.argument("design", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    for (Design design : Design.values()) builder.suggest(design.id());
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                                    var design = Design.fromId(StringArgumentType.getString(context, "design"));
                                    if (design.isEmpty()) {
                                        context.getSource().sendError(Text.translatable("noopsnet_outfits.invalid_design"));
                                        return 0;
                                    }
                                    equip(player, selectedSlot, design.get());
                                    context.getSource().sendFeedback(() -> Text.translatable("noopsnet_outfits.updated"), false);
                                    return 1;
                                })));
            }
            root.then(CommandManager.literal("clear").executes(context -> {
                ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                state(player.getServer()).set(player.getUuid(), Outfit.EMPTY);
                broadcast(player.getServer(), OutfitPayloads.Sync.of(player.getUuid(), Outfit.EMPTY));
                context.getSource().sendFeedback(() -> Text.translatable("noopsnet_outfits.cleared"), false);
                return 1;
            }));
            dispatcher.register(root);
        });
        LOGGER.info("NoopsNet Outfits initialized: 8 pride designs, 2 cosmetic slots, /outfits");
    }

    private static int open(ServerCommandSource source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        if (!ServerPlayNetworking.canSend(player, OutfitPayloads.Open.ID)) {
            source.sendError(Text.literal("Install NoopsNet Outfits on your client to open the wardrobe."));
            return 0;
        }
        Outfit outfit = state(player.getServer()).get(player.getUuid());
        ServerPlayNetworking.send(player, new OutfitPayloads.Open(outfit.pin().ordinal(), outfit.patch().ordinal()));
        return 1;
    }

    private static void equip(ServerPlayerEntity player, int slot, Design design) {
        OutfitState state = state(player.getServer());
        Outfit outfit = state.get(player.getUuid()).with(slot, design);
        state.set(player.getUuid(), outfit);
        broadcast(player.getServer(), OutfitPayloads.Sync.of(player.getUuid(), outfit));
    }

    private static void broadcast(MinecraftServer server, OutfitPayloads.Sync payload) {
        for (ServerPlayerEntity viewer : server.getPlayerManager().getPlayerList()) send(viewer, payload);
    }

    private static void send(ServerPlayerEntity player, OutfitPayloads.Sync payload) {
        if (ServerPlayNetworking.canSend(player, OutfitPayloads.Sync.ID)) ServerPlayNetworking.send(player, payload);
    }
}
