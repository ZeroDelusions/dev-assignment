package com.zero_delusions.dev_assignment.gui;

import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class PageTwo implements GuiPage {
    @Override
    public void populateSlots(CustomGuiScreenHandler handler) {
        Map<Integer, ItemStack> slots = new HashMap<>();
        slots.put(1, handler.createToggleButton("setting_2"));
        slots.put(27, handler.createPreviousButton());
        slots.put(35, handler.createNextButton());
        handler.populateSlots(slots);
    }
}
