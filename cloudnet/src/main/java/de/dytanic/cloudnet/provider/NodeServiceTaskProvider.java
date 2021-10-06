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

package de.dytanic.cloudnet.provider;

import de.dytanic.cloudnet.driver.provider.ServiceTaskProvider;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NodeServiceTaskProvider implements ServiceTaskProvider {

  private static final Path TASKS_DIRECTORY = Paths
    .get(System.getProperty("cloudnet.config.tasks.directory.path", "local/tasks"));

  @Override
  public void reload() {

  }

  @Override
  public Collection<ServiceTask> getPermanentServiceTasks() {
    return null;
  }

  @Override
  public void setPermanentServiceTasks(
    @NotNull Collection<ServiceTask> serviceTasks) {

  }

  @Override
  public @Nullable ServiceTask getServiceTask(
    @NotNull String name) {
    return null;
  }

  @Override
  public boolean isServiceTaskPresent(@NotNull String name) {
    return false;
  }

  @Override
  public boolean addPermanentServiceTask(
    @NotNull ServiceTask serviceTask) {
    return false;
  }

  @Override
  public void removePermanentServiceTask(@NotNull String name) {

  }

  @Override
  public void removePermanentServiceTask(
    @NotNull ServiceTask serviceTask) {

  }
}
