/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.plugins.luckperms;

import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.driver.service.ServiceId;
import eu.cloudnetservice.wrapper.configuration.WrapperConfiguration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.context.StaticContextCalculator;

@Singleton
public final class CloudNetContextCalculator implements StaticContextCalculator {

  private final ServiceId serviceId;
  private final ServiceConfiguration serviceConfiguration;

  @Inject
  public CloudNetContextCalculator(@NonNull WrapperConfiguration wrapperConfiguration) {
    this.serviceConfiguration = wrapperConfiguration.serviceConfiguration();
    this.serviceId = this.serviceConfiguration.serviceId();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void calculate(@NonNull ContextConsumer consumer) {
    consumer.accept("service", this.serviceId.name());
    consumer.accept("service-uuid", this.serviceId.uniqueId().toString());
    consumer.accept("task", this.serviceId.taskName());
    consumer.accept("node", this.serviceId.nodeUniqueId());
    consumer.accept("environment", this.serviceId.environmentName());
    for (var group : this.serviceConfiguration.groups()) {
      consumer.accept("group", group);
    }

    // also add the server key to the context. the key is required as it's used when calculating
    // the location of the player within the network, not setting it results in the full context
    // set being used as the location, which can lead to overflows in db columns. however, this
    // is only applied when no server name is configured in the LuckPerms config already as we
    // should respect the user configured value.
    // https://github.com/LuckPerms/LuckPerms/blob/61ce546a041dff4c8e6c1ff6b9d33f4c2820bc85/common/src/main/java/me/lucko/luckperms/common/sender/Sender.java#L86
    var lpConfiguredServerName = LuckPermsProvider.get().getServerName();
    if (lpConfiguredServerName.equals("global")) {
      consumer.accept(DefaultContextKeys.SERVER_KEY, this.serviceId.name());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ContextSet estimatePotentialContexts() {
    var contexSetBuilder = ImmutableContextSet.builder();
    this.calculate(contexSetBuilder::add);
    return contexSetBuilder.build();
  }
}
