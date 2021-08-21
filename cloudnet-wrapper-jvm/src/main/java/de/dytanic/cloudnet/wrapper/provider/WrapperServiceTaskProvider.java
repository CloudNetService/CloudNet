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
import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.concurrent.CompletedTask;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.api.DriverAPIUser;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.rpc.RPCSender;
import de.dytanic.cloudnet.driver.provider.ServiceTaskProvider;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public class WrapperServiceTaskProvider implements ServiceTaskProvider, DriverAPIUser {

  private final Wrapper wrapper;
  private final RPCSender rpcSender;

  public WrapperServiceTaskProvider(Wrapper wrapper) {
    this.wrapper = wrapper;
    this.rpcSender = wrapper.getRPCProviderFactory()
      .providerForClass(wrapper.getNetworkClient(), ServiceTaskProvider.class);
  }

  @Override
  public void reload() {
    this.rpcSender.invokeMethod("reload").fireAndForget();
  }

  @Override
  public Collection<ServiceTask> getPermanentServiceTasks() {
    return this.rpcSender.invokeMethod("getPermanentServiceTasks").fireSync();
  }

  @Override
  public void setPermanentServiceTasks(@NotNull Collection<ServiceTask> serviceTasks) {
    Preconditions.checkNotNull(serviceTasks);
    this.rpcSender.invokeMethod("setPermanentServiceTasks", serviceTasks).fireAndForget();
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
    this.rpcSender.invokeMethod("removePermanentServiceTask", name).fireAndForget();
  }

  @Override
  public void removePermanentServiceTask(@NotNull ServiceTask serviceTask) {
    Preconditions.checkNotNull(serviceTask);
    this.removePermanentServiceTask(serviceTask.getName());
  }

  @Override
  public @NotNull ITask<Void> reloadAsync() {
    this.reload();
    return CompletedTask.voidTask();
  }

  @Override
  @NotNull
  public ITask<Collection<ServiceTask>> getPermanentServiceTasksAsync() {
    return CompletableTask.supplyAsync(this::getPermanentServiceTasks);
  }

  @Override
  public @NotNull ITask<Void> setPermanentServiceTasksAsync(@NotNull Collection<ServiceTask> serviceTasks) {
    this.setPermanentServiceTasks(serviceTasks);
    return CompletedTask.voidTask();
  }

  @Override
  @NotNull
  public ITask<ServiceTask> getServiceTaskAsync(@NotNull String name) {
    return CompletableTask.supplyAsync(() -> this.getServiceTask(name));
  }

  @Override
  @NotNull
  public ITask<Boolean> isServiceTaskPresentAsync(@NotNull String name) {
    return CompletableTask.supplyAsync(() -> this.isServiceTaskPresent(name));
  }

  @Override
  @NotNull
  public ITask<Boolean> addPermanentServiceTaskAsync(@NotNull ServiceTask serviceTask) {
    return CompletableTask.supplyAsync(() -> this.addPermanentServiceTask(serviceTask));
  }

  @Override
  @NotNull
  public ITask<Void> removePermanentServiceTaskAsync(@NotNull String name) {
    this.removePermanentServiceTask(name);
    return CompletedTask.voidTask();
  }

  @Override
  @NotNull
  public ITask<Void> removePermanentServiceTaskAsync(@NotNull ServiceTask serviceTask) {
    return this.removePermanentServiceTaskAsync(serviceTask.getName());
  }

  @Override
  public INetworkChannel getNetworkChannel() {
    return this.wrapper.getNetworkChannel();
  }
}
