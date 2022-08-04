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

@ToString
@EqualsAndHashCode(callSuper = false)
public class ServiceCreateRetryConfiguration implements Cloneable {

  public static final ServiceCreateRetryConfiguration NO_RETRY = new ServiceCreateRetryConfiguration(
    0,
    List.of(),
    Map.of());

  private final int maxRetries;
  private final List<Long> backoffStrategy;
  private final Map<ServiceCreateResult.State, List<ChannelMessageTarget>> eventReceivers;

  protected ServiceCreateRetryConfiguration(
    int maxRetries,
    @NonNull List<Long> backoffStrategy,
    @NonNull Map<ServiceCreateResult.State, List<ChannelMessageTarget>> eventReceivers
  ) {
    this.maxRetries = maxRetries;
    this.backoffStrategy = backoffStrategy;
    this.eventReceivers = eventReceivers;
  }

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public boolean enabled() {
    return this.maxRetries >= 0 && !this.backoffStrategy.isEmpty();
  }

  public int maxRetries() {
    return this.maxRetries;
  }

  public @NonNull List<Long> backoffStrategy() {
    return this.backoffStrategy;
  }

  public @NonNull Map<ServiceCreateResult.State, List<ChannelMessageTarget>> eventReceivers() {
    return this.eventReceivers;
  }

  @Override
  public ServiceCreateRetryConfiguration clone() {
    try {
      return (ServiceCreateRetryConfiguration) super.clone();
    } catch (CloneNotSupportedException exception) {
      throw new IllegalStateException(); // cannot happen - just explode
    }
  }

  public static final class Builder {

    private int maxRetries = 0;
    private List<Duration> backoffStrategy = Lists.newArrayList(Duration.ofSeconds(15), Duration.ofSeconds(30));
    private Map<ServiceCreateResult.State, List<ChannelMessageTarget>> eventReceivers = new HashMap<>();

    public @NonNull Builder maxRetries(int maxRetries) {
      this.maxRetries = maxRetries;
      return this;
    }

    public @NonNull Builder fixedRetryDelay(@NonNull Duration delay) {
      this.backoffStrategy = Lists.newArrayList(delay);
      return this;
    }

    public @NonNull Builder backoffStrategy(@NonNull Collection<Duration> backoffStrategy) {
      this.backoffStrategy = new ArrayList<>(backoffStrategy);
      return this;
    }

    public @NonNull Builder modifyBackoffStrategy(@NonNull Consumer<Collection<Duration>> modifier) {
      modifier.accept(this.backoffStrategy);
      return this;
    }

    public @NonNull Builder notificationReceivers(@NonNull ChannelMessageTarget... receivers) {
      return this
        .notificationReceivers(ServiceCreateResult.State.CREATED, receivers)
        .notificationReceivers(ServiceCreateResult.State.FAILED, receivers);
    }

    public @NonNull Builder notificationReceivers(
      @NonNull ServiceCreateResult.State state,
      @NonNull ChannelMessageTarget... receivers
    ) {
      this.eventReceivers.put(state, Arrays.asList(receivers));
      return this;
    }

    public @NonNull Builder notificationReceivers(
      @NonNull Map<ServiceCreateResult.State, List<ChannelMessageTarget>> eventReceivers
    ) {
      this.eventReceivers = new HashMap<>(Map.copyOf(eventReceivers));
      return this;
    }

    public @NonNull ServiceCreateRetryConfiguration build() {
      Preconditions.checkArgument(!this.backoffStrategy.isEmpty(), "BackoffStrategy has no entries");
      return new ServiceCreateRetryConfiguration(
        this.maxRetries,
        this.backoffStrategy.stream().map(Duration::toMillis).toList(),
        Map.copyOf(this.eventReceivers));
    }
  }
}
