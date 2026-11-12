// SPDX-License-Identifier: MPL-2.0
package net.noopsnet.outfits;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public final class OutfitPayloads {
    private OutfitPayloads() {}

    public record Request() implements CustomPacketPayload {
        public static final Type<Request> ID =
                new Type<>(ResourceLocation.fromNamespaceAndPath(NoopsnetOutfits.ID, "request_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Request> CODEC =
                StreamCodec.ofMember((value, buf) -> {}, buf -> new Request());

        @Override
        public Type<Request> type() {
            return ID;
        }
    }

    public record Select(int slot, int design) implements CustomPacketPayload {
        public static final Type<Select> ID =
                new Type<>(ResourceLocation.fromNamespaceAndPath(NoopsnetOutfits.ID, "select_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Select> CODEC =
                StreamCodec.ofMember(
                        (value, buf) -> {
                            buf.writeVarInt(value.slot);
                            buf.writeVarInt(value.design);
                        },
                        buf -> new Select(buf.readVarInt(), buf.readVarInt()));

        @Override
        public Type<Select> type() {
            return ID;
        }
    }

    public record Sync(UUID player, int pin, int patch) implements CustomPacketPayload {
        public static final Type<Sync> ID =
                new Type<>(ResourceLocation.fromNamespaceAndPath(NoopsnetOutfits.ID, "sync_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Sync> CODEC =
                StreamCodec.ofMember(
                        (value, buf) -> {
                            buf.writeUUID(value.player);
                            buf.writeVarInt(value.pin);
                            buf.writeVarInt(value.patch);
                        },
                        buf -> new Sync(buf.readUUID(), buf.readVarInt(), buf.readVarInt()));

        @Override
        public Type<Sync> type() {
            return ID;
        }

        public Outfit outfit() {
            return new Outfit(
                    Design.fromWire(pin).orElse(Design.NONE),
                    Design.fromWire(patch).orElse(Design.NONE));
        }

        public static Sync of(UUID player, Outfit outfit) {
            return new Sync(player, outfit.pin().ordinal(), outfit.patch().ordinal());
        }
    }

    public record Open(int pin, int patch) implements CustomPacketPayload {
        public static final Type<Open> ID =
                new Type<>(ResourceLocation.fromNamespaceAndPath(NoopsnetOutfits.ID, "open_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC =
                StreamCodec.ofMember(
                        (value, buf) -> {
                            buf.writeVarInt(value.pin);
                            buf.writeVarInt(value.patch);
                        },
                        buf -> new Open(buf.readVarInt(), buf.readVarInt()));

        @Override
        public Type<Open> type() {
            return ID;
        }

        public Outfit outfit() {
            return new Outfit(
                    Design.fromWire(pin).orElse(Design.NONE),
                    Design.fromWire(patch).orElse(Design.NONE));
        }
    }
}
