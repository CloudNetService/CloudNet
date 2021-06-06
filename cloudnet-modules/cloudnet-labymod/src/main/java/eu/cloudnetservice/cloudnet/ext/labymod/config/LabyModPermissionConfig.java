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
