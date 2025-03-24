package com.zero_delusions.dev_assignment.network

import com.cobblemon.mod.common.pokemon.Pokemon
import kotlinx.serialization.json.Json
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPubSub
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object PokemonTradeManager {

    private val pendingTrades = ConcurrentHashMap<String, TradeRequest>()
    private val TRADE_TIMEOUT_SECONDS = 120

    data class TradeRequest(
        val requesterId: String,
        val requesterServer: String,
        val requesterPokemonSlot: Int,
        val targetId: String,
        val targetServer: String,
        val timestamp: Long = System.currentTimeMillis(),
    )

    fun requestTrade(
        requesterId: String,
        requesterServer: String,
        requesterPokemonSlot: Int,
        targetId: String,
        targetServer: String,
    ): Boolean {
        val tradeId = "trade:${UUID.randomUUID()}"
        // Verify the Pokémon exists and can be traded
        val pokemon = getPlayerPokemonFromSlot(targetServer, requesterId, requesterPokemonSlot) ?: return false
        if (!canTradePokemon(pokemon)) return false
        // Lock the Pokémon to prevent changes while trade is pending
        lockPokemon(targetServer, requesterId, requesterPokemonSlot)

        val request = TradeRequest(
            requesterId,
            requesterServer,
            requesterPokemonSlot,
            targetId,
            targetServer
        )
        pendingTrades[tradeId] = request

        // Set up a timeout to auto-cancel
        scheduleTradeTimeout(tradeId)

        // Send the trade request to the target server
        if (requesterServer == targetServer) {
            // Local trade - notify target player directly
            notifyPlayerOfTradeRequest(targetId, requesterId, tradeId)
        } else {
            val redis = JedisPool().resource
            val dataKey = "pokemontrade:request:$tradeId"
            redis.setex(dataKey, TRADE_TIMEOUT_SECONDS, Json.encodeToString(request))
            // Send notification to target server
            NetworkRequest(
                targetServer = targetServer,
                payload = "trade_request|$dataKey|$tradeId"
            ).sendAndWait(5000)

            redis.close()
        }

        return true
    }

    // Accepts a trade request
    fun acceptTrade(
        tradeId: String,
        targetId: String,
        targetPokemonSlot: Int,
    ): Boolean {
        val request = pendingTrades[tradeId] ?: return false

        // Verify the acceptor matches the target in the request
        if (request.targetId != targetId) return false
        // Get and validate the target Pokémon
        val targetPokemon =
            getPlayerPokemonFromSlot(request.requesterServer, targetId, targetPokemonSlot) ?: return false
        if (!canTradePokemon(targetPokemon)) return false

        // Lock the target Pokémon
        lockPokemon(request.requesterServer, targetId, targetPokemonSlot)

        // Execute the trade
        if (request.requesterServer == request.targetServer) {
            // Same server trade
            executeTrade(
                request.requesterId,
                request.requesterPokemonSlot,
                targetId,
                targetPokemonSlot
            )
            notifyTradeComplete(request.requesterId, targetId)
        } else {
            // Cross-server trade
            val redis = JedisPool().resource
            // Serialize Pokémon data
            val targetPokemonData = serializePokemon(targetPokemon)
            val tradeDataKey = "pokemontrade:data:$tradeId"
            // Store target Pokémon in Redis
            redis.setex(tradeDataKey, 60, targetPokemonData)
            // Notify requester server
            val response = NetworkRequest(
                targetServer = request.requesterServer,
                payload = "trade_accept|$tradeDataKey|$tradeId"
            ).sendAndWait(10000)

            if (response == "COMPLETE") {
                // Trade completed successfully

                // Probably instead of removing and adding Pokémon, a direct Pokémon set can be used to change Pokémon data to needed one
                removePokemonFromPlayer(targetId, targetPokemonSlot)
                // Load the requester's Pokémon from the response
                val requesterPokemonKey = response.split("|")[1]
                val requesterPokemon = deserializePokemon(redis.get(requesterPokemonKey))
                addPokemonToPlayer(targetId, requesterPokemon)

                notifyTradeComplete(targetId)
            } else {
                // Trade failed
                unlockPokemon(targetId, targetPokemonSlot)
                notifyTradeFailed(targetId)
            }

            redis.close()
        }

        // Remove pending trade
        pendingTrades.remove(tradeId)
        return true
    }

    // Denies a trade request
    fun denyTrade(tradeId: String, targetId: String): Boolean {
        val request = pendingTrades[tradeId] ?: return false

        // Verify the denier matches the target
        if (request.targetId != targetId) return false
        // Unlock the requester's Pokémon
        if (request.requesterServer == request.targetServer) {
            unlockPokemon(request.requesterId, request.requesterPokemonSlot)
        } else {
            // Notify the requester's server to unlock
            NetworkRequest(
                targetServer = request.requesterServer,
                payload = "trade_deny|$tradeId"
            ).sendAndWait(10000)
        }

        // Remove the pending trade
        pendingTrades.remove(tradeId)
        // Notify both players
        notifyTradeDenied(request.requesterId, targetId)

        return true
    }

    // Set up request listener for cross-server trade communications
    fun setupTradeRequestListener(currentServerId: String) {
        val jedis = JedisPool().resource

        val pubSub = object : JedisPubSub() {
            override fun onMessage(channel: String, message: String) {
                if (channel == "server:${currentServerId}:requests") {
                    val parts = message.split("|")
                    when (parts[0]) {
                        "trade_request" -> {
                            val dataKey = parts[1]
                            val tradeId = parts[2]
                            val requestData = Json.decodeFromString<TradeRequest>(jedis.get(dataKey))
                            // Store locally and notify player
                            pendingTrades[tradeId] = requestData
                            notifyPlayerOfTradeRequest(requestData.targetId, requestData.requesterId, tradeId)
                        }

                        "trade_accept" -> {
                            val dataKey = parts[1]
                            val tradeId = parts[2]

                            val request = pendingTrades[tradeId] ?: return

                            // Get requester's Pokémon
                            val requesterPokemon = getPlayerPokemonFromSlot(
                                request.requesterServer,
                                request.requesterId,
                                request.requesterPokemonSlot
                            )
                            // Get target's Pokémon from Redis
                            val targetPokemonData = jedis.get(dataKey)
                            val targetPokemon = deserializePokemon(targetPokemonData)

                            // Store requester's Pokémon in Redis for the target server
                            val requesterPokemonKey = "pokemontrade:response:$tradeId"
                            jedis.setex(requesterPokemonKey, 60, serializePokemon(requesterPokemon!!))

                            // Execute trade on this side
                            removePokemonFromPlayer(request.requesterId, request.requesterPokemonSlot)
                            addPokemonToPlayer(request.requesterId, targetPokemon)

                            // Respond with completion
                            val responseChannel = message.split("|").last()
                            jedis.publish(responseChannel, "COMPLETE|$requesterPokemonKey")

                            // Notify player
                            notifyTradeComplete(request.requesterId)

                            // Remove pending trade
                            pendingTrades.remove(tradeId)
                        }

                        "trade_deny" -> {
                            val tradeId = parts[1]
                            val request = pendingTrades[tradeId] ?: return

                            // Unlock Pokémon
                            unlockPokemon(request.requesterId, request.requesterPokemonSlot)

                            // Notify player
                            notifyTradeDenied(request.requesterId, request.targetId)

                            // Remove pending trade
                            pendingTrades.remove(tradeId)
                        }
                    }
                }
            }
        }

        // Start listening in a background thread
        Thread { jedis.subscribe(pubSub, "server:${currentServerId}:requests") }.start()
    }

    // In real implementation, we gonna get dara from player on a specified server via Velocity

    private fun getPlayerPokemonFromSlot(server: String, playerId: String, slot: Int): Pokemon? = Pokemon()
    private fun canTradePokemon(pokemon: Pokemon): Boolean = true

    private fun lockPokemon(server: String, playerId: String, slot: Int) {}
    private fun unlockPokemon(playerId: String, slot: Int) {}

    private fun scheduleTradeTimeout(tradeId: String) {}
    private fun executeTrade(requesterId: String, requesterSlot: Int, targetId: String, targetSlot: Int) {}

    private fun notifyPlayerOfTradeRequest(targetPlayerId: String, requesterId: String, tradeId: String) {}
    private fun notifyTradeComplete(playerId: String, targetId: String? = null) {}
    private fun notifyTradeFailed(playerId: String) {}
    private fun notifyTradeDenied(requesterId: String, targetId: String) {}

    private fun serializePokemon(pokemon: Pokemon): String = ""
    private fun deserializePokemon(data: String): Pokemon = Pokemon()

    private fun removePokemonFromPlayer(playerId: String, slot: Int) {}
    private fun addPokemonToPlayer(playerId: String, pokemon: Pokemon) {}
}