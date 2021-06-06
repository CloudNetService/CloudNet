/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
