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

package eu.cloudnetservice.modules.signs.impl.node;

import eu.cloudnetservice.driver.database.Database;
import eu.cloudnetservice.driver.database.DatabaseProvider;
import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.impl.module.ModuleHelper;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.modules.bridge.WorldPosition;
import eu.cloudnetservice.modules.signs.Sign;
import eu.cloudnetservice.modules.signs.SignManagement;
import eu.cloudnetservice.modules.signs.configuration.SignsConfiguration;
import eu.cloudnetservice.modules.signs.impl.SharedChannelMessageListener;
import eu.cloudnetservice.modules.signs.impl._deprecated.SignConstants;
import eu.cloudnetservice.modules.signs.impl.node.configuration.NodeSignsConfigurationHelper;
import eu.cloudnetservice.node.command.CommandProvider;
import eu.cloudnetservice.node.impl.module.listener.PluginIncludeListener;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class CloudNetSignsModule extends DriverModule {

  protected static final String DATABASE_NAME = "cloudnet_signs";

  private static final Logger LOGGER = LoggerFactory.getLogger(CloudNetSignsModule.class);

  protected Database database;
  protected SignsConfiguration configuration;

  @Inject
  public CloudNetSignsModule(@NonNull @Named("module") InjectionLayer<?> layer) {
    layer.installAutoConfigureBindings(this.getClass().getClassLoader(), "signs");
  }

  @ModuleTask(order = 50)
  public void initialize(@NonNull DatabaseProvider databaseProvider) {
    this.database = databaseProvider.database(DATABASE_NAME);
  }

  @ModuleTask(order = 40)
  public void loadConfiguration() {
    // TODO: remove
    this.configuration = NodeSignsConfigurationHelper.read(this.configPath());
  }

  @ModuleTask(order = 30)
  public void handleInitialization(
    @NonNull @Named("module") InjectionLayer<?> layer,
    @NonNull EventManager eventManager,
    @NonNull ModuleHelper moduleHelper,
    @NonNull ServiceRegistry serviceRegistry,
    @NonNull CommandProvider commandProvider
  ) {
    var management = layer.instance(
      NodeSignManagement.class,
      builder -> builder
        .override(SignsConfiguration.class, this.configuration)
        .override(Database.class, this.database));
    management.registerToServiceRegistry(serviceRegistry);

    commandProvider.register(SignCommand.class);

    eventManager.registerListener(SharedChannelMessageListener.class);
    eventManager.registerListener(NodeSignsListener.class);
    eventManager.registerListener(new PluginIncludeListener(
      "cloudnet-signs",
      CloudNetSignsModule.class,
      moduleHelper,
      service -> ServiceEnvironmentType.minecraftServer(service.serviceId().environment())
        && management.signsConfiguration().entries().stream()
        .anyMatch(entry -> service.serviceConfiguration().groups().contains(entry.targetGroup()))));
  }

  @Deprecated
  @ModuleTask(order = 20)
  public void handleDatabaseConvert(
    @NonNull DatabaseProvider databaseProvider,
    @NonNull @Service SignManagement signManagement
  ) {
    this.convertDatabaseIfNecessary(databaseProvider, signManagement);
  }

  @ModuleTask(order = 40, lifecycle = ModuleLifeCycle.STOPPED)
  public void handleStopping() throws Exception {
    this.database.close();
  }

  @ModuleTask(lifecycle = ModuleLifeCycle.RELOADING)
  public void handleReload(@Nullable @Service SignManagement management) {
    if (management != null) {
      management.signsConfiguration(NodeSignsConfigurationHelper.read(this.configPath()));
    }
  }

  @Deprecated
  private void convertDatabaseIfNecessary(
    @NonNull DatabaseProvider databaseProvider,
    @NonNull SignManagement signManagement
  ) {
    // convert the old database (old h2 databases convert the name to lower case - we need to check both names)
    var db = databaseProvider.database("cloudNet_module_configuration");
    if (db.documentCount() == 0) {
      db = databaseProvider.database("cloudnet_module_configuration");
    }

    // get the npc_store field of the database entry
    var document = db.get("signs_store");
    // when the document is null the conversation already happened
    if (document != null) {
      // notify the user about the change
      LOGGER.warn("Detected old signs database, running conversation...");
      // remove the old document from the database
      db.delete("signs_store");
      // check if the old sign document even contains the signs
      Collection<eu.cloudnetservice.modules.signs.impl._deprecated.Sign> oldSigns = document.readObject(
        "signs",
        SignConstants.COLLECTION_SIGNS);
      if (oldSigns != null) {
        // convert the old sign entries
        for (var oldSign : oldSigns) {
          signManagement.createSign(new Sign(
            oldSign.getTargetGroup(),
            oldSign.getTemplatePath(),
            new WorldPosition(
              oldSign.getWorldPosition().x(),
              oldSign.getWorldPosition().y(),
              oldSign.getWorldPosition().z(),
              0,
              0,
              oldSign.getWorldPosition().world(),
              oldSign.getProvidedGroup()
            )));
        }
      }
    }
  }
}
