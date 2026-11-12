// SPDX-License-Identifier: MPL-2.0
package net.noopsnet.outfits.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import net.noopsnet.outfits.Design;
import net.noopsnet.outfits.Outfit;
import net.noopsnet.outfits.OutfitPayloads;

public final class OutfitScreen extends Screen {
    private Outfit outfit;
    private int left;
    private int top;
    private int rowStep;
    private int panelHeight;

    public OutfitScreen(Outfit outfit) {
        super(Component.translatable("noopsnet_outfits.title"));
        this.outfit = outfit;
    }

    public void update(Outfit outfit) {
        this.outfit = outfit;
        clearWidgets();
        init();
    }

    @Override
    protected void init() {
        left = (width - 320) / 2;
        rowStep = Math.min(20, Math.max(14, (height - 103) / Design.values().length));
        panelHeight = 95 + Design.values().length * rowStep;
        top = Math.max(2, (height - panelHeight) / 2);
        for (int slot = 0; slot < 2; slot++) {
            final int selectedSlot = slot;
            for (Design design : Design.values()) {
                boolean selected = (slot == 0 ? outfit.pin() : outfit.patch()) == design;
                Component label = Component.translatable(design.translationKey());
                if (selected)
                    label =
                            Component.literal("[ ")
                                    .append(label)
                                    .append(" ]")
                                    .withStyle(ChatFormatting.LIGHT_PURPLE);
                addRenderableWidget(
                        Button.builder(
                                        label,
                                        button -> {
                                            if (Minecraft.getInstance().getConnection() != null
                                                    && Minecraft.getInstance()
                                                            .getConnection()
                                                            .hasChannel(OutfitPayloads.Select.ID)) {
                                                PacketDistributor.sendToServer(
                                                        new OutfitPayloads.Select(
                                                                selectedSlot, design.ordinal()));
                                            }
                                        })
                                .bounds(
                                        left + 12 + slot * 156,
                                        top + 47 + design.ordinal() * rowStep,
                                        140,
                                        rowStep - 1)
                                .build());
            }
        }
        addRenderableWidget(
                Button.builder(Component.translatable("gui.done"), button -> onClose())
                        .bounds(left + 110, top + 67 + Design.values().length * rowStep, 100, 20)
                        .build());
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.fill(left, top, left + 320, top + panelHeight, 0xEF211D31);
        context.fill(left, top, left + 320, top + 2, 0xFFF4A8CF);
        context.drawCenteredString(font, title, width / 2, top + 10, 0xFFE9D9F5);
        context.drawCenteredString(
                font,
                Component.translatable("noopsnet_outfits.pin"),
                left + 82,
                top + 31,
                0xFFB6DCF5);
        context.drawCenteredString(
                font,
                Component.translatable("noopsnet_outfits.patch"),
                left + 238,
                top + 31,
                0xFFF3C3DC);
        super.render(context, mouseX, mouseY, delta);
        for (int slot = 0; slot < 2; slot++) {
            for (Design design : Design.values()) {
                if (design == Design.NONE) continue;
                context.blit(
                        OutfitFeatureRenderer.texture(slot == 0 ? "pin" : "patch", design),
                        left + 16 + slot * 156,
                        top + 47 + (rowStep - 16) / 2 + design.ordinal() * rowStep,
                        0,
                        0,
                        16,
                        16,
                        16,
                        16);
            }
        }
        context.drawCenteredString(
                font,
                Component.translatable("noopsnet_outfits.visibility"),
                width / 2,
                top + 52 + Design.values().length * rowStep,
                0xFFADA6BD);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
