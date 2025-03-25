package com.zero_delusions.dev_assignment.network;

import com.zero_delusions.dev_assignment.core.database.table.UserData;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.List;
import java.util.UUID;

class NetworkRequest {
    private final String targetServer;
    private final String payload;

    public NetworkRequest(String targetServer, String payload) {
        this.targetServer = targetServer;
        this.payload = payload;
    }

    public String sendAndWait(int timeout) {
        Jedis jedis = new JedisPool().getResource();
        // Unique response channel using UUID
        String responseChannel = "response:" + UUID.randomUUID();

        // Publish request to target server's channel with response channel
        jedis.publish(
                "server:" + targetServer + ":requests",
                payload + "|" + responseChannel
        );

        // Block and wait for response with timeout
        List<String> response = jedis.blpop(timeout, responseChannel);
        jedis.close();

        // Return response or timeout message
        return (response != null && response.size() >= 2)
                ? response.get(1)
                : "TIMEOUT";
    }
}

class NetworkBroadcast {
    private final String payload;

    public NetworkBroadcast(String payload) {
        this.payload = payload;
    }

    public void send() {
        Jedis jedis = new JedisPool().getResource();
        // Publish to broadcast channel for all servers
        jedis.publish("broadcast", payload);
        jedis.close();
    }
}

class NetworkEvent {
    private final String payload;

    public NetworkEvent(String payload) {
        this.payload = payload;
    }

    public void send() {
        Jedis jedis = new JedisPool().getResource();
        // Fire-and-forget event publication
        jedis.publish("events", payload);
        jedis.close();
    }
}

public final class PlayerDataSynchronizer {

    // ---- Server A (Initiator) ----
    public void transferUser(String userId, String targetServer) {
        Jedis redis = new JedisPool().getResource();
        try {
            // 1. Create a distributed lock
            redis.set("lock:" + userId, "locked", "NX", "EX", 30);

            // 2. Store serialized data
            String dataKey = "transfer:" + userId;
            redis.setex(dataKey, 60, serializeUserData(userId));

            // 3. Create and send network request
            NetworkRequest request = new NetworkRequest(targetServer, dataKey);

            // 4. Wait for acknowledgment
            String response = request.sendAndWait(20_000);

            if ("ACK".equals(response)) {
                // Success case: release lock and log event
                redis.del("lock:" + userId);
                new NetworkEvent("Transfer succeeded: " + userId).send();
            } else {
                // Failure case: broadcast cleanup and release lock
                new NetworkBroadcast("cleanup:" + dataKey).send();
                redis.del("lock:" + userId);
                new NetworkEvent("Transfer failed: " + userId).send();
            }
        } finally {
            redis.close();
        }
    }

    private String serializeUserData(String userId) {
        return "userData";
    }

    // ---- Server B (Receiver) ----
    public void setupRequestListener(String currentServerId) {
        Jedis jedis = new JedisPool().getResource();
        JedisPubSub pubSub = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                if (channel.equals("server:" + currentServerId + ":requests")) {
                    // Split message into components
                    String[] parts = message.split("\\|");
                    if (parts.length < 2) return;

                    String dataKey = parts[0];
                    String responseChannel = parts[1];

                    // Process the transfer request
                    String userData = jedis.get(dataKey);
                    deserialize(userData);
                    loadUserData(new UserData());

                    // Cleanup and acknowledge
                    jedis.del(dataKey);
                    jedis.publish(responseChannel, "ACK");
                }
            }
        };

        // Start listening in a dedicated thread
        new Thread(() -> {
            try {
                jedis.subscribe(pubSub, "server:" + currentServerId + ":requests");
            } finally {
                pubSub.unsubscribe();
                jedis.close();
            }
        }).start();
    }

    public void setupBroadcastListener() {
        Jedis jedis = new JedisPool().getResource();
        JedisPubSub pubSub = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                if (channel.equals("broadcast") && message.startsWith("cleanup:")) {
                    String dataKey = message.substring("cleanup:".length());
                    jedis.del(dataKey);
                }
            }
        };

        // Start broadcast listener thread
        new Thread(() -> {
            try {
                jedis.subscribe(pubSub, "broadcast");
            } finally {
                pubSub.unsubscribe();
                jedis.close();
            }
        }).start();
    }

    private UserData deserialize(String userData) {
        return new UserData();
    }

    private void loadUserData(UserData user) {
    }
}