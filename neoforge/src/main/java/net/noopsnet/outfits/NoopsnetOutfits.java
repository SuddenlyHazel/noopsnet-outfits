// SPDX-License-Identifier: MPL-2.0
package net.noopsnet.outfits;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Mod(NoopsnetOutfits.ID)
public final class NoopsnetOutfits {
    public static final String ID = "noopsnet_outfits";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    // Client implementations are assigned only by the Dist.CLIENT entrypoint.
    // Common class loading never refers to Minecraft's client classes.
    public static Consumer<OutfitPayloads.Sync> clientSync = payload -> {};
    public static Consumer<OutfitPayloads.Open> clientOpen = payload -> {};
    private final Map<UUID, Long> lastSelection = new HashMap<>();
    private final Map<UUID, Long> lastSnapshot = new HashMap<>();

    public static OutfitState state(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(OutfitState.TYPE, ID);
    }

    public NoopsnetOutfits(IEventBus modBus) {
        modBus.addListener(this::registerPayloads);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
        NeoForge.EVENT_BUS.addListener(this::loggedIn);
        NeoForge.EVENT_BUS.addListener(this::loggedOut);
        NeoForge.EVENT_BUS.addListener(this::stopped);
        LOGGER.info(
                "NoopsNet Outfits initialized for NeoForge: 8 pride designs, 2 cosmetic slots,"
                        + " /outfits");
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1").optional().executesOn(HandlerThread.MAIN);
        registrar.playToClient(
                OutfitPayloads.Sync.ID,
                OutfitPayloads.Sync.CODEC,
                (payload, context) -> clientSync.accept(payload));
        registrar.playToClient(
                OutfitPayloads.Open.ID,
                OutfitPayloads.Open.CODEC,
                (payload, context) -> clientOpen.accept(payload));
        registrar.playToServer(
                OutfitPayloads.Request.ID,
                OutfitPayloads.Request.CODEC,
                (payload, context) -> {
                    if (!(context.player() instanceof ServerPlayer viewer)
                            || !viewer.connection.hasChannel(OutfitPayloads.Sync.ID)) return;
                    MinecraftServer server = viewer.getServer();
                    long now = server.overworld().getGameTime();
                    Long previous = lastSnapshot.get(viewer.getUUID());
                    if (previous != null && now - previous < 20) return;
                    lastSnapshot.put(viewer.getUUID(), now);
                    for (ServerPlayer online : server.getPlayerList().getPlayers()) {
                        send(
                                viewer,
                                OutfitPayloads.Sync.of(
                                        online.getUUID(), state(server).get(online.getUUID())));
                    }
                });
        registrar.playToServer(
                OutfitPayloads.Select.ID,
                OutfitPayloads.Select.CODEC,
                (payload, context) -> {
                    // Receivers run on the main server thread. The sender owns the only
                    // UUID this operation can change; there is no client-supplied target.
                    if (!(context.player() instanceof ServerPlayer player)) return;
                    if (payload.slot() < 0
                            || payload.slot() > 1
                            || Design.fromWire(payload.design()).isEmpty()) return;
                    long now = player.getServer().overworld().getGameTime();
                    Long previous = lastSelection.get(player.getUUID());
                    if (previous != null && now - previous < 4) {
                        send(
                                player,
                                OutfitPayloads.Sync.of(
                                        player.getUUID(),
                                        state(player.getServer()).get(player.getUUID())));
                        return;
                    }
                    lastSelection.put(player.getUUID(), now);
                    equip(player, payload.slot(), Design.fromWire(payload.design()).orElseThrow());
                });
    }

    private void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = player.getServer();
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            send(
                    player,
                    OutfitPayloads.Sync.of(online.getUUID(), state(server).get(online.getUUID())));
        }
        broadcast(
                server,
                OutfitPayloads.Sync.of(player.getUUID(), state(server).get(player.getUUID())));
    }

    private void loggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        lastSelection.remove(event.getEntity().getUUID());
        lastSnapshot.remove(event.getEntity().getUUID());
    }

    private void stopped(ServerStoppedEvent event) {
        lastSelection.clear();
        lastSnapshot.clear();
    }

    private void registerCommands(RegisterCommandsEvent event) {
        var root = Commands.literal("outfits").executes(context -> open(context.getSource()));
        for (int slot = 0; slot < 2; slot++) {
            final int selectedSlot = slot;
            var design =
                    Commands.argument("design", StringArgumentType.word())
                            .suggests(
                                    (context, builder) -> {
                                        for (Design item : Design.values())
                                            builder.suggest(item.id());
                                        return builder.buildFuture();
                                    })
                            .executes(context -> selectDesign(context, selectedSlot));
            root.then(Commands.literal(slot == 0 ? "pin" : "patch").then(design));
        }
        root.then(
                Commands.literal("clear")
                        .executes(
                                context -> {
                                    ServerPlayer player =
                                            context.getSource().getPlayerOrException();
                                    state(player.getServer()).set(player.getUUID(), Outfit.EMPTY);
                                    broadcast(
                                            player.getServer(),
                                            OutfitPayloads.Sync.of(player.getUUID(), Outfit.EMPTY));
                                    context.getSource()
                                            .sendSuccess(
                                                    () ->
                                                            Component.translatable(
                                                                    "noopsnet_outfits.cleared"),
                                                    false);
                                    return 1;
                                }));
        event.getDispatcher().register(root);
    }

    private static int selectDesign(CommandContext<CommandSourceStack> context, int slot)
            throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        var design = Design.fromId(StringArgumentType.getString(context, "design"));
        if (design.isEmpty()) {
            context.getSource()
                    .sendFailure(Component.translatable("noopsnet_outfits.invalid_design"));
            return 0;
        }
        equip(player, slot, design.get());
        context.getSource()
                .sendSuccess(() -> Component.translatable("noopsnet_outfits.updated"), false);
        return 1;
    }

    private static int open(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!player.connection.hasChannel(OutfitPayloads.Open.ID)) {
            source.sendFailure(Component.translatable("noopsnet_outfits.install_client"));
            return 0;
        }
        Outfit outfit = state(player.getServer()).get(player.getUUID());
        PacketDistributor.sendToPlayer(
                player, new OutfitPayloads.Open(outfit.pin().ordinal(), outfit.patch().ordinal()));
        return 1;
    }

    private static void equip(ServerPlayer player, int slot, Design design) {
        OutfitState state = state(player.getServer());
        Outfit outfit = state.get(player.getUUID()).with(slot, design);
        state.set(player.getUUID(), outfit);
        broadcast(player.getServer(), OutfitPayloads.Sync.of(player.getUUID(), outfit));
    }

    private static void broadcast(MinecraftServer server, OutfitPayloads.Sync payload) {
        for (ServerPlayer viewer : server.getPlayerList().getPlayers()) send(viewer, payload);
    }

    private static void send(ServerPlayer player, OutfitPayloads.Sync payload) {
        if (player.connection.hasChannel(OutfitPayloads.Sync.ID))
            PacketDistributor.sendToPlayer(player, payload);
    }
}
