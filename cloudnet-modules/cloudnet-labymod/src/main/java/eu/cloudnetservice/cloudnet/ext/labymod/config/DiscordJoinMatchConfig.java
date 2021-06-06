package eu.cloudnetservice.cloudnet.ext.labymod.config;

import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.cloudnet.ext.labymod.LabyModUtils;
import java.util.Collection;

public class DiscordJoinMatchConfig {

  private boolean enabled;
  private Collection<String> excludedGroups;

  public DiscordJoinMatchConfig(boolean enabled, Collection<String> excludedGroups) {
    this.enabled = enabled;
    this.excludedGroups = excludedGroups;
  }

  public boolean isEnabled() {
    return this.enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Collection<String> getExcludedGroups() {
    return this.excludedGroups;
  }

  public void setExcludedGroups(Collection<String> excludedGroups) {
    this.excludedGroups = excludedGroups;
  }

  public boolean isExcluded(ServiceInfoSnapshot serviceInfoSnapshot) {
    return LabyModUtils.isExcluded(this.excludedGroups, serviceInfoSnapshot.getConfiguration().getGroups());
  }

}
