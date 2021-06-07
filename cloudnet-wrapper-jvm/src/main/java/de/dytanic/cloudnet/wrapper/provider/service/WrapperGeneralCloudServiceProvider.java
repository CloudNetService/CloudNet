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

package de.dytanic.cloudnet.wrapper.provider.service;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.api.DriverAPIRequestType;
import de.dytanic.cloudnet.driver.api.DriverAPIUser;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.provider.service.GeneralCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.wrapper.Wrapper;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WrapperGeneralCloudServiceProvider implements GeneralCloudServiceProvider, DriverAPIUser {

  private final Wrapper wrapper;

  public WrapperGeneralCloudServiceProvider(Wrapper wrapper) {
    this.wrapper = wrapper;
  }

  @Override
  public Collection<UUID> getServicesAsUniqueId() {
    return this.getServicesAsUniqueIdAsync().get(5, TimeUnit.SECONDS, null);
  }

  @Nullable
  @Override
  public ServiceInfoSnapshot getCloudServiceByName(@NotNull String name) {
    return this.getCloudServiceByNameAsync(name).get(5, TimeUnit.SECONDS, null);
  }

  @Override
  public Collection<ServiceInfoSnapshot> getCloudServices() {
    return this.getCloudServicesAsync().get(5, TimeUnit.SECONDS, null);
  }

  @Override
  public Collection<ServiceInfoSnapshot> getStartedCloudServices() {
    return this.getStartedCloudServicesAsync().get(5, TimeUnit.SECONDS, null);
  }

  @Override
  public Collection<ServiceInfoSnapshot> getCloudServices(@NotNull String taskName) {
    return this.getCloudServicesAsync(taskName).get(5, TimeUnit.SECONDS, null);
  }

  @Override
  public Collection<ServiceInfoSnapshot> getCloudServices(@NotNull ServiceEnvironmentType environment) {
    Preconditions.checkNotNull(environment);
    return this.getCloudServicesAsync(environment).get(5, TimeUnit.SECONDS, null);
  }

  @Override
  public Collection<ServiceInfoSnapshot> getCloudServicesByGroup(@NotNull String group) {
    return this.getCloudServicesByGroupAsync(group).get(5, TimeUnit.SECONDS, null);
  }

  @Nullable
  @Override
  public ServiceInfoSnapshot getCloudService(@NotNull UUID uniqueId) {
    return this.getCloudServiceAsync(uniqueId).get(5, TimeUnit.SECONDS, null);
  }

  @Override
  public int getServicesCount() {
    return this.getServicesCountAsync().get(5, TimeUnit.SECONDS, null);
  }

  @Override
  public int getServicesCountByGroup(@NotNull String group) {
    return this.getServicesCountByGroupAsync(group).get(5, TimeUnit.SECONDS, null);
  }

  @Override
  public int getServicesCountByTask(@NotNull String taskName) {
    return this.getServicesCountByTaskAsync(taskName).get(5, TimeUnit.SECONDS, null);
  }

  @Override
  @NotNull
  public ITask<Collection<UUID>> getServicesAsUniqueIdAsync() {
    return this.executeDriverAPIMethod(
      DriverAPIRequestType.GET_SERVICES_AS_UNIQUE_ID,
      packet -> packet.getBuffer().readUUIDCollection()
    );
  }

  @Override
  @NotNull
  public ITask<ServiceInfoSnapshot> getCloudServiceByNameAsync(@NotNull String name) {
    return this.executeDriverAPIMethod(
      DriverAPIRequestType.GET_CLOUD_SERVICE_BY_NAME,
      buffer -> buffer.writeString(name),
      packet -> packet.getBuffer().readOptionalObject(ServiceInfoSnapshot.class)
    );
  }

  @Override
  @NotNull
  public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync() {
    return this.executeDriverAPIMethod(
      DriverAPIRequestType.GET_CLOUD_SERVICES,
      packet -> packet.getBuffer().readObjectCollection(ServiceInfoSnapshot.class)
    );
  }

  @Override
  @NotNull
  public ITask<Collection<ServiceInfoSnapshot>> getStartedCloudServicesAsync() {
    return this.executeDriverAPIMethod(
      DriverAPIRequestType.GET_STARTED_CLOUD_SERVICES,
      packet -> packet.getBuffer().readObjectCollection(ServiceInfoSnapshot.class)
    );
  }

  @Override
  @NotNull
  public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync(@NotNull String taskName) {
    return this.executeDriverAPIMethod(
      DriverAPIRequestType.GET_CLOUD_SERVICES_BY_SERVICE_TASK,
      buffer -> buffer.writeString(taskName),
      packet -> packet.getBuffer().readObjectCollection(ServiceInfoSnapshot.class)
    );
  }

  @Override
  @NotNull
  public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync(@NotNull ServiceEnvironmentType environment) {
    Preconditions.checkNotNull(environment);

    return this.executeDriverAPIMethod(
      DriverAPIRequestType.GET_CLOUD_SERVICES_BY_ENVIRONMENT,
      buffer -> buffer.writeEnumConstant(environment),
      packet -> packet.getBuffer().readObjectCollection(ServiceInfoSnapshot.class)
    );
  }

  @Override
  @NotNull
  public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesByGroupAsync(@NotNull String group) {
    return this.executeDriverAPIMethod(
      DriverAPIRequestType.GET_CLOUD_SERVICES_BY_GROUP,
      buffer -> buffer.writeString(group),
      packet -> packet.getBuffer().readObjectCollection(ServiceInfoSnapshot.class)
    );
  }

  @Override
  @NotNull
  public ITask<Integer> getServicesCountAsync() {
    return this.executeDriverAPIMethod(
      DriverAPIRequestType.GET_SERVICES_COUNT,
      packet -> packet.getBuffer().readInt()
    );
  }

  @Override
  @NotNull
  public ITask<Integer> getServicesCountByGroupAsync(@NotNull String group) {
    return this.executeDriverAPIMethod(
      DriverAPIRequestType.GET_SERVICES_COUNT_BY_GROUP,
      buffer -> buffer.writeString(group),
      packet -> packet.getBuffer().readInt()
    );
  }

  @Override
  @NotNull
  public ITask<Integer> getServicesCountByTaskAsync(@NotNull String taskName) {
    return this.executeDriverAPIMethod(
      DriverAPIRequestType.GET_SERVICES_COUNT_BY_TASK,
      buffer -> buffer.writeString(taskName),
      packet -> packet.getBuffer().readInt()
    );
  }

  @Override
  @NotNull
  public ITask<ServiceInfoSnapshot> getCloudServiceAsync(@NotNull UUID uniqueId) {
    return this.executeDriverAPIMethod(
      DriverAPIRequestType.GET_CLOUD_SERVICE_BY_UNIQUE_ID,
      buffer -> buffer.writeUUID(uniqueId),
      packet -> packet.getBuffer().readOptionalObject(ServiceInfoSnapshot.class)
    );
  }

  @Override
  public INetworkChannel getNetworkChannel() {
    return this.wrapper.getNetworkChannel();
  }
}
