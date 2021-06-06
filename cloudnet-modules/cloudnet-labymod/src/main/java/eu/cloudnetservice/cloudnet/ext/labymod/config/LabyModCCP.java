package eu.cloudnetservice.cloudnet.ext.labymod.config;

public class LabyModCCP {

  private final boolean enabled;
  private final int version;

  public LabyModCCP(boolean enabled, int version) {
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
