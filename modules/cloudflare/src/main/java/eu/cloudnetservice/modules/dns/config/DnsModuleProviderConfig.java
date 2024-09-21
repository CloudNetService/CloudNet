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
import eu.cloudnetservice.common.Named;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.property.DefaultedDocPropertyHolder;
import eu.cloudnetservice.modules.dns.provider.DnsProviderZoneConfig;
import java.time.Duration;
import java.util.function.Consumer;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.Contract;

/**
 * A configuration for the provider that is associated with a specific configuration entry.
 *
 * @since 4.0
 */
@ToString
@EqualsAndHashCode
@SuppressWarnings("ClassCanBeRecord") // we want the sealed constructor
public final class DnsModuleProviderConfig implements DefaultedDocPropertyHolder, Named {

  private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 5;
  private static final int DEFAULT_REQUEST_TIMEOUT_SECONDS = 15;

  private final String name;
  private final Document properties;

  private final int connectTimeoutSeconds;
  private final int requestTimeoutSeconds;

  /**
   * Constructs a new dns provider config instance.
   *
   * @param name                  the name of the provider, will be resolved using the service registry.
   * @param properties            the provider-specific properties that will be passed during zone provider construct.
   * @param connectTimeoutSeconds the connect timeout (in seconds) that should be used when sending api requests.
   * @param requestTimeoutSeconds the request timeout (in seconds) that should be used when sending api requests.
   * @throws NullPointerException if the given name or properties document is null.
   */
  private DnsModuleProviderConfig(
    @NonNull String name,
    @NonNull Document properties,
    int connectTimeoutSeconds,
    int requestTimeoutSeconds
  ) {
    this.name = name;
    this.properties = properties;
    this.connectTimeoutSeconds = connectTimeoutSeconds;
    this.requestTimeoutSeconds = requestTimeoutSeconds;
  }

  /**
   * Constructs a new builder instance for provider configurations.
   *
   * @return a newly constructed dns module provider configuration builder instance.
   */
  @Contract(value = " -> new", pure = true)
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Constructs a new builder instance for dns module provider configurations, copying the settings from the given
   * provider configuration into the builder.
   *
   * @param providerConfig the provider config of which the settings should be copied into the builder.
   * @return the newly created builder instance with all values set to the values from the given provider config.
   * @throws NullPointerException if the given provider config is null.
   */
  @Contract("_ -> new")
  public static @NonNull Builder builder(@NonNull DnsModuleProviderConfig providerConfig) {
    return builder()
      .name(providerConfig.name())
      .properties(providerConfig.propertyHolder().mutableCopy())
      .connectTimeout(providerConfig.connectTimeout())
      .requestTimeout(providerConfig.requestTimeout());
  }

