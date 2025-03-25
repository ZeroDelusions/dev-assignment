package com.zero_delusions.dev_assignment.gui;

import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class PageOne implements GuiPage {
    @Override
    public void populateSlots(CustomGuiScreenHandler handler) {
        Map<Integer, ItemStack> slots = new HashMap<>();
        slots.put(0, handler.createToggleButton("setting_1"));
        slots.put(35, handler.createNextButton());
        handler.populateSlots(slots);
    }
}
