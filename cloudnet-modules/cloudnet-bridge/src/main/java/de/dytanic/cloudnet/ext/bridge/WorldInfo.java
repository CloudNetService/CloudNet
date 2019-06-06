package de.dytanic.cloudnet.ext.bridge;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Map;
import java.util.UUID;

@ToString
@EqualsAndHashCode
public class WorldInfo {

    protected UUID uniqueId;

    protected String name;

    protected String difficulty;

    protected Map<String, String> gameRules;

    public WorldInfo(UUID uniqueId, String name, String difficulty, Map<String, String> gameRules) {
        this.uniqueId = uniqueId;
        this.name = name;
        this.difficulty = difficulty;
        this.gameRules = gameRules;
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public String getName() {
        return this.name;
    }

    public String getDifficulty() {
        return this.difficulty;
    }

    public Map<String, String> getGameRules() {
        return this.gameRules;
    }

    public void setUniqueId(UUID uniqueId) {
        this.uniqueId = uniqueId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public void setGameRules(Map<String, String> gameRules) {
        this.gameRules = gameRules;
    }

}