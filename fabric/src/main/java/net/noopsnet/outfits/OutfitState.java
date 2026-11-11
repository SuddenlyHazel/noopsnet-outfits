// SPDX-License-Identifier: MPL-2.0
package net.noopsnet.outfits;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.PersistentState;

/** Cosmetic choices only; no identity/profile inference and nothing equipped by default. */
public final class OutfitState extends PersistentState {
    public static final Type<OutfitState> TYPE = new Type<>(OutfitState::new, OutfitState::read, null);
    private final Map<UUID, Outfit> outfits = new HashMap<>();

    public Outfit get(UUID player) { return outfits.getOrDefault(player, Outfit.EMPTY); }

    public boolean set(UUID player, Outfit outfit) {
        if (get(player).equals(outfit)) return false;
        if (outfit.equals(Outfit.EMPTY)) outfits.remove(player);
        else outfits.put(player, outfit);
        markDirty();
        return true;
    }

    public static OutfitState read(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        OutfitState state = new OutfitState();
        NbtCompound players = nbt.getCompound("players");
        for (String key : players.getKeys()) {
            try {
                NbtCompound entry = players.getCompound(key);
                Outfit outfit = new Outfit(Design.fromId(entry.getString("pin")).orElse(Design.NONE),
                        Design.fromId(entry.getString("patch")).orElse(Design.NONE));
                if (!outfit.equals(Outfit.EMPTY)) state.outfits.put(UUID.fromString(key), outfit);
            } catch (IllegalArgumentException ignored) {
                // A malformed UUID must not prevent other players' preferences from loading.
            }
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtCompound players = new NbtCompound();
        outfits.forEach((uuid, outfit) -> {
            NbtCompound entry = new NbtCompound();
            entry.putString("pin", outfit.pin().id());
            entry.putString("patch", outfit.patch().id());
            players.put(uuid.toString(), entry);
        });
        nbt.put("players", players);
        nbt.putInt("schema", 1);
        return nbt;
    }
}
