/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.plugins.papi;

import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.modules.bridge.BridgeServiceHelper;
import eu.cloudnetservice.wrapper.holder.ServiceInfoHolder;
import lombok.NonNull;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

public class CloudNetPapiExpansion extends PlaceholderExpansion {

  private final ServiceInfoHolder serviceInfoHolder;

  public CloudNetPapiExpansion() {
    // not an ideal solution but should be enough in order to let this expansion
    // work without the need to wrap it in a plugin
    this.serviceInfoHolder = InjectionLayer.ext().instance(ServiceInfoHolder.class);
  }

  @Override
  public @NonNull String getIdentifier() {
    return "cloudnet";
  }

  @Override
  public @NonNull String getName() {
    return "CloudNet";
  }

  @Override
  public @NonNull String getAuthor() {
    return "CloudNetService";
  }

  @Override
  public @NonNull String getVersion() {
    return "@version@";
  }

  @Override
  public @Nullable String getRequiredPlugin() {
    return "CloudNet-Bridge";
  }

  @Override
  public @Nullable String onRequest(OfflinePlayer player, @NonNull String params) {
    // This is a bit tricky - PlaceholderAPI requires us to return "null" if the placeholder is unknown.
    // The bridge will just replace all placeholders in the string with the correct association.
    // We can just return null if the resulting string matches the input string
    var input = '%' + params + '%';
    var out = BridgeServiceHelper.fillCommonPlaceholders(input, null, this.serviceInfoHolder.serviceInfo());

    return out.equals(input) ? null : out;
  }
}
