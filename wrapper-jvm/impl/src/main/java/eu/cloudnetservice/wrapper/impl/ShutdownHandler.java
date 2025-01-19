/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.wrapper.impl;

import eu.cloudnetservice.driver.module.ModuleProvider;
import eu.cloudnetservice.driver.network.NetworkClient;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import lombok.NonNull;

@Singleton
final class ShutdownHandler {

  private final NetworkClient networkClient;
  private final ModuleProvider moduleProvider;
  private final ExecutorService scheduledExecutor;

  @Inject
  public ShutdownHandler(
    @NonNull NetworkClient networkClient,
    @NonNull ModuleProvider moduleProvider,
    @NonNull @Named("taskScheduler") ScheduledExecutorService scheduledExecutor
  ) {
    this.networkClient = networkClient;
    this.moduleProvider = moduleProvider;
    this.scheduledExecutor = scheduledExecutor;
  }

  public void shutdown() {
    try {
      this.networkClient.close();
    } catch (Exception ignored) {
    }

    this.moduleProvider.unloadAll();
    this.scheduledExecutor.shutdownNow();
  }
}
