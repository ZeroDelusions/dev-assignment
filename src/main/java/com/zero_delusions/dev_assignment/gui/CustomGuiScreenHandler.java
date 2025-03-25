package com.zero_delusions.dev_assignment.gui;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;
import java.util.function.Consumer;

public final class CustomGuiScreenHandler extends AbstractContainerMenu {

    private final List<GuiPage> pages = Arrays.asList(new PageOne(), new PageTwo(), new PageThree());
    private int currentPage = 1;

    private final Map<Integer, Consumer<Player>> actionMap = new HashMap<Integer, Consumer<Player>>() {{
        put(ButtonType.TOGGLE.ordinal(), CustomGuiScreenHandler.this::toggleSetting);
        put(ButtonType.SEARCH.ordinal(), CustomGuiScreenHandler.this::openSearch);
        put(ButtonType.PREVIOUS.ordinal(), player -> previousPage());
        put(ButtonType.NEXT.ordinal(), player -> nextPage());
    }};

    public CustomGuiScreenHandler(int syncId, Inventory inventory, Player player) {
        super(MenuType.GENERIC_9x4, syncId);

        // Add slots for the GUI
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                int slotIndex = col + row * 9;
                int xPos = 8 + col * 18;
                int yPos = 18 + row * 18;
                addSlot(new Slot(inventory, slotIndex, xPos, yPos) {
                });
            }
        }
        updateGUI();
    }

    @Override
    public void slotsChanged(Container container) {
        updateGUI();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        return ItemStack.EMPTY;
    }

    private void updateGUI() {
        if (currentPage >= 1 && currentPage <= pages.size()) {
            pages.get(currentPage - 1).populateSlots(this);
        }
    }

    @Override
    public void clicked(int slotIndex, int button, ClickType clickType, Player player) {
        if (slotIndex < 0 || clickType.ordinal() != 0) {
            return;
        }

        Slot slot = getSlot(slotIndex);
        if (!slot.hasItem()) {
            return;
        }

        ItemStack stack = slot.getItem();
        // Instead of DAMAGE in finial code custom DataComponentType will be used
        // To apply custom textures, serverside resourcepack should be used,
        // where each texture applied to specified DataComponent value
        int customModelData = stack.get(DataComponents.DAMAGE);
        Consumer<Player> action = actionMap.get(customModelData);
        if (action != null) {
            action.accept(player);
        }
    }

    public void toggleSetting(Player player) {
        System.out.println("Setting changed!");
    }

    public void openSearch(Player player) {
        System.out.println("Search opened!");
    }

    public void previousPage() {
        if (currentPage > 1) {
            currentPage--;
            updateGUI();
        }
    }

    public void nextPage() {
        if (currentPage < pages.size()) {
            currentPage++;
            updateGUI();
        }
    }

    public ItemStack createItemStack(Item item, ButtonType buttonType) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.DAMAGE, buttonType.ordinal());
        return stack;
    }

    public void populateSlots(Map<Integer, ItemStack> slots) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = slots.getOrDefault(i, ItemStack.EMPTY);
            setItem(i, 0, stack);
        }
    }

    public ItemStack createToggleButton(String setting) {
        return ButtonFactory.createToggleButton(this, setting);
    }

    public ItemStack createSearchButton() {
        return ButtonFactory.createSearchButton(this);
    }

    public ItemStack createPreviousButton() {
        return ButtonFactory.createPreviousButton(this);
    }

    public ItemStack createNextButton() {
        return ButtonFactory.createNextButton(this);
    }
}


