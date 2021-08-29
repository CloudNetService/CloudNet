/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.wrapper.provider;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import de.dytanic.cloudnet.driver.provider.GroupConfigurationProvider;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WrapperGroupConfigurationProvider implements GroupConfigurationProvider {

  private final RPCSender rpcSender;

  public WrapperGroupConfigurationProvider(Wrapper wrapper) {
    this.rpcSender = wrapper.getRPCProviderFactory()
      .providerForClass(wrapper.getNetworkClient(), GroupConfigurationProvider.class);
  }

  @Override
  public void reload() {
    this.rpcSender.invokeMethod("reload").fireAndForget();
  }

  @Override
  public Collection<GroupConfiguration> getGroupConfigurations() {
    return this.rpcSender.invokeMethod("getGroupConfigurations").fireSync();
  }

  @Override
  public void setGroupConfigurations(@NotNull Collection<GroupConfiguration> groupConfigurations) {
    Preconditions.checkNotNull(groupConfigurations);
    this.rpcSender.invokeMethod("setGroupConfigurations").fireSync();
  }

  @Nullable
  @Override
  public GroupConfiguration getGroupConfiguration(@NotNull String name) {
    Preconditions.checkNotNull(name);
    return this.rpcSender.invokeMethod("getGroupConfiguration").fireSync();
  }

  @Override
  public boolean isGroupConfigurationPresent(@NotNull String name) {
    Preconditions.checkNotNull(name);
    return this.rpcSender.invokeMethod("isGroupConfigurationPresent", name).fireSync();
  }

  @Override
  public void addGroupConfiguration(@NotNull GroupConfiguration groupConfiguration) {
    Preconditions.checkNotNull(groupConfiguration);
    this.rpcSender.invokeMethod("addGroupConfiguration", groupConfiguration).fireAndForget();
  }

  @Override
  public void removeGroupConfiguration(@NotNull String name) {
    Preconditions.checkNotNull(name);
    this.rpcSender.invokeMethod("removeGroupConfiguration", name).fireAndForget();
  }

  @Override
  public void removeGroupConfiguration(@NotNull GroupConfiguration groupConfiguration) {
    Preconditions.checkNotNull(groupConfiguration);
    this.removeGroupConfiguration(groupConfiguration.getName());
  }
}
