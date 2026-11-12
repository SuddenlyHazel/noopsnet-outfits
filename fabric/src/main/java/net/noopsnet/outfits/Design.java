// SPDX-License-Identifier: MPL-2.0
package net.noopsnet.outfits;

import java.util.Arrays;
import java.util.Optional;

/** Stable IDs are persisted; wire IDs are scoped to the versioned v1 channels. */
public enum Design {
    NONE("none"),
    RAINBOW("rainbow"),
    TRANS("trans"),
    NONBINARY("nonbinary"),
    BI("bi"),
    PAN("pan"),
    LESBIAN("lesbian"),
    ACE("ace"),
    GENDERFLUID("genderfluid");

    private final String id;

    Design(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "noopsnet_outfits.design." + id;
    }

    public static Optional<Design> fromId(String id) {
        return Arrays.stream(values()).filter(design -> design.id.equals(id)).findFirst();
    }

    public static Optional<Design> fromWire(int index) {
        return index >= 0 && index < values().length
                ? Optional.of(values()[index])
                : Optional.empty();
    }
}
