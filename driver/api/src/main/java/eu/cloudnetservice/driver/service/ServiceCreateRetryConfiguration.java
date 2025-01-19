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

package eu.cloudnetservice.driver.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import eu.cloudnetservice.driver.channel.ChannelMessageTarget;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.Unmodifiable;

/**
 * A configuration applied when the initial creation of a service fails. The caller has the ability to specify how the
 * retry of the service creation should be done. Additionally, if the service creation was deferred the further state
 * changes will be published to the configured event receivers.
 *
 * @since 4.0
 */
@ToString
@EqualsAndHashCode
public class ServiceCreateRetryConfiguration implements Cloneable {

  /**
   * A jvm static retry configuration which will not retry failed service creations.
   */
  public static final ServiceCreateRetryConfiguration NO_RETRY = new ServiceCreateRetryConfiguration(
    0,
    List.of(),
    Map.of());

  private final int maxRetries;
  private final List<Long> backoffStrategy;
  private final Map<ServiceCreateResult.State, List<ChannelMessageTarget>> eventReceivers;

  /**
   * Constructs a new retry configuration instance.
   *
   * @param maxRetries      the maximum amount of retries for the service creation.
   * @param backoffStrategy the strategy to apply to delay the service creation based on executed times.
   * @param eventReceivers  the receivers of state events if the creation was deferred.
   * @throws NullPointerException if the given backoff strategy or event receivers are null.
   */
  protected ServiceCreateRetryConfiguration(
    int maxRetries,
    @NonNull List<Long> backoffStrategy,
    @NonNull Map<ServiceCreateResult.State, List<ChannelMessageTarget>> eventReceivers
  ) {
    this.maxRetries = maxRetries;
    this.backoffStrategy = backoffStrategy;
    this.eventReceivers = eventReceivers;
  }

  /**
   * Constructs a new builder instance for a retry configuration.
   *
   * @return a new retry configuration builder.
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Creates a new retry configuration builder which has the same options set as the given retry configuration. Changes
   * made to the given configuration will not reflect into the new builder and vice-versa.
   * <p>
   * When calling build directly after creating the builder based on the given retry configuration it will return a
   * retry configuration which is equal to the given one, but not identical.
   *
   * @param retryConfiguration the configuration to copy the set options of.
   * @return a new builder instance which has the same options set as the given configuration.
   * @throws NullPointerException if the given configuration is null.
   */
  public static @NonNull Builder builder(@NonNull ServiceCreateRetryConfiguration retryConfiguration) {
    return builder()
      .maxRetries(retryConfiguration.maxRetries())
      .notificationReceivers(retryConfiguration.eventReceivers())
      .backoffStrategy(retryConfiguration.backoffStrategy().stream().map(Duration::ofMillis).toList());
  }

  /**
   * Checks if this configuration is enabled, in other words if there is at least one retry and delay specified.
   *
   * @return if this configuration is enabled.
   */
  public boolean enabled() {
    return this.maxRetries > 0 && !this.backoffStrategy.isEmpty();
  }

  /**
   * Get the maximum number of retries until the retry process should stop. Negative values or zero indicate that no
   * retry attempts should be made.
   *
   * @return the maximum number of retries.
   */
  public int maxRetries() {
    return this.maxRetries;
  }

  /**
   * Get an unmodifiable copy of the backoff strategy which should be applied when retrying the service creation. An
   * empty list indicates that no retry attempts should be made.
   *
   * @return the backoff strategy which should be applied when retrying the service creation.
   */
  @Unmodifiable
  public @NonNull List<Long> backoffStrategy() {
    return this.backoffStrategy;
  }

