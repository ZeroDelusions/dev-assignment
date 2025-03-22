package com.zero_delusions.dev_assignment.core.database.table

import jakarta.persistence.*

@Entity
@Table(name = "user_data")
data class UserData(
    @Id
    @Column(name = "uuid", columnDefinition = "BINARY(16)")
    var uuid: ByteArray? = null,

    @Column(name = "armourInventory", columnDefinition = "LONGTEXT")
    var armourInventory: String? = null,

    @Column(name = "enderChest", columnDefinition = "LONGTEXT")
    var enderChest: String? = null,

    @Column(name = "exhaustion", columnDefinition = "FLOAT", nullable = false)
    var exhaustion: Float = 0f,

    @Column(name = "experienceLevel", columnDefinition = "INT", nullable = false)
    var experienceLevel: Int = 0,

    @Column(name = "foodLevel", columnDefinition = "INT", nullable = false)
    var foodLevel: Int = 0,

    @Column(name = "gamemode", columnDefinition = "INT")
    var gamemode: Int? = null,

    @Column(name = "health", columnDefinition = "DOUBLE", nullable = false)
    var health: Double = 0.0,

    @Column(name = "mainInventory", columnDefinition = "LONGTEXT")
    var mainInventory: String? = null,

    @Column(name = "offHandInventory", columnDefinition = "LONGTEXT")
    var offHandInventory: String? = null,

    @Column(name = "saturationLevel", columnDefinition = "FLOAT", nullable = false)
    var saturationLevel: Float = 0f,

    @Column(name = "totalExperience", columnDefinition = "INT", nullable = false)
    var totalExperience: Int = 0,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UserData

        return uuid.contentEquals(other.uuid)
    }

    override fun hashCode(): Int {
        return uuid.contentHashCode()
    }
}