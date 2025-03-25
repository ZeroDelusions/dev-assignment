package com.zero_delusions.dev_assignment.gui;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ButtonFactory {
    public static ItemStack createToggleButton(CustomGuiScreenHandler handler, String setting) {
        return handler.createItemStack(Items.TORCH, ButtonType.TOGGLE);
    }

    public static ItemStack createSearchButton(CustomGuiScreenHandler handler) {
        return handler.createItemStack(Items.COMPASS, ButtonType.SEARCH);
    }

    public static ItemStack createPreviousButton(CustomGuiScreenHandler handler) {
        return handler.createItemStack(Items.APPLE, ButtonType.PREVIOUS);
    }

    public static ItemStack createNextButton(CustomGuiScreenHandler handler) {
        return handler.createItemStack(Items.GOLDEN_APPLE, ButtonType.NEXT);
    }
}
