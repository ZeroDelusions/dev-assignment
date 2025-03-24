package com.zero_delusions.dev_assignment.core.command

import com.mojang.brigadier.CommandDispatcher
import com.zero_delusions.dev_assignment.command.OpenGuiCommand
import com.zero_delusions.dev_assignment.command.PokemonTradeCommand
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands

object DevAssignmentCommands {
    fun register(
        dispatcher: CommandDispatcher<CommandSourceStack>,
        registry: CommandBuildContext,
        selection: Commands.CommandSelection,
    ) {
        OpenGuiCommand.register(dispatcher)
        PokemonTradeCommand.register(dispatcher)
    }
}