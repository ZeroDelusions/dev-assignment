package com.zero_delusions.dev_assignment.command

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.zero_delusions.dev_assignment.CustomGuiScreenHandler
import net.minecraft.commands.CommandSourceStack
import net.minecraft.network.chat.Component
import net.minecraft.world.SimpleMenuProvider

object OpenGuiCommand {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        dispatcher.register(
            literal<CommandSourceStack>("opengui")
                .executes { context ->
                    val player = context.source.player
                    if (player != null) {
                        player.openMenu(SimpleMenuProvider(
                            { syncId, inventory, p ->
                                CustomGuiScreenHandler(syncId, inventory, p)
                            },
                            Component.literal("Settings")
                        ))
                        1
                    } else {
                        0
                    }
                }
        )
    }
}