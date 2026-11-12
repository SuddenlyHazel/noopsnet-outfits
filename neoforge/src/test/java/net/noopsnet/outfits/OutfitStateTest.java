// SPDX-License-Identifier: MPL-2.0
package net.noopsnet.outfits;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.nbt.CompoundTag;

import org.junit.jupiter.api.Test;

import java.util.UUID;

class OutfitStateTest {
    @Test
    void pinsAndPatchesPersistIndependentlyAndClearingRemovesTheEntry() {
        UUID player = UUID.randomUUID();
        OutfitState state = new OutfitState();
        assertEquals(Outfit.EMPTY, state.get(player));
        Outfit outfit = Outfit.EMPTY.with(0, Design.TRANS).with(1, Design.NONBINARY);
        assertTrue(state.set(player, outfit));
        assertTrue(state.isDirty());
        state.setDirty(false);
        assertFalse(state.set(player, outfit));
        assertFalse(state.isDirty());
        OutfitState loaded = OutfitState.read(state.save(new CompoundTag(), null), null);
        assertEquals(outfit, loaded.get(player));
        assertEquals(new Outfit(Design.NONE, Design.NONBINARY), outfit.with(0, Design.NONE));
        loaded.set(player, Outfit.EMPTY);
        assertTrue(loaded.save(new CompoundTag(), null).getCompound("players").isEmpty());
    }

    @Test
    void corruptedEntriesDoNotLoseOtherPlayersOrInventSelections() {
        UUID valid = UUID.randomUUID();
        CompoundTag nbt = new CompoundTag();
        CompoundTag players = new CompoundTag();
        CompoundTag entry = new CompoundTag();
        entry.putString("pin", "unrecognized_future_design");
        entry.putString("patch", "trans");
        players.put(valid.toString(), entry);
        players.put("not-a-uuid", entry);
        nbt.put("players", players);
        OutfitState loaded = OutfitState.read(nbt, null);
        assertEquals(new Outfit(Design.NONE, Design.TRANS), loaded.get(valid));
        assertEquals(
                1, loaded.save(new CompoundTag(), null).getCompound("players").getAllKeys().size());
    }

    @Test
    void invalidSelectionsAndSlotsAreRejected() {
        assertTrue(Design.fromWire(-1).isEmpty());
        assertTrue(Design.fromWire(Integer.MAX_VALUE).isEmpty());
        assertTrue(Design.fromId("../../other/path").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> Outfit.EMPTY.with(2, Design.TRANS));
        assertThrows(NullPointerException.class, () -> new Outfit(null, Design.TRANS));
        for (Design design : Design.values()) {
            assertEquals(design, Design.fromId(design.id()).orElseThrow());
            assertEquals(design, Design.fromWire(design.ordinal()).orElseThrow());
        }
    }
}
