// SPDX-License-Identifier: MPL-2.0
package net.noopsnet.outfits;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.saveddata.SavedData;

/** Cosmetic choices only; no identity/profile inference and nothing equipped by default. */
public final class OutfitState extends SavedData {
    public static final Factory<OutfitState> TYPE = new Factory<>(OutfitState::new, OutfitState::read, null);
    private final Map<UUID, Outfit> outfits = new HashMap<>();

    public Outfit get(UUID player) { return outfits.getOrDefault(player, Outfit.EMPTY); }

    public boolean set(UUID player, Outfit outfit) {
        if (get(player).equals(outfit)) return false;
        if (outfit.equals(Outfit.EMPTY)) outfits.remove(player);
        else outfits.put(player, outfit);
        setDirty();
        return true;
    }

    public static OutfitState read(CompoundTag nbt, HolderLookup.Provider registries) {
        OutfitState state = new OutfitState();
        CompoundTag players = nbt.getCompound("players");
        for (String key : players.getAllKeys()) {
            try {
                CompoundTag entry = players.getCompound(key);
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
    public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries) {
        CompoundTag players = new CompoundTag();
        outfits.forEach((uuid, outfit) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("pin", outfit.pin().id());
            entry.putString("patch", outfit.patch().id());
            players.put(uuid.toString(), entry);
        });
        nbt.put("players", players);
        nbt.putInt("schema", 1);
        return nbt;
    }
}
