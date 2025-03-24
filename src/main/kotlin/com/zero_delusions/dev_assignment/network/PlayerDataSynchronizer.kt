package com.zero_delusions.dev_assignment.network

import com.zero_delusions.dev_assignment.core.database.table.UserData
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPubSub
import java.util.UUID

class NetworkRequest(val targetServer: String, val payload: String) {
    fun sendAndWait(timeout: Int): String {
        val jedis = JedisPool().resource
        // Unique response channel
        val responseChannel = "response:${UUID.randomUUID()}"

        // Send request to Server B's channel
        jedis.publish(
            "server:$targetServer:requests",
            "$payload|$responseChannel"
        )

        // Block and wait for a response
        val response = jedis.blpop(timeout, responseChannel)
        jedis.close()

        return response?.component2() ?: "TIMEOUT"
    }
}

class NetworkBroadcast(val payload: String) {
    // Send a message to all servers (except self via filtering)
    fun send() {
        val jedis = JedisPool().resource
        jedis.publish("broadcast", payload) // All servers listen to "broadcast"
        jedis.close()
    }
}

class NetworkEvent(val payload: String) {
    // Fire-and-forget event (no response needed)
    fun send() {
        val jedis = JedisPool().resource
        jedis.publish("events", payload)
        jedis.close()
    }
}

/**
 * Redis player synchronization system pseudocode
 */
object PlayerDataSynchronizer {
    // ---- Server A (Initiator) ----
    fun transferUser(userId: String, targetServer: String) {
        val redis = JedisPool().resource
        // 1. Lock user data in Redis
        redis.set("lock:$userId", "locked", "NX", "EX", 30) // Lock for 30 seconds

        // 2. Serialize data and store in Redis
        val dataKey = "transfer:$userId"
        redis.setex(dataKey, 60, serializeUserData(userId))

        // 3. Send a NetworkRequest to Server B
        val request = NetworkRequest(
            targetServer = targetServer,
            payload = dataKey // Send the Redis key
        )

        // 4. Wait for acknowledgment
        when (val response = request.sendAndWait(timeout = 20_000)) {
            "ACK" -> {
                // Success: Unlock user and log event
                redis.del("lock:$userId")
                NetworkEvent("Transfer succeeded: $userId").send()
            }
            else -> {
                // Failure: Broadcast cleanup and unlock
                NetworkBroadcast("cleanup:$dataKey").send()
                redis.del("lock:$userId")
                NetworkEvent("Transfer failed: $userId").send()
            }
        }
    }

    fun serializeUserData(userId: String): String = "userData"

    // ---- Server B (Receiver) ----
    // Set up listener for incoming NetworkRequests
    fun setupRequestListener(currentServerId: String) {
        val jedis = JedisPool().resource
        val pubSub = object : JedisPubSub() {
            override fun onMessage(channel: String, message: String) {
                if (channel == "server:${currentServerId}:requests") {
                    // Split payload and response channel
                    val (dataKey, responseChannel) = message.split("|")

                    // Fetch data from Redis
                    val userData = deserialize(jedis.get(dataKey))
                    // Load data and send acknowledgment
                    loadUserData(userData)
                    jedis.del(dataKey)
                    jedis.publish(responseChannel, "ACK")
                }
            }
        }
        // Start listening in a background thread
        Thread { jedis.subscribe(pubSub, "server:${currentServerId}:requests") }.start()
    }

    // Set up listener for cleanup broadcasts
    // All servers listen to the "broadcast" channel
    fun setupBroadcastListener() {
        val jedis = JedisPool().resource
        val pubSub = object : JedisPubSub() {
            override fun onMessage(channel: String, message: String) {
                if (channel == "broadcast" && message.startsWith("cleanup:")) {
                    val dataKey = message.removePrefix("cleanup:")
                    jedis.del(dataKey) // Delete orphaned data
                }
            }
        }
        Thread { jedis.subscribe(pubSub, "broadcast") }.start()
    }

    fun deserialize(userId: String): UserData = UserData()
    fun loadUserData(user: UserData) {}
}