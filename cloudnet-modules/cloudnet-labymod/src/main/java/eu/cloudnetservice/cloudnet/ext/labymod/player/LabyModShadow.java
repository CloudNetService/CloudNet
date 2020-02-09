package eu.cloudnetservice.cloudnet.ext.labymod.player;

public class LabyModShadow {
    private boolean enabled;
    private int version;

    public LabyModShadow(boolean enabled, int version) {
        this.enabled = enabled;
        this.version = version;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public int getVersion() {
        return this.version;
    }
}
