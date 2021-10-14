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
import de.dytanic.cloudnet.driver.provider.ServiceTaskProvider;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public class WrapperServiceTaskProvider implements ServiceTaskProvider {

  private final RPCSender rpcSender;

  public WrapperServiceTaskProvider(@NotNull Wrapper wrapper) {
    this.rpcSender = wrapper.getRPCProviderFactory().providerForClass(
      wrapper.getNetworkClient(),
      ServiceTaskProvider.class);
  }

  @Override
  public void reload() {
    this.rpcSender.invokeMethod("reload").fireSync();
  }

  @Override
  public @NotNull Collection<ServiceTask> getPermanentServiceTasks() {
    return this.rpcSender.invokeMethod("getPermanentServiceTasks").fireSync();
  }

  @Override
  public void setPermanentServiceTasks(@NotNull Collection<ServiceTask> serviceTasks) {
    Preconditions.checkNotNull(serviceTasks);
    this.rpcSender.invokeMethod("setPermanentServiceTasks", serviceTasks).fireSync();
  }

  @Override
  public ServiceTask getServiceTask(@NotNull String name) {
    Preconditions.checkNotNull(name);
    return this.rpcSender.invokeMethod("getServiceTask", name).fireSync();
  }

  @Override
  public boolean isServiceTaskPresent(@NotNull String name) {
    Preconditions.checkNotNull(name);
    return this.rpcSender.invokeMethod("isServiceTaskPresent", name).fireSync();
  }

  @Override
  public boolean addPermanentServiceTask(@NotNull ServiceTask serviceTask) {
    Preconditions.checkNotNull(serviceTask);
    return this.rpcSender.invokeMethod("addPermanentServiceTask", serviceTask).fireSync();
  }

  @Override
  public void removePermanentServiceTask(@NotNull String name) {
    Preconditions.checkNotNull(name);
    this.rpcSender.invokeMethod("removePermanentServiceTask", name).fireSync();
  }

  @Override
  public void removePermanentServiceTask(@NotNull ServiceTask serviceTask) {
    Preconditions.checkNotNull(serviceTask);
    this.removePermanentServiceTask(serviceTask.getName());
  }
}
