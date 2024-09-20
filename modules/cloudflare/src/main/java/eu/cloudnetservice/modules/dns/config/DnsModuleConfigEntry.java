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

import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

/**
 * A configuration entry of the module for a specific domain.
 *
 * @since 4.0
 */
@ToString
@EqualsAndHashCode
@SuppressWarnings("ClassCanBeRecord") // we want the sealed constructor
public final class DnsModuleConfigEntry {

  private final boolean enabled;

  private final String domain;
  private final String domainNamespace;

  private final String hostAddressV4;
  private final String hostAddressV6;

  private final List<DnsModuleGroupEntry> groups;
  private final DnsModuleProviderConfig providerConfig;

  /**
   * Constructs a new dns module entry instance.
   *
   * @param enabled         if the configuration entry is enabled.
   * @param domain          the name of the domain that is managed by this entry.
   * @param domainNamespace the namespace to apply to srv target records, can be null.
   * @param hostAddressV4   the host address (ipv4) to use as target for srv records.
   * @param hostAddressV6   the host address (ipv6) to use as target for srv records.
   * @param groups          the configurations for the groups that are managed by this configuration.
   * @param providerConfig  the configuration for the api of the provider that is managing the domain dns.
   * @throws NullPointerException if the given domain, groups or provider configuration is null.
   */
  private DnsModuleConfigEntry(
    boolean enabled,
    @NonNull String domain,
    @Nullable String domainNamespace,
    @Nullable String hostAddressV4,
    @Nullable String hostAddressV6,
    @NonNull List<DnsModuleGroupEntry> groups,
    @NonNull DnsModuleProviderConfig providerConfig
  ) {
    this.enabled = enabled;
    this.domain = domain;
    this.domainNamespace = domainNamespace;
    this.hostAddressV4 = hostAddressV4;
    this.hostAddressV6 = hostAddressV6;
    this.groups = groups;
    this.providerConfig = providerConfig;
  }

  /**
   * Get if this configuration entry is enabled and should be used.
   *
   * @return true if this configuration entry is enabled, false otherwise.
   */
  public boolean enabled() {
    return this.enabled;
  }

  /**
   * Get the domain name that is handled by this configuration entry.
   *
   * @return the domain name that is handled by this configuration entry.
   */
  public @NonNull String domain() {
    return this.domain;
  }

  /**
   * Get the namespace that should be applied to A/AAAA records (targets for the created srv records). Can be null or an
   * empty string to indicate that no namespace should be applied.
   *
   * @return the namespace that should be applied to A/AAAA records.
   */
  public @Nullable String domainNamespace() {
    return this.domainNamespace;
  }

  /**
   * Get the host address (ipv4) that should be used as target for the SRV records created for this configuration. If
   * null, the bound ip of the target proxy will be used instead. This can either be an ip address or the name of an ip
   * alias defined in the node config.
   *
   * @return the host address (ipv4) that should be used as target for the SRV records created for this configuration.
   */
  public @Nullable String hostAddressV4() {
    return this.hostAddressV4;
  }

  /**
   * Get the host address (ipv6) that should be used as target for the SRV records created for this configuration. If
   * null, the bound ip of the target proxy will be used instead. This can either be an ip address or the name of an ip
   * alias defined in the node config.
   *
   * @return the host address (ipv6) that should be used as target for the SRV records created for this configuration.
   */
  public @Nullable String hostAddressV6() {
    return this.hostAddressV6;
  }

  /**
   * Get the configurations for the groups that are managed by this configuration entry.
   *
   * @return the configurations for the groups that are managed by this configuration entry.
   */
  @Unmodifiable
  public @NonNull List<DnsModuleGroupEntry> groups() {
    return this.groups;
  }

  /**
   * Get the configuration to access the api of the provider that is managing the associated domain.
   *
   * @return the configuration to access the api of the provider that is managing the associated domain.
   */
  public @NonNull DnsModuleProviderConfig providerConfig() {
    return this.providerConfig;
  }
}
