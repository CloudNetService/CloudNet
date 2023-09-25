/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.node;

import eu.cloudnetservice.common.io.FileUtil;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.module.ModuleProvider;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.NetworkServer;
import eu.cloudnetservice.driver.network.http.HttpServer;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.console.Console;
import eu.cloudnetservice.node.database.NodeDatabaseProvider;
import eu.cloudnetservice.node.service.CloudServiceManager;
import eu.cloudnetservice.node.version.ServiceVersionProvider;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.concurrent.ScheduledExecutorService;
import lombok.NonNull;

@Singleton
public final class ShutdownHandler {

  public static final String SHUTDOWN_THREAD_NAME = "CloudNet Shutdown Thread";
  private static final Logger LOGGER = LogManager.logger(ShutdownHandler.class);

  private final Console console;
  private final ModuleProvider moduleProvider;
  private final CloudServiceManager serviceManager;
  private final NodeServerProvider nodeServerProvider;
  private final ScheduledExecutorService scheduledExecutor;
  private final ServiceVersionProvider serviceVersionProvider;

  // network
  private final HttpServer httpServer;
  private final NetworkClient networkClient;
  private final NetworkServer networkServer;

  // database stuff
  private final NodeDatabaseProvider databaseProvider;
  private final PermissionManagement permissionManagement;

  @Inject
  public ShutdownHandler(
    @NonNull Console console,
    @NonNull ModuleProvider moduleProvider,
    @NonNull CloudServiceManager serviceManager,
    @NonNull NodeServerProvider nodeServerProvider,
    @NonNull @Named("taskScheduler") ScheduledExecutorService scheduledExecutor,
    @NonNull ServiceVersionProvider serviceVersionProvider,
    @NonNull HttpServer httpServer,
    @NonNull NetworkClient networkClient,
    @NonNull NetworkServer networkServer,
    @NonNull NodeDatabaseProvider databaseProvider,
    @NonNull PermissionManagement permissionManagement
  ) {
    this.console = console;
    this.moduleProvider = moduleProvider;
    this.serviceManager = serviceManager;
    this.nodeServerProvider = nodeServerProvider;
    this.scheduledExecutor = scheduledExecutor;
    this.serviceVersionProvider = serviceVersionProvider;
    this.httpServer = httpServer;
    this.networkClient = networkClient;
    this.networkServer = networkServer;
    this.databaseProvider = databaseProvider;
    this.permissionManagement = permissionManagement;
  }

  public void shutdown() {
    if (TickLoop.RUNNING.getAndSet(false)) {
      try {
        LOGGER.info(I18n.trans("stop-application"));

        // stop task execution
        this.scheduledExecutor.shutdownNow();
        this.serviceVersionProvider.interruptInstallSteps();

        // interrupt the connection to other nodes
        LOGGER.info(I18n.trans("stop-node-connections"));
        this.nodeServerProvider.close();

        // close all services
        LOGGER.info(I18n.trans("stop-services"));
        this.serviceManager.deleteAllCloudServices();

        // close all networking listeners
        LOGGER.info(I18n.trans("stop-network-components"));
        this.httpServer.close();
        this.networkClient.close();
        this.networkServer.close();

        // close all the other providers
        LOGGER.info(I18n.trans("stop-providers"));
        this.permissionManagement.close();
        this.databaseProvider.close();

        // stop & unload all modules
        this.moduleProvider.stopAll();
        this.moduleProvider.unloadAll();

        // remove temp directory
        LOGGER.info(I18n.trans("stop-delete-temp"));
        FileUtil.delete(FileUtil.TEMP_DIR);

        // close console
        this.console.close();
      } catch (Exception exception) {
        LOGGER.severe("Caught exception while trying to cleanly stop CloudNet", exception);
      }

      // exit if this was not called from a shutdown thread. We have to check this to prevent calling System.exit(0)
      // twice which results in the jvm stalling due to a lock
      if (!Thread.currentThread().getName().equals(SHUTDOWN_THREAD_NAME)) {
        System.exit(0);
      }
    }
  }
}
