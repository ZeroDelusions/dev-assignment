package com.zero_delusions.dev_assignment.command;

import com.mojang.brigadier.CommandDispatcher;
import com.zero_delusions.dev_assignment.gui.CustomGuiScreenHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;

public final class OpenGuiCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("opengui")
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            var player = source.getPlayer();
                            if (player != null) {
                                player.openMenu(new SimpleMenuProvider(
                                        CustomGuiScreenHandler::new,
                                        Component.literal("Settings")
                                ));
                                return 1;
                            } else {
                                return 0;
                            }
                        })
        );
    }
}
