package de.dytanic.cloudnet.ext.bridge;

import java.util.Map;
import java.util.UUID;

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

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof WorldInfo)) return false;
        final WorldInfo other = (WorldInfo) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$uniqueId = this.getUniqueId();
        final Object other$uniqueId = other.getUniqueId();
        if (this$uniqueId == null ? other$uniqueId != null : !this$uniqueId.equals(other$uniqueId)) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final Object this$difficulty = this.getDifficulty();
        final Object other$difficulty = other.getDifficulty();
        if (this$difficulty == null ? other$difficulty != null : !this$difficulty.equals(other$difficulty))
            return false;
        final Object this$gameRules = this.getGameRules();
        final Object other$gameRules = other.getGameRules();
        if (this$gameRules == null ? other$gameRules != null : !this$gameRules.equals(other$gameRules)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof WorldInfo;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $uniqueId = this.getUniqueId();
        result = result * PRIME + ($uniqueId == null ? 43 : $uniqueId.hashCode());
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final Object $difficulty = this.getDifficulty();
        result = result * PRIME + ($difficulty == null ? 43 : $difficulty.hashCode());
        final Object $gameRules = this.getGameRules();
        result = result * PRIME + ($gameRules == null ? 43 : $gameRules.hashCode());
        return result;
    }

    public String toString() {
        return "WorldInfo(uniqueId=" + this.getUniqueId() + ", name=" + this.getName() + ", difficulty=" + this.getDifficulty() + ", gameRules=" + this.getGameRules() + ")";
    }
}