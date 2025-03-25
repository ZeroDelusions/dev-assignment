package com.zero_delusions.dev_assignment.core.database.table;

import jakarta.persistence.*;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.UUID;

@Entity
@Table(name = "user_data")
public class UserData {

    @Id
    @Column(name = "uuid", columnDefinition = "BINARY(16)")
    private byte[] uuid;

    @Column(name = "armourInventory", columnDefinition = "LONGTEXT")
    private String armourInventory;

    @Column(name = "enderChest", columnDefinition = "LONGTEXT")
    private String enderChest;

    @Column(name = "exhaustion", columnDefinition = "FLOAT", nullable = false)
    private float exhaustion;

    @Column(name = "experienceLevel", columnDefinition = "INT", nullable = false)
    private int experienceLevel;

    @Column(name = "foodLevel", columnDefinition = "INT", nullable = false)
    private int foodLevel;

    @Column(name = "gamemode", columnDefinition = "INT")
    private Integer gamemode;

    @Column(name = "health", columnDefinition = "DOUBLE", nullable = false)
    private double health;

    @Column(name = "mainInventory", columnDefinition = "LONGTEXT")
    private String mainInventory;

    @Column(name = "offHandInventory", columnDefinition = "LONGTEXT")
    private String offHandInventory;

    @Column(name = "saturationLevel", columnDefinition = "FLOAT", nullable = false)
    private float saturationLevel;

    @Column(name = "totalExperience", columnDefinition = "INT", nullable = false)
    private int totalExperience;

    // Default constructor for JPA
    public UserData() {
    }

    // Getters and Setters
    public byte[] getUuid() {
        return uuid;
    }

    public void setUuid(byte[] uuid) {
        this.uuid = uuid;
    }

    public String getArmourInventory() {
        return armourInventory;
    }

    public void setArmourInventory(String armourInventory) {
        this.armourInventory = armourInventory;
    }

    public String getEnderChest() {
        return enderChest;
    }

    public void setEnderChest(String enderChest) {
        this.enderChest = enderChest;
    }

    public float getExhaustion() {
        return exhaustion;
    }

    public void setExhaustion(float exhaustion) {
        this.exhaustion = exhaustion;
    }

    public int getExperienceLevel() {
        return experienceLevel;
    }

    public void setExperienceLevel(int experienceLevel) {
        this.experienceLevel = experienceLevel;
    }

    public int getFoodLevel() {
        return foodLevel;
    }

    public void setFoodLevel(int foodLevel) {
        this.foodLevel = foodLevel;
    }

    public Integer getGamemode() {
        return gamemode;
    }

    public void setGamemode(Integer gamemode) {
        this.gamemode = gamemode;
    }

    public double getHealth() {
        return health;
    }

    public void setHealth(double health) {
        this.health = health;
    }

    public String getMainInventory() {
        return mainInventory;
    }

    public void setMainInventory(String mainInventory) {
        this.mainInventory = mainInventory;
    }

    public String getOffHandInventory() {
        return offHandInventory;
    }

    public void setOffHandInventory(String offHandInventory) {
        this.offHandInventory = offHandInventory;
    }

    public float getSaturationLevel() {
        return saturationLevel;
    }

    public void setSaturationLevel(float saturationLevel) {
        this.saturationLevel = saturationLevel;
    }

    public int getTotalExperience() {
        return totalExperience;
    }

    public void setTotalExperience(int totalExperience) {
        this.totalExperience = totalExperience;
    }

    // equals and hashCode based on uuid
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserData userData)) return false;
        return Arrays.equals(uuid, userData.uuid);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(uuid);
    }

    // Custom function to convert the uuid byte array into a Java UUID object
    public UUID getJavaUUID() {
        if (uuid != null) {
            ByteBuffer bb = ByteBuffer.wrap(uuid);
            long mostSignificantBits = bb.getLong();
            long leastSignificantBits = bb.getLong();
            return new UUID(mostSignificantBits, leastSignificantBits);
        }
        return null;
    }
}
