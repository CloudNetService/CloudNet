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

package de.dytanic.cloudnet.ext.cloudperms.node.config;

import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class CloudPermissionConfig {

  public static final CloudPermissionConfig DEFAULT = new CloudPermissionConfig(true, Collections.emptyList());

  private final boolean enabled;
  private final List<String> excludedGroups;

  public CloudPermissionConfig(boolean enabled, List<String> excludedGroups) {
    this.enabled = enabled;
    this.excludedGroups = excludedGroups;
  }

  public boolean isEnabled() {
    return this.enabled;
  }

  public @NotNull List<String> getExcludedGroups() {
    return this.excludedGroups;
  }
}
