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
 * A configuration for a specific a group. Service records are constructed based on the group configuration that are
 * matching the service settings.
 *
 * @since 4.0
 */
@ToString
@EqualsAndHashCode
@SuppressWarnings("ClassCanBeRecord") // we want the sealed constructor
public final class DnsModuleGroupEntry {

  private final String targetGroup;

  private final String hostAddressV4;
  private final String hostAddressV6;

  private final List<DnsModuleGroupRecord> records;

  /**
   * Constructs a new dns module group configuration entry.
   *
   * @param targetGroup   the target group that is handled by this entry.
   * @param hostAddressV4 the host address (ipv4) that should be used as target for created SRV records.
   * @param hostAddressV6 the host address (ipv6) that should be used as target for created SRV records.
   * @param records       the SRV record configurations associated with this group entry.
   * @throws NullPointerException if the given target group or records list is null.
   */
  private DnsModuleGroupEntry(
    @NonNull String targetGroup,
    @Nullable String hostAddressV4,
    @Nullable String hostAddressV6,
    @NonNull List<DnsModuleGroupRecord> records
  ) {
    this.targetGroup = targetGroup;
    this.hostAddressV4 = hostAddressV4;
    this.hostAddressV6 = hostAddressV6;
    this.records = records;
  }

  /**
   * Constructs a new builder instance for module group entries.
   *
   * @return a new builder instance for module group entries.
   */
  @Contract(value = " -> new", pure = true)
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Constructs a new builder instance for module group entries, copying the settings from the given group configuration
   * entry into the builder.
   *
   * @param groupEntry the config entry of which the settings should be copied into the builder.
   * @return the newly created builder instance with all values set to the values from the given group config entry.
   * @throws NullPointerException if the group config entry is null.
   */
  @Contract("_ -> new")
  public static @NonNull Builder builder(@NonNull DnsModuleGroupEntry groupEntry) {
    return builder()
      .targetGroup(groupEntry.targetGroup())
      .hostAddressV4(groupEntry.hostAddressV4())
      .hostAddressV6(groupEntry.hostAddressV6())
      .records(groupEntry.records());
  }

  /**
   * Get the name of the target group of this group configuration entry.
   *
   * @return the name of the target group of this group configuration entry.
   */
  public @NonNull String targetGroup() {
    return this.targetGroup;
  }

  /**
   * Get the host address (ipv4) that should be used as target for the SRV records created for this configuration. If
   * null, the target ipv4 from the parent configuration entry will be used. This can either be an ip address or the
   * name of an ip alias defined in the node config.
   *
   * @return the host address (ipv4) that should be used as target for the SRV records created for this configuration.
   */
  public @Nullable String hostAddressV4() {
    return this.hostAddressV4;
  }

  /**
   * Get the host address (ipv6) that should be used as target for the SRV records created for this configuration. If
   * null, the target ipv6 from the parent configuration entry will be used. This can either be an ip address or the
   * name of an ip alias defined in the node config.
   *
   * @return the host address (ipv6) that should be used as target for the SRV records created for this configuration.
   */
  public @Nullable String hostAddressV6() {
    return this.hostAddressV6;
  }

  /**
   * Get the SRV record configurations that are associated with this group configuration. For each matching service a
   * record is created based on each entry in the records list.
   *
   * @return the SRV record configurations that are associated with this group configuration.
   */
  @Unmodifiable
  public @NonNull List<DnsModuleGroupRecord> records() {
    return records;
  }

  /**
   * A builder for group configuration entries.
   *
   * @since 4.0
   */
  public static final class Builder {

    private String targetGroup;

    private String hostAddressV4;
    private String hostAddressV6;

    private List<DnsModuleGroupRecord> records = new ArrayList<>();

    /**
     * Sealed constructor to prevent direct instantiation. Use {@link DnsModuleGroupEntry#builder()} instead.
     */
    private Builder() {
    }

    /**
     * Sets the name of the group that should be targeted by the constructed configuration.
     *
     * @param targetGroup the name of the configuration that should be targeted.
     * @return this builder, for chaining.
     * @throws NullPointerException if the given target group is null.
     */
    @Contract("_ -> this")
    public @NonNull Builder targetGroup(@NonNull String targetGroup) {
      this.targetGroup = targetGroup;
      return this;
    }

    /**
     * Sets the host address (ipv4) to use as a target for records created based on this configuration. If null, the
     * host address (ipv4) from the parent configuration will be used instead.
     *
     * @param hostAddressV4 the host address (ipv4) that will be used as target for created records.
     * @return this builder, for chaining.
     */
    @Contract("_ -> this")
    public @NonNull Builder hostAddressV4(@Nullable String hostAddressV4) {
      this.hostAddressV4 = hostAddressV4;
      return this;
    }

    /**
     * Sets the host address (ipv6) to use as a target for records created based on this configuration. If null, the
     * host address (ipv4) from the parent configuration will be used instead.
     *
     * @param hostAddressV6 the host address (ipv6) that will be used as target for created records.
     * @return this builder, for chaining.
     */
    @Contract("_ -> this")
    public @NonNull Builder hostAddressV6(@Nullable String hostAddressV6) {
      this.hostAddressV6 = hostAddressV6;
      return this;
    }

    /**
     * Sets the dns records that should be created for services that have the given target group assigned.
     *
     * @param records the records to create for matching services.
     * @return this builder, for chaining.
     * @throws NullPointerException if the given records list is null.
     */
    @Contract("_ -> this")
    public @NonNull Builder records(@NonNull List<DnsModuleGroupRecord> records) {
      this.records = new ArrayList<>(records);
      return this;
    }

    /**
     * Modifies the dns records that should be created for services that have the given target group assigned.
     *
     * @param modifier the modifier action to edit the list of records that should be created.
     * @return this builder, for chaining.
     * @throws NullPointerException if the given modifier action is null.
     */
    @Contract("_ -> this")
    public @NonNull Builder modifyRecords(@NonNull Consumer<List<DnsModuleGroupRecord>> modifier) {
      modifier.accept(this.records);
      return this;
    }

    /**
     * Constructs a new dns group configuration entry from this builder. The builder can be re-used after this operation
     * as further changes will not reflect into the constructed group entry.
     *
     * @return the newly created dns group configuration based on this builder.
     * @throws NullPointerException if not target group was specified.
     */
    @Contract(" -> new")
    public @NonNull DnsModuleGroupEntry build() {
      Preconditions.checkNotNull(this.targetGroup, "target group not provided");
      return new DnsModuleGroupEntry(
        this.targetGroup,
        this.hostAddressV4,
        this.hostAddressV6,
        List.copyOf(this.records));
    }
  }
}
