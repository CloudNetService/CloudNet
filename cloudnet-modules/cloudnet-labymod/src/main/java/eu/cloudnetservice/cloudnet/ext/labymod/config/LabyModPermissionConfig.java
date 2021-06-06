package eu.cloudnetservice.cloudnet.ext.labymod.config;

import java.util.Map;

public class LabyModPermissionConfig {

  private boolean enabled;
  private Map<String, Boolean> labyModPermissions;

  public LabyModPermissionConfig(boolean enabled, Map<String, Boolean> labyModPermissions) {
    this.enabled = enabled;
    this.labyModPermissions = labyModPermissions;
  }

  public boolean isEnabled() {
    return this.enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Map<String, Boolean> getLabyModPermissions() {
    return this.labyModPermissions;
  }

  public void setLabyModPermissions(Map<String, Boolean> labyModPermissions) {
    this.labyModPermissions = labyModPermissions;
  }

}
