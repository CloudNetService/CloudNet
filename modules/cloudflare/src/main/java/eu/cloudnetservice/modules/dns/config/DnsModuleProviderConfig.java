/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.dns.config;

import eu.cloudnetservice.common.Named;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.modules.dns.provider.DnsProviderZoneConfig;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;

/**
 * A configuration for the provider that is associated with a specific configuration entry.
 *
 * @param name       the name of the provider, will be resolved using the service registry.
 * @param properties the provider-specific properties that will be passed when constructing a dns zone provider.
 * @since 4.0
 */
public record DnsModuleProviderConfig(@NonNull String name, @NonNull Document properties) implements Named {

  /**
   * Converts this configuration into a dns provider zone config which can be used to construct a dns zone provider
   * using a dns provider.
   *
   * @return a new dns provider zone config based on the properties of this configuration.
   */
  @Contract(value = " -> new", pure = true)
  public @NonNull DnsProviderZoneConfig toProviderZoneConfig() {
    return new DnsProviderZoneConfig(this.properties);
  }
}
