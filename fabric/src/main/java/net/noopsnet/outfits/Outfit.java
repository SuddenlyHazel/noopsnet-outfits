// SPDX-License-Identifier: MPL-2.0
package net.noopsnet.outfits;

import java.util.Objects;

public record Outfit(Design pin, Design patch) {
    public static final Outfit EMPTY = new Outfit(Design.NONE, Design.NONE);

    public Outfit {
        Objects.requireNonNull(pin);
        Objects.requireNonNull(patch);
    }

    public Outfit with(int slot, Design design) {
        return switch (slot) {
            case 0 -> new Outfit(design, patch);
            case 1 -> new Outfit(pin, design);
            default -> throw new IllegalArgumentException("Unknown outfit slot: " + slot);
        };
    }
}
