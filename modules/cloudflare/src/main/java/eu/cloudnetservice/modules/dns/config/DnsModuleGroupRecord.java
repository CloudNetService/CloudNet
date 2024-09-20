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
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.Contract;

/**
 * A configuration for SRV records to create for services that are matching the parent group configuration entry.
 *
 * @since 4.0
 */
@ToString
@EqualsAndHashCode
@SuppressWarnings("ClassCanBeRecord") // we want the sealed constructor
public final class DnsModuleGroupRecord {

  private static final int DEFAULT_TTL = 5 * 60;
  private static final int DEFAULT_PRIORITY = 1;

  private final String subdomain;

  private final int ttl;
  private final int priority;
  private final int weight;

  /**
   * Constructs a new group record configuration instance.
   *
   * @param subdomain the subdomain to create for matching services.
   * @param ttl       the ttl to use for the subdomain.
   * @param priority  the priority to use for the subdomain.
   * @param weight    the weight to use for the subdomain.
   * @throws NullPointerException if the given subdomain is null.
   */
  private DnsModuleGroupRecord(@NonNull String subdomain, int ttl, int priority, int weight) {
    this.subdomain = subdomain;
    this.ttl = ttl;
    this.priority = priority;
    this.weight = weight;
  }

  /**
   * Constructs a new builder instance for dns module group records.
   *
   * @return the newly created builder instance.
   */
  @Contract(value = " -> new", pure = true)
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Constructs a new builder instance for dns module group records, copying the settings from the given group record
   * into the builder.
   *
   * @param record the record of which the settings should be copied into the builder.
   * @return the newly created builder instance with all values set to the values from the given record instance.
   * @throws NullPointerException if the given record instance is null.
   */
  @Contract("_ -> new")
  public static @NonNull Builder builder(@NonNull DnsModuleGroupRecord record) {
    return builder()
      .subdomain(record.subdomain())
      .ttl(record.ttl())
      .priority(record.priority())
      .weight(record.weight());
  }

  /**
   * Get the subdomain name that should be used when creating a record based on this configuration.
   *
   * @return the subdomain name of records that are created based on this configuration.
   */
  public @NonNull String subdomain() {
    return this.subdomain;
  }

  /**
   * Get the TTL for records that are created based on this configuration. Providers can ignore this setting if they
   * don't support setting the TTL per record.
   *
   * @return the TTL for records that are created based on this configuration.
   */
  public int ttl() {
    return this.ttl;
  }

  /**
   * Get the priority for records that are created based on this configuration.
   *
   * @return the priority for records that are created based on this configuration.
   */
  public int priority() {
    return this.priority;
  }

  /**
   * Get the weight for records that are created based on this configuration.
   *
   * @return the weight for records that are created based on this configuration.
   */
  public int weight() {
    return this.weight;
  }

  /**
   * A builder for dns module group records.
   *
   * @since 4.0
   */
  public static final class Builder {

    private String subdomain;

    private int ttl = DEFAULT_TTL;
    private int priority = DEFAULT_PRIORITY;
    private int weight = DEFAULT_PRIORITY;

    /**
     * Sealed constructor to prevent direct instantiation. Use {@link DnsModuleGroupRecord#builder()} instead.
     */
    private Builder() {
    }

    /**
     * Sets the subdomain that should be created for services that are matching the associated group configuration.
     *
     * @param subdomain the subdomain name to create.
     * @return this builder, for chaining.
     * @throws NullPointerException if the given subdomain name is null.
     */
    @Contract("_ -> this")
    public @NonNull Builder subdomain(@NonNull String subdomain) {
      this.subdomain = subdomain;
      return this;
    }

    /**
     * Sets the TTL of the created subdomain. The ttl must be a positive number and can be ignored by providers when
     * they don't support specifying the ttl per subdomain.
     *
     * @param ttl the ttl to use when creating subdomains based on this configuration.
     * @return this builder, for chaining.
     * @throws IllegalArgumentException if the given ttl is less than 0.
     */
    @Contract("_ -> this")
    public @NonNull Builder ttl(int ttl) {
      Preconditions.checkArgument(ttl >= 0, "ttl cannot be negative");
      this.ttl = ttl;
      return this;
    }

    /**
     * Sets the priority of the created subdomain. Entries with a lower priority will be picked first. The given value
     * cannot be negative.
     *
     * @param priority the priority to use when creating subdomains based on this configuration.
     * @return this builder, for chaining.
     * @throws IllegalArgumentException if the given priority is negative.
     */
    @Contract("_ -> this")
    public @NonNull Builder priority(int priority) {
      Preconditions.checkArgument(priority >= 0, "priority cannot be negative");
      this.priority = priority;
      return this;
    }

    /**
     * Sets the weight of the created subdomain. The weight basically represents the priority of the subdomain relative
     * to other subdomains with the same priority setting. A higher weight will be picked first over a lower weight. The
     * given value cannot be negative.
     *
     * @param weight the weight to use when creating subdomains based on this configuration.
     * @return this builder, for chaining.
     * @throws IllegalArgumentException if the given weight is negative.
     */
    @Contract("_ -> this")
    public @NonNull Builder weight(int weight) {
      Preconditions.checkArgument(weight >= 0, "weight cannot be negative");
      this.weight = weight;
      return this;
    }

    /**
     * Constructs a new group record instance based on the settings of this builder.
     *
     * @return the newly created group record instance.
     * @throws NullPointerException if the subdomain of the group record hasn't been specified.
     */
    @Contract(" -> new")
    public @NonNull DnsModuleGroupRecord build() {
      Preconditions.checkNotNull(this.subdomain, "subdomain");
      return new DnsModuleGroupRecord(this.subdomain, this.ttl, this.priority, this.weight);
    }
  }
}
