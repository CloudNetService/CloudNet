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

package eu.cloudnetservice.node.impl.tick;

import dev.derklaro.aerogel.auto.Provides;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.module.ModuleProvider;
import eu.cloudnetservice.driver.network.NetworkClient;
import eu.cloudnetservice.driver.network.NetworkServer;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.node.cluster.NodeServerProvider;
import eu.cloudnetservice.node.impl.console.Console;
import eu.cloudnetservice.node.impl.database.AbstractNodeDatabaseProvider;
import eu.cloudnetservice.node.impl.version.ServiceVersionProvider;
import eu.cloudnetservice.node.service.CloudServiceManager;
import eu.cloudnetservice.node.tick.ShutdownHandler;
import eu.cloudnetservice.utils.base.io.FileUtil;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.concurrent.ScheduledExecutorService;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Provides(ShutdownHandler.class)
public final class DefaultShutdownHandler implements ShutdownHandler {

  public static final String SHUTDOWN_THREAD_NAME = "CloudNet Shutdown Thread";
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultShutdownHandler.class);

  private final I18n i18n;
  private final Console console;
  private final ModuleProvider moduleProvider;
  private final CloudServiceManager serviceManager;
  private final NodeServerProvider nodeServerProvider;
  private final ScheduledExecutorService scheduledExecutor;
  private final ServiceVersionProvider serviceVersionProvider;

  // network
  private final NetworkClient networkClient;
  private final NetworkServer networkServer;

  // database stuff
  private final AbstractNodeDatabaseProvider databaseProvider;

  @Inject
  public DefaultShutdownHandler(
    @NonNull @Service I18n i18n,
    @NonNull Console console,
    @NonNull ModuleProvider moduleProvider,
    @NonNull CloudServiceManager serviceManager,
    @NonNull NodeServerProvider nodeServerProvider,
    @NonNull @Named("taskScheduler") ScheduledExecutorService scheduledExecutor,
    @NonNull ServiceVersionProvider serviceVersionProvider,
    @NonNull NetworkClient networkClient,
    @NonNull NetworkServer networkServer,
    @NonNull AbstractNodeDatabaseProvider databaseProvider
  ) {
    this.i18n = i18n;
    this.console = console;
    this.moduleProvider = moduleProvider;
    this.serviceManager = serviceManager;
    this.nodeServerProvider = nodeServerProvider;
    this.scheduledExecutor = scheduledExecutor;
    this.serviceVersionProvider = serviceVersionProvider;
    this.networkClient = networkClient;
    this.networkServer = networkServer;
    this.databaseProvider = databaseProvider;
  }

  public void shutdown() {
    if (DefaultTickLoop.RUNNING.getAndSet(false)) {
      try {
        LOGGER.info(this.i18n.translate("stop-application"));

        // stop task execution
        this.scheduledExecutor.shutdownNow();
        this.serviceVersionProvider.interruptInstallSteps();

        // interrupt the connection to other nodes
        LOGGER.info(this.i18n.translate("stop-node-connections"));
        this.nodeServerProvider.close();

        // close all services
        LOGGER.info(this.i18n.translate("stop-services"));
        this.serviceManager.deleteAllCloudServices();

        // close all networking listeners
        LOGGER.info(this.i18n.translate("stop-network-components"));
        this.networkClient.close();
        this.networkServer.close();

        // close all the other providers
        LOGGER.info(this.i18n.translate("stop-providers"));
        this.databaseProvider.close();

        // stop & unload all modules
        this.moduleProvider.stopAll();
        this.moduleProvider.unloadAll();

        // remove temp directory
        LOGGER.info(this.i18n.translate("stop-delete-temp"));
        FileUtil.delete(FileUtil.TEMP_DIR);

        // close console
        this.console.close();
      } catch (Exception exception) {
        LOGGER.error("Caught exception while trying to cleanly stop CloudNet", exception);
      }

      // exit if this was not called from a shutdown thread. We have to check this to prevent calling System.exit(0)
      // twice which results in the jvm stalling due to a lock
      if (!Thread.currentThread().getName().equals(SHUTDOWN_THREAD_NAME)) {
        System.exit(0);
      }
    }
  }
}