  /**
   * Get an unmodifiable copy of the event receivers which should get notified about state change events if the service
   * creation was deferred. The listeners won't be called if the service creation succeeded or failed without any
   * retries.
   *
   * @return the event receivers for deferred service state changes.
   */
  @Unmodifiable
  public @NonNull Map<ServiceCreateResult.State, List<ChannelMessageTarget>> eventReceivers() {
    return this.eventReceivers;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ServiceCreateRetryConfiguration clone() {
    try {
      return (ServiceCreateRetryConfiguration) super.clone();
    } catch (CloneNotSupportedException exception) {
      throw new IllegalStateException(); // cannot happen - just explode
    }
  }

  /**
   * Represents a builder for a creation retry configuration.
   *
   * @since 4.0
   */
  public static final class Builder {

    private int maxRetries = 1;
    private List<Duration> backoffStrategy = Lists.newArrayList(Duration.ofSeconds(15));
    private Map<ServiceCreateResult.State, List<ChannelMessageTarget>> eventReceivers = new HashMap<>();

    /**
     * Sets the maximum amount of times to retry the service creation. Only numbers higher than zero are accepted by the
     * method.
     * <p>
     * If you want zero retries (or in other words no retries) use {@link ServiceCreateRetryConfiguration#NO_RETRY}
     * instead.
     *
     * @param maxRetries the maximum amount of retries to do before marking the creation as failed.
     * @return the same instance as used to call the method, for chaining.
     * @throws IllegalArgumentException if the given max retries are lower or equal to zero.
     */
    public @NonNull Builder maxRetries(@Range(from = 1, to = Integer.MAX_VALUE) int maxRetries) {
      Preconditions.checkArgument(maxRetries > 0, "Max retries must be > 0");
      this.maxRetries = maxRetries;
      return this;
    }

    /**
     * Sets the delay between each service creation retry to the given duration.
     *
     * @param delay the fixed delay between each service creation retry.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given retry duration is null.
     */
    public @NonNull Builder fixedRetryDelay(@NonNull Duration delay) {
      this.backoffStrategy = Lists.newArrayList(delay);
      return this;
    }

    /**
     * Sets the backoff strategy when retrying a service creation. Each index in the given collection will be used for
     * the retry delay, the first one as the initial delay before the first retry. If the retry count exceeds the
     * entries in the given collection, a fixed retry delay based on the last index will be used for further retries.
     *
     * @param backoffStrategy the backoff strategy to apply when retrying a service creation.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given backoff strategy is null.
     */
    public @NonNull Builder backoffStrategy(@NonNull Collection<Duration> backoffStrategy) {
      this.backoffStrategy = new ArrayList<>(backoffStrategy);
      return this;
    }

    /**
     * Modifies the backoff strategy which is currently applied to this builder.
     *
     * @param modifier the modifier to apply to the already added backoff entries of this builder.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given modifier function is null.
     */
    public @NonNull Builder modifyBackoffStrategy(@NonNull Consumer<List<Duration>> modifier) {
      modifier.accept(this.backoffStrategy);
      return this;
    }

    /**
     * Sets the receivers of state change events when a service creation which was deferred either succeeded or failed.
     * The given targets will be notified about both these events. Already added receivers for one of the states will
     * get overridden.
     * <p>
     * The event will not be called on the receivers if no retry was needed, therefore the service either got created
     * instantly or the creation failed because no retry was specified.
     *
     * @param receivers the receivers for state event changes of deferred service creations.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given receivers are null.
     */
    public @NonNull Builder notificationReceivers(@NonNull ChannelMessageTarget... receivers) {
      return this
        .notificationReceivers(ServiceCreateResult.State.CREATED, receivers)
        .notificationReceivers(ServiceCreateResult.State.FAILED, receivers);
    }

    /**
     * Sets the receivers of state change events when a service creation which was deferred reached the given target
     * state of the service creation. Already added receivers for the same state will get overridden.
     * <p>
     * The event will not be called on the receivers if no retry was needed, therefore the service either got created
     * instantly or the creation failed because no retry was specified.
     *
     * @param state     the state in which the deferred service creation must run in order to call the receivers.
     * @param receivers the receivers for state event changes of deferred service creations.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given state or receivers are null.
     */
    public @NonNull Builder notificationReceivers(
      @NonNull ServiceCreateResult.State state,
      @NonNull ChannelMessageTarget... receivers
    ) {
      this.eventReceivers.put(state, Arrays.asList(receivers));
      return this;
    }

    /**
     * Sets the event receivers for state changes. These are mapped in a state-to-receivers relation, meaning that all
     * receivers for key state will be called when a deferred creation results in the state. Adding receivers for the
     * {@code DEFERRED} state is acceptable but will be without effect.
     * <p>
     * The event will not be called on the receivers if no retry was needed, therefore the service either got created
     * instantly or the creation failed because no retry was specified.
     *
     * @param eventReceivers the receivers mapping for state changes of deferred services.
     * @return the same instance as used to call the method, for chaining.
     * @throws NullPointerException if the given receiver mapping is null.
     */
    public @NonNull Builder notificationReceivers(
      @NonNull Map<ServiceCreateResult.State, List<ChannelMessageTarget>> eventReceivers
    ) {
      this.eventReceivers = new HashMap<>(Map.copyOf(eventReceivers));
      return this;
    }

    /**
     * Builds a new retry configuration based on the previously supplied properties.
     *
     * @return a new retry configuration based on this builder.
     * @throws IllegalArgumentException if the backoff strategy has no entries.
     */
    public @NonNull ServiceCreateRetryConfiguration build() {
      Preconditions.checkArgument(!this.backoffStrategy.isEmpty(), "BackoffStrategy has no entries");
      return new ServiceCreateRetryConfiguration(
        this.maxRetries,
        this.backoffStrategy.stream().map(Duration::toMillis).toList(),
        Map.copyOf(this.eventReceivers));
    }
  }
}
