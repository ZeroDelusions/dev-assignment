package com.zero_delusions.dev_assignment.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zero_delusions.dev_assignment.network.PokemonTradeManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;

public final class PokemonTradeCommand {
    private static final String NAME = "trade-pokemon";
    private static final String PLAYER = "player";
    private static final String SLOT = "slot";
    private static final String ACCEPT = "accept";
    private static final String DENY = "deny";

    private static final ConcurrentHashMap<String, String> activeRequests = new ConcurrentHashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Build the initiate trade command: /trade-pokemon <player> <slot>
        RequiredArgumentBuilder<CommandSourceStack, EntitySelector> playerArgInitiate =
                argument(PLAYER, EntityArgument.player());

        RequiredArgumentBuilder<CommandSourceStack, Integer> slotArg =
                RequiredArgumentBuilder.<CommandSourceStack, Integer>argument(SLOT, IntegerArgumentType.integer(1, 6))
                        .executes(PokemonTradeCommand::executeInitiateTrade);

        RequiredArgumentBuilder<CommandSourceStack, EntitySelector> initiateTradeCommand = playerArgInitiate.then(slotArg);

        // Build the accept trade command: /trade-pokemon accept <player> <slot>
        RequiredArgumentBuilder<CommandSourceStack, EntitySelector> playerArgAccept =
                argument(PLAYER, EntityArgument.player());

        RequiredArgumentBuilder<CommandSourceStack, Integer> slotArgAccept =
                RequiredArgumentBuilder.<CommandSourceStack, Integer>argument(SLOT, IntegerArgumentType.integer(1, 6))
                        .executes(PokemonTradeCommand::executeAcceptTrade);

        LiteralArgumentBuilder<CommandSourceStack> acceptTradeCommand =
                LiteralArgumentBuilder.<CommandSourceStack>literal(ACCEPT)
                        .then(playerArgAccept.then(slotArgAccept));

        // Build the deny trade command: /trade-pokemon deny <player>
        RequiredArgumentBuilder<CommandSourceStack, EntitySelector> playerArgDeny =
                RequiredArgumentBuilder.<CommandSourceStack, EntitySelector>argument(PLAYER, EntityArgument.player())
                        .executes(PokemonTradeCommand::executeDenyTrade);

        LiteralArgumentBuilder<CommandSourceStack> denyTradeCommand =
                LiteralArgumentBuilder.<CommandSourceStack>literal(DENY)
                        .then(playerArgDeny);

        // Register the main command with subcommands
        LiteralArgumentBuilder<CommandSourceStack> command =
                LiteralArgumentBuilder.<CommandSourceStack>literal(NAME)
                        .then(initiateTradeCommand)
                        .then(acceptTradeCommand)
                        .then(denyTradeCommand);
        dispatcher.register(command);
    }


    private static int executeInitiateTrade(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        var player = source.getPlayerOrException();
        var targetPlayer = EntityArgument.getPlayer(context, PLAYER);
        int slot = IntegerArgumentType.getInteger(context, SLOT);

        // Check if the player already has an active trade
        if (activeRequests.containsKey(player.getStringUUID())) {
            source.sendFailure(Component.literal("You already have an active trade request. Wait for it to expire or be processed."));
            return 0;
        }

        // Get server information
        String currentServer = getCurrentServerId();
        String targetServer = getPlayerServer(targetPlayer.getStringUUID());
        if (targetServer == null) {
            targetServer = currentServer;
        }

        // Request the trade using our PokemonTradeManager
        boolean success = PokemonTradeManager.requestTrade(
                player.getStringUUID(),
                currentServer,
                slot,
                targetPlayer.getStringUUID(),
                targetServer
        );

        if (success) {
            // Create a unique trade id and store it for tracking
            String tradeId = "trade:" + UUID.randomUUID();
            activeRequests.put(player.getStringUUID(), tradeId);

            source.sendSuccess(() -> Component.literal("Trade request sent to " + targetPlayer.getName().getString() + ". Waiting for response..."), false);
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("Could not initiate trade. Make sure the Pokémon in slot " + slot + " can be traded."));
            return 0;
        }
    }

    private static int executeAcceptTrade(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        var player = source.getPlayerOrException();
        var requesterPlayer = EntityArgument.getPlayer(context, PLAYER);
        int slot = IntegerArgumentType.getInteger(context, SLOT);

        // Find the pending trade request from the requester to this player
        String tradeId = findTradeRequest(requesterPlayer.getStringUUID(), player.getStringUUID());
        if (tradeId == null) {
            source.sendFailure(Component.literal("No pending trade request from " + requesterPlayer.getName().getString()));
            return 0;
        }

        // Accept the trade using our PokemonTradeManager
        boolean success = PokemonTradeManager.acceptTrade(tradeId, player.getStringUUID(), slot);
        if (success) {
            // Clean up active requests after a successful trade
            activeRequests.remove(requesterPlayer.getStringUUID());
            source.sendSuccess(() -> Component.literal("Trade accepted! Exchanging Pokémon with " + requesterPlayer.getName().getString() + "..."), false);
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("Could not complete the trade. Make sure the Pokémon in slot " + slot + " can be traded."));
            return 0;
        }
    }

    private static int executeDenyTrade(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        var player = source.getPlayerOrException();
        var requesterPlayer = EntityArgument.getPlayer(context, PLAYER);

        // Find the pending trade request from the requester to this player
        String tradeId = findTradeRequest(requesterPlayer.getStringUUID(), player.getStringUUID());
        if (tradeId == null) {
            source.sendFailure(Component.literal("No pending trade request from " + requesterPlayer.getName().getString()));
            return 0;
        }

        // Deny the trade using our PokemonTradeManager
        boolean success = PokemonTradeManager.denyTrade(tradeId, player.getStringUUID());
        if (success) {
            // Clean up active requests after denial
            activeRequests.remove(requesterPlayer.getStringUUID());
            source.sendSuccess(() -> Component.literal("Trade request from " + requesterPlayer.getName().getString() + " denied."), false);
            return Command.SINGLE_SUCCESS;
        } else {
            source.sendFailure(Component.literal("Could not deny the trade. The request may have expired."));
            return 0;
        }
    }

    private static String findTradeRequest(String requesterId, String targetId) {
        return activeRequests.get(requesterId);
    }

    private static String getCurrentServerId() {
        return "server1";
    }

    private static String getPlayerServer(String playerId) {
        return null;
    }
}
