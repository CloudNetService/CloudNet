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

package de.dytanic.cloudnet.provider.service;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.driver.provider.service.GeneralCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceLifeCycle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NodeGeneralCloudServiceProvider implements GeneralCloudServiceProvider {

  private final CloudNet cloudNet;

  public NodeGeneralCloudServiceProvider(CloudNet cloudNet) {
    this.cloudNet = cloudNet;
  }

  @Override
  public Collection<UUID> getServicesAsUniqueId() {
    return Collections
      .unmodifiableCollection(this.cloudNet.getCloudServiceManager().getGlobalServiceInfoSnapshots().keySet());
  }

  @Nullable
  @Override
  public ServiceInfoSnapshot getCloudServiceByName(@NotNull String name) {
    return this.cloudNet.getCloudServiceManager().getGlobalServiceInfoSnapshots().values().stream()
      .filter(serviceInfoSnapshot -> serviceInfoSnapshot.getServiceId().getName().equalsIgnoreCase(name))
      .findFirst()
      .orElse(null);
  }

  @Override
  public Collection<ServiceInfoSnapshot> getCloudServices() {
    return new ArrayList<>(this.cloudNet.getCloudServiceManager().getGlobalServiceInfoSnapshots().values());
  }

  @Override
  public Collection<ServiceInfoSnapshot> getStartedCloudServices() {
    return this.getCloudServices().stream()
      .filter(serviceInfoSnapshot -> serviceInfoSnapshot.getLifeCycle() == ServiceLifeCycle.RUNNING)
      .collect(Collectors.toList());
  }

  @Override
  public Collection<ServiceInfoSnapshot> getCloudServices(@NotNull String taskName) {
    Preconditions.checkNotNull(taskName);

    return this.getCloudServices().stream()
      .filter(snapshot -> snapshot.getServiceId().getTaskName().equalsIgnoreCase(taskName))
      .collect(Collectors.toList());
  }

  @Override
  public Collection<ServiceInfoSnapshot> getCloudServices(@NotNull ServiceEnvironmentType environment) {
    Preconditions.checkNotNull(environment);

    return this.getCloudServices().stream()
      .filter(snapshot -> snapshot.getServiceId().getEnvironment() == environment)
      .collect(Collectors.toList());
  }

  @Override
  public Collection<ServiceInfoSnapshot> getCloudServicesByGroup(@NotNull String group) {
    Preconditions.checkNotNull(group);

    return this.cloudNet.getCloudServiceManager().getGlobalServiceInfoSnapshots().values()
      .stream()
      .filter(serviceInfoSnapshot -> Arrays.asList(serviceInfoSnapshot.getConfiguration().getGroups()).contains(group))
      .collect(Collectors.toList());
  }

  @Nullable
  @Override
  public ServiceInfoSnapshot getCloudService(@NotNull UUID uniqueId) {
    Preconditions.checkNotNull(uniqueId);

    return this.cloudNet.getCloudServiceManager().getGlobalServiceInfoSnapshots().get(uniqueId);
  }

  @Override
  public int getServicesCount() {
    return this.cloudNet.getCloudServiceManager().getGlobalServiceInfoSnapshots().size();
  }

  @Override
  public int getServicesCountByGroup(@NotNull String group) {
    Preconditions.checkNotNull(group);

    int amount = 0;

    for (ServiceInfoSnapshot serviceInfoSnapshot : this.cloudNet.getCloudServiceManager()
      .getGlobalServiceInfoSnapshots().values()) {
      if (Arrays.asList(serviceInfoSnapshot.getConfiguration().getGroups()).contains(group)) {
        amount++;
      }
    }

    return amount;
  }

  @Override
  public int getServicesCountByTask(@NotNull String taskName) {
    Preconditions.checkNotNull(taskName);

    int amount = 0;

    for (ServiceInfoSnapshot serviceInfoSnapshot : this.cloudNet.getCloudServiceManager()
      .getGlobalServiceInfoSnapshots().values()) {
      if (serviceInfoSnapshot.getServiceId().getTaskName().equals(taskName)) {
        amount++;
      }
    }

    return amount;
  }

  @Override
  @NotNull
  public ITask<Collection<UUID>> getServicesAsUniqueIdAsync() {
    return this.cloudNet.scheduleTask(this::getServicesAsUniqueId);
  }

  @Override
  @NotNull
  public ITask<ServiceInfoSnapshot> getCloudServiceByNameAsync(@NotNull String name) {
    return this.cloudNet.scheduleTask(() -> this.getCloudServiceByName(name));
  }

  @Override
  @NotNull
  public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync() {
    return this.cloudNet.scheduleTask(this::getCloudServices);
  }

  @Override
  @NotNull
  public ITask<Collection<ServiceInfoSnapshot>> getStartedCloudServicesAsync() {
    return this.cloudNet.scheduleTask(this::getStartedCloudServices);
  }

  @Override
  @NotNull
  public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync(@NotNull String taskName) {
    Preconditions.checkNotNull(taskName);

    return this.cloudNet.scheduleTask(() -> this.getCloudServices(taskName));
  }

  @Override
  @NotNull
  public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesAsync(@NotNull ServiceEnvironmentType environment) {
    return this.cloudNet.scheduleTask(() -> this.getCloudServices(environment));
  }

  @Override
  @NotNull
  public ITask<Collection<ServiceInfoSnapshot>> getCloudServicesByGroupAsync(@NotNull String group) {
    Preconditions.checkNotNull(group);

    return this.cloudNet.scheduleTask(() -> this.getCloudServicesByGroup(group));
  }

  @Override
  @NotNull
  public ITask<Integer> getServicesCountAsync() {
    return this.cloudNet.scheduleTask(this::getServicesCount);
  }

  @Override
  @NotNull
  public ITask<Integer> getServicesCountByGroupAsync(@NotNull String group) {
    Preconditions.checkNotNull(group);

    return this.cloudNet.scheduleTask(() -> this.getServicesCountByGroup(group));
  }

  @Override
  @NotNull
  public ITask<Integer> getServicesCountByTaskAsync(@NotNull String taskName) {
    Preconditions.checkNotNull(taskName);

    return this.cloudNet.scheduleTask(() -> this.getServicesCountByTask(taskName));
  }

  @Override
  @NotNull
  public ITask<ServiceInfoSnapshot> getCloudServiceAsync(@NotNull UUID uniqueId) {
    Preconditions.checkNotNull(uniqueId);

    return this.cloudNet.scheduleTask(() -> this.getCloudService(uniqueId));
  }
}
