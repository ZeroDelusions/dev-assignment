package com.zero_delusions.dev_assignment.network;


import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.Gson;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PokemonTradeManager {
    private static final ConcurrentHashMap<String, TradeRequest> pendingTrades = new ConcurrentHashMap<>();
    private static final int TRADE_TIMEOUT_SECONDS = 120;

    public static class TradeRequest {
        private final String requesterId;
        private final String requesterServer;
        private final int requesterPokemonSlot;
        private final String targetId;
        private final String targetServer;
        private final long timestamp;

        public TradeRequest(
            String requesterId,
            String requesterServer,
            int requesterPokemonSlot,
            String targetId,
            String targetServer
        ) {
            this(
                requesterId,
                requesterServer,
                requesterPokemonSlot,
                targetId, targetServer,
                System.currentTimeMillis()
            );
        }

        public TradeRequest(String requesterId, String requesterServer, int requesterPokemonSlot,
        String targetId, String targetServer, long timestamp) {
            this.requesterId = requesterId;
            this.requesterServer = requesterServer;
            this.requesterPokemonSlot = requesterPokemonSlot;
            this.targetId = targetId;
            this.targetServer = targetServer;
            this.timestamp = timestamp;
        }

        // Getters
        public String getRequesterId() { return requesterId; }
        public String getRequesterServer() { return requesterServer; }
        public int getRequesterPokemonSlot() { return requesterPokemonSlot; }
        public String getTargetId() { return targetId; }
        public String getTargetServer() { return targetServer; }
        public long getTimestamp() { return timestamp; }
    }

    public static boolean requestTrade(
        String requesterId,
        String requesterServer,
        int requesterPokemonSlot,
        String targetId,
        String targetServer
    ) {
        String tradeId = "trade:" + UUID.randomUUID();

        // Get the requester's Pokémon from the specified slot
        Pokemon pokemon = getPlayerPokemonFromSlot(targetServer, requesterId, requesterPokemonSlot);
        if (!canTradePokemon(pokemon)) return false;

        // Lock the Pokémon to prevent other trades
        lockPokemon(targetServer, requesterId, requesterPokemonSlot);

        TradeRequest request = new TradeRequest(
            requesterId,
            requesterServer,
            requesterPokemonSlot,
            targetId,
            targetServer
        );
        pendingTrades.put(tradeId, request);

        scheduleTradeTimeout(tradeId);

        if (requesterServer.equals(targetServer)) {
            // If both players are on the same server, notify directly
            notifyPlayerOfTradeRequest(targetId, requesterId, tradeId);
        } else {
            // Otherwise, send a network request to the target server with trade details
            try (JedisPool pool = new JedisPool();
                 var redis = pool.getResource()) {

                    String dataKey = "pokemontrade:request:" + tradeId;

                    Gson gson = new Gson();
                    redis.setex(dataKey, TRADE_TIMEOUT_SECONDS, gson.toJson(request));

                    new NetworkRequest(
                            targetServer,
                    "trade_request|" + dataKey + "|" + tradeId
                    ).sendAndWait(5000);
                }
            }
            return true;
        }

    public static boolean acceptTrade(String tradeId, String targetId, int targetPokemonSlot) {
        TradeRequest request = pendingTrades.get(tradeId);
        if (request == null || !request.getTargetId().equals(targetId)) return false;

        // Get the target's Pokémon from the specified slot
        Pokemon targetPokemon = getPlayerPokemonFromSlot(
                request.getRequesterServer(), targetId, targetPokemonSlot
        );
        if (!canTradePokemon(targetPokemon)) return false;

        lockPokemon(request.getRequesterServer(), targetId, targetPokemonSlot);

        if (request.getRequesterServer().equals(request.getTargetServer())) {
            // If both players are on the same server, execute the trade immediately
            executeTrade(
                request.getRequesterId(),
                request.getRequesterPokemonSlot(),
                targetId,
                targetPokemonSlot
            );
            notifyTradeComplete(request.getRequesterId(), targetId);
        } else {
            // For cross-server trades, send a network request
            try (JedisPool pool = new JedisPool();
                var redis = pool.getResource()) {

                    String targetPokemonData = serializePokemon(targetPokemon);
                    String tradeDataKey = "pokemontrade:data:" + tradeId;
                    redis.setex(tradeDataKey, 60, targetPokemonData);

                    String response = new NetworkRequest(
                        request.getRequesterServer(),
                        "trade_accept|" + tradeDataKey + "|" + tradeId
                    ).sendAndWait(10000);

                    if ("COMPLETE".equals(response)) {
                        // On trade complete replace Pokémon
                        removePokemonFromPlayer(targetId, targetPokemonSlot);

                        String requesterPokemonKey = response.split("\\|")[1];
                        Pokemon requesterPokemon = deserializePokemon(redis.get(requesterPokemonKey));

                        addPokemonToPlayer(targetId, requesterPokemon);
                        notifyTradeComplete(targetId);
                    } else {
                        // If the trade was not successful, unlock the Pokémon and notify failure
                        unlockPokemon(targetId, targetPokemonSlot);
                        notifyTradeFailed(targetId);
                    }
                }
            }

            pendingTrades.remove(tradeId);
            return true;
        }

    public static boolean denyTrade(String tradeId, String targetId) {
        TradeRequest request = pendingTrades.get(tradeId);
        if (request == null || !request.getTargetId().equals(targetId)) return false;

        if (request.getRequesterServer().equals(request.getTargetServer())) {
            // For same-server trades, unlock the Pokémon reserved by the requester
            unlockPokemon(request.getRequesterId(), request.getRequesterPokemonSlot());
        } else {
            // For cross-server trades, notify the requester server of the denial
            new NetworkRequest(
                    request.getRequesterServer(),
            "trade_deny|" + tradeId
            ).sendAndWait(10000);
        }

        pendingTrades.remove(tradeId);
        notifyTradeDenied(request.getRequesterId(), targetId);
        return true;
    }

    public static void setupTradeRequestListener(String currentServerId) {
        new Thread(() -> {
            try (JedisPool pool = new JedisPool();
                var jedis = pool.getResource()) {

                    jedis.subscribe(new JedisPubSub() {
                        @Override
                        public void onMessage(String channel, String message) {
                            if (channel.equals("server:" + currentServerId + ":requests")) {
                                String[] parts = message.split("\\|");
                                switch (parts[0]) {
                                    case "trade_request":
                                        handleTradeRequest(parts, jedis);
                                        break;
                                    case "trade_accept":
                                        handleTradeAccept(parts, jedis);
                                        break;
                                    case "trade_deny":
                                        handleTradeDeny(parts);
                                        break;
                                }
                            }
                        }
                    }, "server:" + currentServerId + ":requests");
                }
            }).start();
    }

    private static void handleTradeRequest(String[] parts, Jedis jedis) {
        String dataKey = parts[1];
        String tradeId = parts[2];

        Gson gson = new Gson();
        TradeRequest requestData = gson.fromJson(jedis.get(dataKey), TradeRequest.class);

        pendingTrades.put(tradeId, requestData);
        notifyPlayerOfTradeRequest(requestData.getTargetId(), requestData.getRequesterId(), tradeId);
    }

    private static void handleTradeAccept(String[] parts, Jedis jedis) {
        // parts[0] = "trade_accept", parts[1] = dataKey, parts[2] = tradeId, parts[last] = responseChannel
        String dataKey = parts[1];
        String tradeId = parts[2];
        TradeRequest request = pendingTrades.get(tradeId);
        if (request == null) return;

        // Get requester's Pokémon
        Pokemon requesterPokemon = getPlayerPokemonFromSlot(
                request.getRequesterServer(),
                request.getRequesterId(),
                request.getRequesterPokemonSlot()
        );

        // Get target's Pokémon from Redis
        String targetPokemonData = jedis.get(dataKey);
        Pokemon targetPokemon = deserializePokemon(targetPokemonData);

        // Store requester's Pokémon in Redis for the target server
        String requesterPokemonKey = "pokemontrade:response:" + tradeId;
        jedis.setex(requesterPokemonKey, 60, serializePokemon(requesterPokemon));

        // Execute trade on this side
        removePokemonFromPlayer(request.getRequesterId(), request.getRequesterPokemonSlot());
        addPokemonToPlayer(request.getRequesterId(), targetPokemon);

        // Respond with completion
        String responseChannel = parts[parts.length - 1];
        jedis.publish(responseChannel, "COMPLETE|" + requesterPokemonKey);

        // Notify player
        notifyTradeComplete(request.getRequesterId());

        // Remove pending trade
        pendingTrades.remove(tradeId);
    }

    private static void handleTradeDeny(String[] parts) {
        // parts[0] = "trade_deny", parts[1] = tradeId
        String tradeId = parts[1];
        TradeRequest request = pendingTrades.get(tradeId);
        if (request == null) return;

        unlockPokemon(request.getRequesterId(), request.getRequesterPokemonSlot());

        notifyTradeDenied(request.getRequesterId(), request.getTargetId());

        // Remove pending trade
        pendingTrades.remove(tradeId);
    }

    // Placeholder implementations
    private static Pokemon getPlayerPokemonFromSlot(String server, String playerId, int slot) {
        return new Pokemon();
    }
    private static boolean canTradePokemon(Pokemon pokemon) {
        return true;
    }

    private static void lockPokemon(String server, String playerId, int slot) {}
    private static void unlockPokemon(String playerId, int slot) {}

    private static void scheduleTradeTimeout(String tradeId) {}
    private static void executeTrade(String requesterId, int requesterSlot, String targetId, int targetSlot) {}

    private static void notifyPlayerOfTradeRequest(String targetId, String requesterId, String tradeId) {}
    private static void notifyTradeComplete(String playerId) {
        notifyTradeComplete(playerId, null);
    }
    private static void notifyTradeComplete(String playerId, String targetId) {}
    private static void notifyTradeFailed(String playerId) {}
    private static void notifyTradeDenied(String requesterId, String targetId) {}

    private static String serializePokemon(Pokemon pokemon) {
        return "";
    }
    private static Pokemon deserializePokemon(String data) {
        return new Pokemon();
    }

    private static void removePokemonFromPlayer(String playerId, int slot) {}
    private static void addPokemonToPlayer(String playerId, Pokemon pokemon) {}
}