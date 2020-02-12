package eu.cloudnetservice.cloudnet.ext.npcs;


import de.dytanic.cloudnet.ext.bridge.WorldPosition;

import java.util.Set;

public class CloudNPC {

    private String displayName;

    private Set<NPCProfileProperty> profileProperties;

    private WorldPosition position;

    private boolean lookAtPlayer;

    private boolean imitatePlayer;

    public CloudNPC(String displayName, Set<NPCProfileProperty> profileProperties, WorldPosition position, boolean lookAtPlayer, boolean imitatePlayer) {
        this.displayName = displayName;
        this.profileProperties = profileProperties;
        this.position = position;
        this.lookAtPlayer = lookAtPlayer;
        this.imitatePlayer = imitatePlayer;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Set<NPCProfileProperty> getProfileProperties() {
        return profileProperties;
    }

    public void setProfileProperties(Set<NPCProfileProperty> profileProperties) {
        this.profileProperties = profileProperties;
    }

    public WorldPosition getPosition() {
        return position;
    }

    public void setPosition(WorldPosition position) {
        this.position = position;
    }

    public boolean isLookAtPlayer() {
        return lookAtPlayer;
    }

    public void setLookAtPlayer(boolean lookAtPlayer) {
        this.lookAtPlayer = lookAtPlayer;
    }

    public boolean isImitatePlayer() {
        return imitatePlayer;
    }

    public void setImitatePlayer(boolean imitatePlayer) {
        this.imitatePlayer = imitatePlayer;
    }

    public static class NPCProfileProperty {

        private String name, value, signature;

        public NPCProfileProperty(String name, String value, String signature) {
            this.name = name;
            this.value = value;
            this.signature = signature;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public String getSignature() {
            return signature;
        }

    }

}
