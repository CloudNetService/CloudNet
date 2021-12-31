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

package de.dytanic.cloudnet.wrapper.provider;

import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import de.dytanic.cloudnet.driver.provider.GroupConfigurationProvider;
import de.dytanic.cloudnet.driver.service.GroupConfiguration;
import de.dytanic.cloudnet.wrapper.Wrapper;
import de.dytanic.cloudnet.wrapper.network.listener.message.GroupChannelMessageListener;
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class WrapperGroupConfigurationProvider implements GroupConfigurationProvider {

  private final RPCSender rpcSender;

  public WrapperGroupConfigurationProvider(@NonNull Wrapper wrapper) {
    this.rpcSender = wrapper.rpcProviderFactory().providerForClass(
      wrapper.networkClient(),
      GroupConfigurationProvider.class);
    wrapper.eventManager().registerListener(new GroupChannelMessageListener(wrapper.eventManager()));
  }

  @Override
  public void reload() {
    this.rpcSender.invokeMethod("reload").fireAndForget();
  }

  @Override
  public @NonNull Collection<GroupConfiguration> groupConfigurations() {
    return this.rpcSender.invokeMethod("groupConfigurations").fireSync();
  }

  @Override
  public void groupConfigurations(@NonNull Collection<GroupConfiguration> groupConfigurations) {
    this.rpcSender.invokeMethod("groupConfigurations").fireSync();
  }

  @Override
  public @Nullable GroupConfiguration groupConfiguration(@NonNull String name) {
    return this.rpcSender.invokeMethod("groupConfiguration").fireSync();
  }

  @Override
  public boolean groupConfigurationPresent(@NonNull String name) {
    return this.rpcSender.invokeMethod("groupConfigurationPresent", name).fireSync();
  }

  @Override
  public void addGroupConfiguration(@NonNull GroupConfiguration groupConfiguration) {
    this.rpcSender.invokeMethod("addGroupConfiguration", groupConfiguration).fireAndForget();
  }

  @Override
  public void removeGroupConfigurationByName(@NonNull String name) {
    this.rpcSender.invokeMethod("removeGroupConfigurationByName", name).fireAndForget();
  }

  @Override
  public void removeGroupConfiguration(@NonNull GroupConfiguration groupConfiguration) {
    this.removeGroupConfigurationByName(groupConfiguration.name());
  }
}
