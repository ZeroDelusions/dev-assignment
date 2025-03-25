package com.zero_delusions.dev_assignment.gui;

import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class PageThree implements GuiPage {
    @Override
    public void populateSlots(CustomGuiScreenHandler handler) {
        Map<Integer, ItemStack> slots = new HashMap<>();
        slots.put(2, handler.createToggleButton("setting_3"));
        slots.put(27, handler.createPreviousButton());
        handler.populateSlots(slots);
    }
}
