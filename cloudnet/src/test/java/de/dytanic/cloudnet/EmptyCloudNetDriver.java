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

package de.dytanic.cloudnet;

import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.logging.LogLevel;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.database.DatabaseProvider;
import de.dytanic.cloudnet.driver.network.INetworkClient;
import de.dytanic.cloudnet.driver.provider.service.SpecificCloudServiceProvider;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import java.util.Collection;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EmptyCloudNetDriver extends CloudNetDriver {

  public EmptyCloudNetDriver() {
    super(null);
  }

  @Override
  public void start() throws Exception {

  }

  @Override
  public void stop() {

  }

  @Override
  public @NotNull String getComponentName() {
    return null;
  }

  @Override
  public @NotNull String getNodeUniqueId() {
    return null;
  }

  @Override
  public @NotNull TemplateStorage getLocalTemplateStorage() {
    return null;
  }

  @Override
  public @Nullable TemplateStorage getTemplateStorage(String storage) {
    return null;
  }

  @Override
  public @NotNull Collection<TemplateStorage> getAvailableTemplateStorages() {
    return null;
  }

  @Override
  public @NotNull ITask<Collection<TemplateStorage>> getAvailableTemplateStoragesAsync() {
    return null;
  }

  @Override
  public @NotNull DatabaseProvider getDatabaseProvider() {
    return null;
  }

  @Override
  public @NotNull SpecificCloudServiceProvider getCloudServiceProvider(@NotNull String name) {
    return null;
  }

  @Override
  public @NotNull SpecificCloudServiceProvider getCloudServiceProvider(@NotNull UUID uniqueId) {
    return null;
  }

  @Override
  public @NotNull SpecificCloudServiceProvider getCloudServiceProvider(
    @NotNull ServiceInfoSnapshot serviceInfoSnapshot) {
    return null;
  }

  @Override
  public @NotNull INetworkClient getNetworkClient() {
    return null;
  }

  @Override
  public void setGlobalLogLevel(@NotNull LogLevel logLevel) {

  }

  @Override
  public void setGlobalLogLevel(int logLevel) {

  }

  @Override
  public Pair<Boolean, String[]> sendCommandLineAsPermissionUser(@NotNull UUID uniqueId, @NotNull String commandLine) {
    return null;
  }

  @Override
  public @NotNull ITask<Pair<Boolean, String[]>> sendCommandLineAsPermissionUserAsync(@NotNull UUID uniqueId,
    @NotNull String commandLine) {
    return null;
  }
}
