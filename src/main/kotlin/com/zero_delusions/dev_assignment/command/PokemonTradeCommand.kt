package com.zero_delusions.dev_assignment.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.builder.RequiredArgumentBuilder.argument
import com.mojang.brigadier.context.CommandContext
import com.zero_delusions.dev_assignment.network.PokemonTradeManager
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.selector.EntitySelector
import net.minecraft.network.chat.Component
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object PokemonTradeCommand {
    private const val NAME = "trade-pokemon"
    private const val PLAYER = "player"
    private const val SLOT = "slot"
    private const val ACCEPT = "accept"
    private const val DENY = "deny"

    private val activeRequests = ConcurrentHashMap<String, String>()

    fun register(dispatcher: CommandDispatcher<CommandSourceStack>) {
        // Create a trade request
        val initiateTradeCommand = argument<CommandSourceStack, EntitySelector>(PLAYER, EntityArgument.player())
            .then(
                argument<CommandSourceStack, Int>(SLOT, IntegerArgumentType.integer(1, 6))
                    .executes { context -> executeInitiateTrade(context) }
            )

        // Accept a trade request
        val acceptTradeCommand = literal<CommandSourceStack>(ACCEPT)
            .then(
                argument<CommandSourceStack, EntitySelector>(PLAYER, EntityArgument.player())
                    .then(
                        argument<CommandSourceStack, Int>(SLOT, IntegerArgumentType.integer(1, 6))
                            .executes { context -> executeAcceptTrade(context) }
                    )
            )

        // Deny a trade request
        val denyTradeCommand = literal<CommandSourceStack>(DENY)
            .then(
                argument<CommandSourceStack, EntitySelector>(PLAYER, EntityArgument.player())
                    .executes { context -> executeDenyTrade(context) }
            )

        dispatcher.register(
            literal<CommandSourceStack>(NAME)
                .then(initiateTradeCommand)
                .then(acceptTradeCommand)
                .then(denyTradeCommand)
        )
    }

    private fun executeInitiateTrade(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val player = source.playerOrException
        val targetPlayer = EntityArgument.getPlayer(context, PLAYER)
        val slot = IntegerArgumentType.getInteger(context, SLOT)

        // Check if player already has an active trade
        if (activeRequests.containsKey(player.stringUUID)) {
            source.sendFailure(Component.literal("You already have an active trade request. Wait for it to expire or be processed."))
            return 0
        }

        // Get server information
        val currentServer = getCurrentServerId()
        val targetServer = getPlayerServer(targetPlayer.stringUUID) ?: currentServer

        // Request the trade
        val success = PokemonTradeManager.requestTrade(
            player.stringUUID,
            currentServer,
            slot,
            targetPlayer.stringUUID,
            targetServer
        )

        if (success) {
            val tradeId = "trade:${UUID.randomUUID()}"
            activeRequests[player.stringUUID] = tradeId

            source.sendSuccess(
                { Component.literal("Trade request sent to ${targetPlayer.name.string}. Waiting for response...") },
                false
            )
            return Command.SINGLE_SUCCESS
        } else {
            source.sendFailure(Component.literal("Could not initiate trade. Make sure the Pokémon in slot $slot can be traded."))
            return 0
        }
    }

    private fun executeAcceptTrade(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val player = source.playerOrException
        val requesterPlayer = EntityArgument.getPlayer(context, PLAYER)
        val slot = IntegerArgumentType.getInteger(context, SLOT)

        // Find the trade request
        val tradeId = findTradeRequest(requesterPlayer.stringUUID, player.stringUUID)

        if (tradeId == null) {
            source.sendFailure(Component.literal("No pending trade request from ${requesterPlayer.name.string}"))
            return 0
        }

        // Accept the trade
        val success = PokemonTradeManager.acceptTrade(tradeId, player.stringUUID, slot)

        if (success) {
            // Clean up active requests
            activeRequests.remove(requesterPlayer.stringUUID)

            source.sendSuccess(
                { Component.literal("Trade accepted! Exchanging Pokémon with ${requesterPlayer.name.string}...") },
                false
            )
            return Command.SINGLE_SUCCESS
        } else {
            source.sendFailure(Component.literal("Could not complete the trade. Make sure the Pokémon in slot $slot can be traded."))
            return 0
        }
    }

    private fun executeDenyTrade(context: CommandContext<CommandSourceStack>): Int {
        val source = context.source
        val player = source.playerOrException
        val requesterPlayer = EntityArgument.getPlayer(context, PLAYER)

        // Find the trade request
        val tradeId = findTradeRequest(requesterPlayer.stringUUID, player.stringUUID)

        if (tradeId == null) {
            source.sendFailure(Component.literal("No pending trade request from ${requesterPlayer.name.string}"))
            return 0
        }

        // Deny the trade
        val success = PokemonTradeManager.denyTrade(tradeId, player.stringUUID)

        if (success) {
            // Clean up active requests
            activeRequests.remove(requesterPlayer.stringUUID)

            source.sendSuccess(
                { Component.literal("Trade request from ${requesterPlayer.name.string} denied.") },
                false
            )
            return Command.SINGLE_SUCCESS
        } else {
            source.sendFailure(Component.literal("Could not deny the trade. The request may have expired."))
            return 0
        }
    }

    private fun findTradeRequest(requesterId: String, targetId: String): String? {
        return activeRequests[requesterId]
    }

    private fun getCurrentServerId(): String = "server1" // Get current server ID
    private fun getPlayerServer(playerId: String): String? = null // Query which server a player is on
}