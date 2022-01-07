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

package eu.cloudnetservice.cloudnet.wrapper.provider;

import eu.cloudnetservice.cloudnet.driver.network.rpc.RPCSender;
import eu.cloudnetservice.cloudnet.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.cloudnet.driver.service.ServiceTask;
import eu.cloudnetservice.cloudnet.wrapper.Wrapper;
import eu.cloudnetservice.cloudnet.wrapper.network.listener.message.TaskChannelMessageListener;
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class WrapperServiceTaskProvider implements ServiceTaskProvider {

  private final RPCSender rpcSender;

  public WrapperServiceTaskProvider(@NonNull Wrapper wrapper) {
    this.rpcSender = wrapper.rpcProviderFactory().providerForClass(
      wrapper.networkClient(),
      ServiceTaskProvider.class);
    wrapper.eventManager().registerListener(new TaskChannelMessageListener(wrapper.eventManager()));
  }

  @Override
  public void reload() {
    this.rpcSender.invokeMethod("reload").fireSync();
  }

  @Override
  public @NonNull Collection<ServiceTask> permanentServiceTasks() {
    return this.rpcSender.invokeMethod("permanentServiceTasks").fireSync();
  }

  @Override
  public void permanentServiceTasks(@NonNull Collection<ServiceTask> serviceTasks) {
    this.rpcSender.invokeMethod("permanentServiceTasks", serviceTasks).fireSync();
  }

  @Override
  public @Nullable ServiceTask serviceTask(@NonNull String name) {
    return this.rpcSender.invokeMethod("serviceTask", name).fireSync();
  }

  @Override
  public boolean serviceTaskPresent(@NonNull String name) {
    return this.rpcSender.invokeMethod("serviceTaskPresent", name).fireSync();
  }

  @Override
  public boolean addPermanentServiceTask(@NonNull ServiceTask serviceTask) {
    return this.rpcSender.invokeMethod("addPermanentServiceTask", serviceTask).fireSync();
  }

  @Override
  public void removePermanentServiceTaskByName(@NonNull String name) {
    this.rpcSender.invokeMethod("removePermanentServiceTaskByName", name).fireSync();
  }

  @Override
  public void removePermanentServiceTask(@NonNull ServiceTask serviceTask) {
    this.removePermanentServiceTaskByName(serviceTask.name());
  }
}
