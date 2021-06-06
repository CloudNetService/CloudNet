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
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.api.DriverAPIUser;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.provider.ServiceTaskProvider;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

public class WrapperServiceTaskProvider implements ServiceTaskProvider, DriverAPIUser {

  private final Wrapper wrapper;

  public WrapperServiceTaskProvider(Wrapper wrapper) {
    this.wrapper = wrapper;
  }

  @Override
  public void reload() {
    this.reloadAsync().get(5, TimeUnit.SECONDS, null);
  }

  @Override
  public Collection<ServiceTask> getPermanentServiceTasks() {
    return this.getPermanentServiceTasksAsync().get(5, TimeUnit.SECONDS, null);
  }

  @Override
  public void setPermanentServiceTasks(@NotNull Collection<ServiceTask> serviceTasks) {
    this.setPermanentServiceTasksAsync(serviceTasks).get(5, TimeUnit.SECONDS, null);
  }

  @Override
  public ServiceTask getServiceTask(@NotNull String name) {
    Preconditions.checkNotNull(name);
    return this.getServiceTaskAsync(name).get(5, TimeUnit.SECONDS, null);
  }

  @Override
  public boolean isServiceTaskPresent(@NotNull String name) {
    Preconditions.checkNotNull(name);
    return this.isServiceTaskPresentAsync(name).get(5, TimeUnit.SECONDS, false);
  }

  @Override
  public boolean addPermanentServiceTask(@NotNull ServiceTask serviceTask) {
    return this.addPermanentServiceTaskAsync(serviceTask).get(5, TimeUnit.SECONDS, false);
  }

  @Override
  public void removePermanentServiceTask(@NotNull String name) {
    this.removePermanentServiceTaskAsync(name).get(5, TimeUnit.SECONDS, null);
  }

  @Override
  public void removePermanentServiceTask(@NotNull ServiceTask serviceTask) {
    Preconditions.checkNotNull(serviceTask);
    this.removePermanentServiceTask(serviceTask.getName());
  }

  @Override
  public @NotNull ITask<Void> reloadAsync() {
    return this.executeVoidDriverAPIMethod(DriverAPIRequestType.RELOAD_TASKS, null);
  }

  @Override
  @NotNull
  public ITask<Collection<ServiceTask>> getPermanentServiceTasksAsync() {
    return this.executeDriverAPIMethod(
      DriverAPIRequestType.GET_PERMANENT_SERVICE_TASKS,
      packet -> packet.getBuffer().readObjectCollection(ServiceTask.class)
    );
  }

  @Override
  public @NotNull ITask<Void> setPermanentServiceTasksAsync(@NotNull Collection<ServiceTask> serviceTasks) {
    return this.executeVoidDriverAPIMethod(
      DriverAPIRequestType.SET_PERMANENT_SERVICE_TASKS,
      buffer -> buffer.writeObjectCollection(serviceTasks)
    );
  }

  @Override
  @NotNull
  public ITask<ServiceTask> getServiceTaskAsync(@NotNull String name) {
    Preconditions.checkNotNull(name);

    return this.executeDriverAPIMethod(
      DriverAPIRequestType.GET_PERMANENT_SERVICE_TASK_BY_NAME,
      buffer -> buffer.writeString(name),
      packet -> packet.getBuffer().readOptionalObject(ServiceTask.class)
    );
  }

  @Override
  @NotNull
  public ITask<Boolean> isServiceTaskPresentAsync(@NotNull String name) {
    Preconditions.checkNotNull(name);

    return this.executeDriverAPIMethod(
      DriverAPIRequestType.IS_SERVICE_TASK_PRESENT,
      buffer -> buffer.writeString(name),
      packet -> packet.getBuffer().readBoolean()
    );
  }

  @Override
  @NotNull
  public ITask<Boolean> addPermanentServiceTaskAsync(@NotNull ServiceTask serviceTask) {
    Preconditions.checkNotNull(serviceTask);

    return this.executeDriverAPIMethod(
      DriverAPIRequestType.ADD_PERMANENT_SERVICE_TASK,
      buffer -> buffer.writeObject(serviceTask),
      packet -> packet.getBuffer().readBoolean()
    );
  }

  @Override
  @NotNull
  public ITask<Void> removePermanentServiceTaskAsync(@NotNull String name) {
    Preconditions.checkNotNull(name);

    return this.executeVoidDriverAPIMethod(
      DriverAPIRequestType.REMOVE_PERMANENT_SERVICE_TASK,
      buffer -> buffer.writeString(name)
    );
  }

  @Override
  @NotNull
  public ITask<Void> removePermanentServiceTaskAsync(@NotNull ServiceTask serviceTask) {
    Preconditions.checkNotNull(serviceTask);

    return this.removePermanentServiceTaskAsync(serviceTask.getName());
  }

  @Override
  public INetworkChannel getNetworkChannel() {
    return this.wrapper.getNetworkChannel();
  }
}