  /**
   * Converts this configuration into a dns provider zone config which can be used to construct a dns zone provider
   * using a dns provider.
   *
   * @return a new dns provider zone config based on the properties of this configuration.
   */
  @Contract(value = " -> new", pure = true)
  public @NonNull DnsProviderZoneConfig toProviderZoneConfig() {
    return new DnsProviderZoneConfig(this.connectTimeout(), this.requestTimeout(), this.properties);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String name() {
    return this.name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Document propertyHolder() {
    return this.properties;
  }

  /**
   * Get the connect timeout that should be used when sending api requests.
   *
   * @return the connect timeout that should be used when sending api requests.
   */
  public @NonNull Duration connectTimeout() {
    return Duration.ofSeconds(this.connectTimeoutSeconds);
  }

  /**
   * Get the request timeout that should be used when sending api requests.
   *
   * @return the request timeout that should be used when sending api requests.
   */
  public @NonNull Duration requestTimeout() {
    return Duration.ofSeconds(this.requestTimeoutSeconds);
  }

  /**
   * A builder for dns module provider configs.
   *
   * @since 4.0
   */
  public static final class Builder {

    private String name;
    private Document.Mutable properties = Document.newJsonDocument();

    private int connectTimeoutSeconds = DEFAULT_CONNECT_TIMEOUT_SECONDS;
    private int requestTimeoutSeconds = DEFAULT_REQUEST_TIMEOUT_SECONDS;

    /**
     * Sealed constructor to prevent direct instantiation. Use {@link DnsModuleProviderConfig#builder()} instead.
     */
    private Builder() {
    }

    /**
     * Sets the name of the dns provider which is configured by the final configuration.
     *
     * @param name the name of the dns provider being configured.
     * @return this builder, for chaining.
     * @throws NullPointerException if the given name is null.
     */
    @Contract("_ -> this")
    public @NonNull Builder name(@NonNull String name) {
      this.name = name;
      return this;
    }

    /**
     * Sets the properties of the dns provider. This usually includes the configuration (such as api keys, dns zone id,
     * etc.). The properties are later passed to the provider to construct zone providers.
     *
     * @param properties the properties for the dns provider.
     * @return this builder, for chaining.
     * @throws NullPointerException if the given properties document is null.
     */
    @Contract("_ -> this")
    public @NonNull Builder properties(@NonNull Document.Mutable properties) {
      this.properties = properties.mutableCopy();
      return this;
    }

    /**
     * Modifies the properties that are stored in this builder, for example to add new properties.
     *
     * @param modifier the modifier function to call for the modification.
     * @return this builder, for chaining.
     * @throws NullPointerException if the given modifier action is null.
     */
    @Contract("_ -> this")
    public @NonNull Builder modifyProperties(@NonNull Consumer<Document.Mutable> modifier) {
      modifier.accept(this.properties);
      return this;
    }

    /**
     * Sets the connect timeout to use for api requests. The timeout is stored in seconds, so the duration must at least
     * contain one second of timeout. Defaults to 5 seconds.
     *
     * @param connectTimeout the connect timeout for api requests.
     * @return this builder, for chaining.
     * @throws NullPointerException     if the given connect timeout duration is null.
     * @throws IllegalArgumentException if the given connect timeout is negative or zero.
     * @throws ArithmeticException      if the seconds stored in the given duration exceed the positive int bound.
     */
    @Contract("_ -> this")
    public @NonNull Builder connectTimeout(@NonNull Duration connectTimeout) {
      // as we store seconds we need to validate that the seconds are positive
      // else one could provide a duration with 0 seconds and a few nanos
      var cts = connectTimeout.toSeconds();
      Preconditions.checkArgument(cts > 0, "connect timeout must be positive");
      this.connectTimeoutSeconds = Math.toIntExact(cts);
      return this;
    }

    /**
     * Sets the request timeout to use for api requests. The timeout is stored in seconds, so the duration must at least
     * contain one second of timeout. Defaults to 15 seconds.
     *
     * @param requestTimeout the request timeout for api requests.
     * @return this builder, for chaining.
     * @throws NullPointerException     if the given request timeout duration is null.
     * @throws IllegalArgumentException if the given request timeout is negative or zero.
     * @throws ArithmeticException      if the seconds stored in the given duration exceed the positive int bound.
     */
    @Contract("_ -> this")
    public @NonNull Builder requestTimeout(@NonNull Duration requestTimeout) {
      // as we store seconds we need to validate that the seconds are positive
      // else one could provide a duration with 0 seconds and a few nanos
      var rts = requestTimeout.toSeconds();
      Preconditions.checkArgument(rts > 0, "request timeout must be positive");
      this.requestTimeoutSeconds = Math.toIntExact(rts);
      return this;
    }

    /**
     * Constructs a new dns provider config instance based on the settings of this builder.
     *
     * @return a newly constructed dns module provider config based on this builder.
     * @throws NullPointerException if no provider name was given to this builder.
     */
    @Contract(" -> new")
    public @NonNull DnsModuleProviderConfig build() {
      Preconditions.checkNotNull(this.name, "name");
      return new DnsModuleProviderConfig(
        this.name,
        this.properties.immutableCopy(),
        this.connectTimeoutSeconds,
        this.requestTimeoutSeconds);
    }
  }
}
