/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.labymod.config;

import eu.cloudnetservice.cloudnet.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.bridge.BridgeServiceHelper;
import org.jetbrains.annotations.Nullable;

public record LabyModServiceDisplay(boolean enabled, @Nullable String format) {

  public @Nullable String display(@Nullable ServiceInfoSnapshot serviceInfoSnapshot) {
    if (serviceInfoSnapshot == null || this.format == null || !this.enabled) {
      return null;
    } else {
      return BridgeServiceHelper.fillCommonPlaceholders(this.format, null, serviceInfoSnapshot);
    }
  }
}
