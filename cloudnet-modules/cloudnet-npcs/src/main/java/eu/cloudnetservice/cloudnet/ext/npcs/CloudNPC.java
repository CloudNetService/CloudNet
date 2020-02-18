package eu.cloudnetservice.cloudnet.ext.npcs;


import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import lombok.EqualsAndHashCode;

import java.util.Set;
import java.util.UUID;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CloudNPC {

    @EqualsAndHashCode.Include
    private UUID uuid;

    private String displayName;

    private String infoLine;

    private Set<NPCProfileProperty> profileProperties;

    private WorldPosition position;

    private String targetGroup;

    private String itemInHand;

    private boolean lookAtPlayer;

    private boolean imitatePlayer;

    private transient int entityId;

    public CloudNPC(UUID uuid, String displayName, String infoLine, Set<NPCProfileProperty> profileProperties, WorldPosition position, String targetGroup, String itemInHand, boolean lookAtPlayer, boolean imitatePlayer) {
        this.uuid = uuid;
        this.displayName = displayName;
        this.infoLine = infoLine;
        this.profileProperties = profileProperties;
        this.position = position;
        this.targetGroup = targetGroup;
        this.itemInHand = itemInHand;
        this.lookAtPlayer = lookAtPlayer;
        this.imitatePlayer = imitatePlayer;
    }

    public UUID getUUID() {
        return uuid;
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getInfoLine() {
        return infoLine;
    }

    public void setInfoLine(String infoLine) {
        this.infoLine = infoLine;
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

    public String getTargetGroup() {
        return targetGroup;
    }

    public void setTargetGroup(String targetGroup) {
        this.targetGroup = targetGroup;
    }

    public String getItemInHand() {
        return itemInHand;
    }

    public void setItemInHand(String itemInHand) {
        this.itemInHand = itemInHand;
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

    public int getEntityId() {
        return entityId;
    }

    public void setEntityId(int entityId) {
        this.entityId = entityId;
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
