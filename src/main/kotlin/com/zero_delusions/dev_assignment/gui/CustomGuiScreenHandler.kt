package com.zero_delusions.dev_assignment.gui

import net.minecraft.core.component.DataComponents
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ClickType
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

// For input and search field, custom MenuType should be created
// where input field slots and custom methods would be integrated
class CustomGuiScreenHandler(syncId: Int, inventory: Inventory, player: Player) :
    AbstractContainerMenu(MenuType.GENERIC_9x4, syncId) {

    private val pages: List<GuiPage> = listOf(PageOne(), PageTwo(), PageThree())
    var currentPage = 1

    init {
        // Add slots for the GUI
        for (row in 0 until 4) {
            for (col in 0 until 9) {
                val slotIndex = col + row * 9
                val xPos = 8 + col * 18
                val yPos = 18 + row * 18
                addSlot(object : Slot(inventory, slotIndex, xPos, yPos) {})
            }
        }
        updateGUI()
    }

    override fun slotsChanged(container: Container) {
        updateGUI()
    }

    override fun stillValid(player: Player): Boolean = true

    override fun quickMoveStack(player: Player, i: Int): ItemStack = ItemStack.EMPTY

    private fun updateGUI() {
        if (currentPage in 1..pages.size) {
            pages[currentPage - 1].populateSlots(this)
        }
    }

    private val actionMap: Map<Int, (Player) -> Unit> = mapOf(
        ButtonType.TOGGLE.ordinal to ::toggleSetting,
        ButtonType.SEARCH.ordinal to ::openSearch,
        ButtonType.PREVIOUS.ordinal to { _ -> previousPage() },
        ButtonType.NEXT.ordinal to { _ -> nextPage() }
    )

    override fun clicked(slotIndex: Int, button: Int, clickType: ClickType, player: Player) {
        if (slotIndex < 0 || clickType.ordinal != 0) return

        val slot = getSlot(slotIndex)
        if (!slot.hasItem()) return

        val stack = slot.item
        // Instead of DAMAGE in finial code custom DataComponentType will be used
        // To apply custom textures, serverside resourcepack should be used,
        // where each texture applied to specified DataComponent value
        val customModelData = stack.get(DataComponents.DAMAGE)
        actionMap[customModelData]?.invoke(player)
    }

    fun toggleSetting(player: Player) {
        println("Setting changed!")
    }

    fun openSearch(player: Player) {
        println("Search opened!")
    }

    fun previousPage() {
        if (currentPage > 1) {
            currentPage--
            updateGUI()
        }
    }

    fun nextPage() {
        if (currentPage < pages.size) {
            currentPage++
            updateGUI()
        }
    }

    fun createItemStack(item: Item, buttonType: ButtonType): ItemStack {
        return ItemStack(item).apply {
            set(DataComponents.DAMAGE, buttonType.ordinal)
        }
    }

    fun populateSlots(slots: Map<Int, ItemStack>) {
        for (i in 0 until 36) {
            val stack = slots.getOrDefault(i, ItemStack.EMPTY)
            setItem(i, 0, stack)
        }
    }

    fun createToggleButton(setting: String): ItemStack =
        ButtonFactory.createToggleButton(this, setting)

    fun createSearchButton(): ItemStack = ButtonFactory.createSearchButton(this)
    fun createPreviousButton(): ItemStack = ButtonFactory.createPreviousButton(this)
    fun createNextButton(): ItemStack = ButtonFactory.createNextButton(this)
}

enum class ButtonType() {
    TOGGLE,
    SEARCH,
    PREVIOUS,
    NEXT
}

object ButtonFactory {
    fun createToggleButton(handler: CustomGuiScreenHandler, setting: String): ItemStack {
        return handler.createItemStack(Items.TORCH, ButtonType.TOGGLE)
    }

    fun createSearchButton(handler: CustomGuiScreenHandler): ItemStack {
        return handler.createItemStack(Items.COMPASS, ButtonType.SEARCH)
    }

    fun createPreviousButton(handler: CustomGuiScreenHandler): ItemStack {
        return handler.createItemStack(Items.APPLE, ButtonType.PREVIOUS)
    }

    fun createNextButton(handler: CustomGuiScreenHandler): ItemStack {
        return handler.createItemStack(Items.GOLDEN_APPLE, ButtonType.NEXT)
    }
}

interface GuiPage {
    fun populateSlots(handler: CustomGuiScreenHandler)
}

class PageOne : GuiPage {
    override fun populateSlots(handler: CustomGuiScreenHandler) {
        handler.populateSlots(
            mapOf(
                0 to handler.createToggleButton("setting_1"),
                35 to handler.createNextButton()
            )
        )
    }
}

class PageTwo : GuiPage {
    override fun populateSlots(handler: CustomGuiScreenHandler) {
        handler.populateSlots(
            mapOf(
                1 to handler.createToggleButton("setting_2"),
                27 to handler.createPreviousButton(),
                35 to handler.createNextButton()
            )
        )
    }
}

class PageThree : GuiPage {
    override fun populateSlots(handler: CustomGuiScreenHandler) {
        handler.populateSlots(
            mapOf(
                2 to handler.createToggleButton("setting_3"),
                27 to handler.createPreviousButton()
            )
        )
    }
}