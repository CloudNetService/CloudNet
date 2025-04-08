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

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.Contract;
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
   * Constructs a new builder instance for dns module configuration entries.
   *
   * @return a new builder instance for dns module configuration entries.
   */
  @Contract(value = " -> new", pure = true)
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Constructs a new builder instance for dns module configuration entries, copying the settings from the given
   * configuration entry into the builder.
   *
   * @param configEntry the config entry of which the settings should be copied into the builder.
   * @return the newly created builder instance with all values set to the values from the given config entry.
   * @throws NullPointerException if the config entry is null.
   */
  @Contract("_ -> new")
  public static @NonNull Builder builder(@NonNull DnsModuleConfigEntry configEntry) {
    return builder()
      .enabled(configEntry.enabled())
      .domain(configEntry.domain())
      .domainNamespace(configEntry.domainNamespace())
      .hostAddressV4(configEntry.hostAddressV4())
      .hostAddressV6(configEntry.hostAddressV6())
      .groups(configEntry.groups())
      .providerConfig(configEntry.providerConfig());
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

  /**
   * A builder for dns module configuration entries.
   *
   * @since 4.0
   */
  public static final class Builder {

    private boolean enabled = true;

    private String domain;
    private String domainNamespace;

    private String hostAddressV4;
    private String hostAddressV6;

    private List<DnsModuleGroupEntry> groups = new ArrayList<>();
    private DnsModuleProviderConfig providerConfig;

    /**
     * Sealed constructor to prevent direct instantiation. Use {@link DnsModuleConfigEntry#builder()} instead.
     */
    private Builder() {
    }

    /**
     * Sets whether the dns module entry is enabled or not.
     *
     * @param enabled whether the DNS module should be enabled.
     * @return this builder, for chaining.
     */
    @Contract("_ -> this")
    public @NonNull Builder enabled(boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    /**
     * Sets the domain for the dns module configuration entry.
     *
     * @param domain the domain for the DNS configuration.
     * @return this builder, for chaining.
     * @throws NullPointerException if the given domain is null.
     */
    @Contract("_ -> this")
    public @NonNull Builder domain(@NonNull String domain) {
      this.domain = domain;
      return this;
    }

    /**
     * Sets the domain namespace for target records created for SRV records. Can be null to apply no namespace.
     *
     * @param domainNamespace the namespace of the domain.
     * @return this builder, for chaining.
     */
    @Contract(value = "_ -> this", pure = true)
    public @NonNull Builder domainNamespace(@Nullable String domainNamespace) {
      this.domainNamespace = domainNamespace;
      return this;
    }

    /**
     * Sets the host address (ipv4) to use for SRV target records (A-records). If null the bound address of the proxy is
     * used instead.
     *
     * @param hostAddressV4 the host address (ipv4) that will be used as target for created records.
     * @return this builder, for chaining.
     */
    @Contract(value = "_ -> this", pure = true)
    public @NonNull Builder hostAddressV4(@Nullable String hostAddressV4) {
      this.hostAddressV4 = hostAddressV4;
      return this;
    }

    /**
     * Sets the host address (ipv6) to use for SRV target records (AAAA-records). If null the bound address of the proxy
     * is used instead.
     *
     * @param hostAddressV6 the host address (ipv6) that will be used as target for created records.
     * @return this builder, for chaining.
     */
    @Contract(value = "_ -> this", pure = true)
    public @NonNull Builder hostAddressV6(@Nullable String hostAddressV6) {
      this.hostAddressV6 = hostAddressV6;
      return this;
    }

    /**
     * Sets the group entries for the dns module configuration entry.
     *
     * @param groups the group entries to set.
     * @return this builder, for chaining.
     * @throws NullPointerException if the given groups list is null.
     */
    @Contract("_ -> this")
    public @NonNull Builder groups(@NonNull List<DnsModuleGroupEntry> groups) {
      this.groups = new ArrayList<>(groups);
      return this;
    }

    /**
     * Modifies the group entries for the dns module configuration entry.
     *
     * @param modifier the modifier action to edit the list of groups.
     * @return this builder, for chaining.
     * @throws NullPointerException if the given modifier action is null.
     */
    @Contract("_ -> this")
    public @NonNull Builder modifyGroups(@NonNull Consumer<List<DnsModuleGroupEntry>> modifier) {
      modifier.accept(this.groups);
      return this;
    }

    /**
     * Sets the provider configuration for the DNS module.
     *
     * @param providerConfig the provider configuration.
     * @return this builder, for chaining.
     * @throws NullPointerException if the given provider configuration is null.
     */
    @Contract("_ -> this")
    public @NonNull Builder providerConfig(@NonNull DnsModuleProviderConfig providerConfig) {
      this.providerConfig = providerConfig;
      return this;
    }

    /**
     * Constructs a new DNS module configuration entry from this builder. The builder can be re-used after this
     * operation as further changes will not reflect into the constructed config entry.
     *
     * @return the newly created DNS module configuration entry.
     * @throws NullPointerException if no domain, domain namespace or provider config was provided.
     */
    @Contract(" -> new")
    public @NonNull DnsModuleConfigEntry build() {
      Preconditions.checkNotNull(this.domain, "domain not provided");
      Preconditions.checkNotNull(this.domainNamespace, "domain namespace not provided");
      Preconditions.checkNotNull(this.providerConfig, "provider configuration not provided");

      return new DnsModuleConfigEntry(
        this.enabled,
        this.domain,
        this.domainNamespace,
        this.hostAddressV4,
        this.hostAddressV6,
        List.copyOf(this.groups),
        this.providerConfig
      );
    }
  }
}
