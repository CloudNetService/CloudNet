package eu.cloudnetservice.cloudnet.ext.labymod.player;

import eu.cloudnetservice.cloudnet.ext.labymod.config.LabyModAddon;
import eu.cloudnetservice.cloudnet.ext.labymod.config.LabyModCCP;
import eu.cloudnetservice.cloudnet.ext.labymod.config.LabyModMod;
import eu.cloudnetservice.cloudnet.ext.labymod.config.LabyModShadow;

import java.util.Collection;
import java.util.UUID;

public class LabyModPlayerOptions {

    private long creationTime;
    private final String version;
    private final LabyModCCP ccp;
    private final LabyModShadow shadow;
    private final Collection<LabyModAddon> addons;
    private final Collection<LabyModMod> mods;
    private UUID joinSecret;
    private long lastJoinSecretRedeem = -1;
    private UUID spectateSecret;
    private long lastSpectateSecretRedeem = -1;

    public LabyModPlayerOptions(String version, LabyModCCP ccp, LabyModShadow shadow, Collection<LabyModAddon> addons, Collection<LabyModMod> mods) {
        this.version = version;
        this.ccp = ccp;
        this.shadow = shadow;
        this.addons = addons;
        this.mods = mods;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public long getCreationTime() {
        return this.creationTime;
    }

    public String getVersion() {
        return this.version;
    }

    public LabyModCCP getCcp() {
        return this.ccp;
    }

    public LabyModShadow getShadow() {
        return this.shadow;
    }

    public Collection<LabyModAddon> getAddons() {
        return this.addons;
    }

    public Collection<LabyModMod> getMods() {
        return this.mods;
    }

    public boolean isValid() {
        return this.version != null && this.ccp != null && this.shadow != null && this.addons != null;
    }

    public UUID getJoinSecret() {
        return this.joinSecret;
    }

    public long getLastJoinSecretRedeem() {
        return this.lastJoinSecretRedeem;
    }

    public void setLastJoinSecretRedeem(long lastJoinSecretRedeem) {
        this.lastJoinSecretRedeem = lastJoinSecretRedeem;
    }

    public void removeJoinSecret() {
        this.joinSecret = null;
    }

    public void createNewJoinSecret() {
        this.joinSecret = UUID.randomUUID();
    }


    public UUID getSpectateSecret() {
        return this.spectateSecret;
    }

    public long getLastSpectateSecretRedeem() {
        return this.lastSpectateSecretRedeem;
    }

    public void setLastSpectateSecretRedeem(long lastSpectateSecretRedeem) {
        this.lastSpectateSecretRedeem = lastSpectateSecretRedeem;
    }

    public void removeSpectateSecret() {
        this.spectateSecret = null;
    }

    public void createNewSpectateSecret() {
        this.spectateSecret = UUID.randomUUID();
    }

}
