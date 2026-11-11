// SPDX-License-Identifier: MPL-2.0
package net.noopsnet.outfits;

import java.util.UUID;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public final class OutfitPayloads {
    private OutfitPayloads() {}

    public record Request() implements CustomPayload {
        public static final Id<Request> ID = new Id<>(Identifier.of(NoopsnetOutfits.ID, "request_v1"));
        public static final PacketCodec<RegistryByteBuf, Request> CODEC = PacketCodec.of(
                (value, buf) -> {}, buf -> new Request());
        @Override public Id<Request> getId() { return ID; }
    }

    public record Select(int slot, int design) implements CustomPayload {
        public static final Id<Select> ID = new Id<>(Identifier.of(NoopsnetOutfits.ID, "select_v1"));
        public static final PacketCodec<RegistryByteBuf, Select> CODEC = PacketCodec.of(
                (value, buf) -> { buf.writeVarInt(value.slot); buf.writeVarInt(value.design); },
                buf -> new Select(buf.readVarInt(), buf.readVarInt()));
        @Override public Id<Select> getId() { return ID; }
    }

    public record Sync(UUID player, int pin, int patch) implements CustomPayload {
        public static final Id<Sync> ID = new Id<>(Identifier.of(NoopsnetOutfits.ID, "sync_v1"));
        public static final PacketCodec<RegistryByteBuf, Sync> CODEC = PacketCodec.of(
                (value, buf) -> { buf.writeUuid(value.player); buf.writeVarInt(value.pin); buf.writeVarInt(value.patch); },
                buf -> new Sync(buf.readUuid(), buf.readVarInt(), buf.readVarInt()));
        @Override public Id<Sync> getId() { return ID; }
        public Outfit outfit() {
            return new Outfit(Design.fromWire(pin).orElse(Design.NONE), Design.fromWire(patch).orElse(Design.NONE));
        }
        public static Sync of(UUID player, Outfit outfit) {
            return new Sync(player, outfit.pin().ordinal(), outfit.patch().ordinal());
        }
    }

    public record Open(int pin, int patch) implements CustomPayload {
        public static final Id<Open> ID = new Id<>(Identifier.of(NoopsnetOutfits.ID, "open_v1"));
        public static final PacketCodec<RegistryByteBuf, Open> CODEC = PacketCodec.of(
                (value, buf) -> { buf.writeVarInt(value.pin); buf.writeVarInt(value.patch); },
                buf -> new Open(buf.readVarInt(), buf.readVarInt()));
        @Override public Id<Open> getId() { return ID; }
        public Outfit outfit() {
            return new Outfit(Design.fromWire(pin).orElse(Design.NONE), Design.fromWire(patch).orElse(Design.NONE));
        }
    }
}
