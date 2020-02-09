package eu.cloudnetservice.cloudnet.ext.labymod.player;

import java.util.Collection;
import java.util.UUID;

public class LabyModPlayerOptions {

    private String version;
    private LabyModCCP ccp;
    private LabyModShadow shadow;
    private Collection<LabyModAddon> addons;
    private Collection<LabyModMod> mods;
    private UUID joinSecret;
    private long lastJoinSecretRedeem = -1;

    public LabyModPlayerOptions(String version, LabyModCCP ccp, LabyModShadow shadow, Collection<LabyModAddon> addons, Collection<LabyModMod> mods) {
        this.version = version;
        this.ccp = ccp;
        this.shadow = shadow;
        this.addons = addons;
        this.mods = mods;
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
}
