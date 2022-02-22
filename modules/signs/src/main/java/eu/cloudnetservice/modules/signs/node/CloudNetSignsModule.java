/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.signs.node;

import eu.cloudnetservice.cloudnet.common.log.LogManager;
import eu.cloudnetservice.cloudnet.common.log.Logger;
import eu.cloudnetservice.cloudnet.driver.database.Database;
import eu.cloudnetservice.cloudnet.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.cloudnet.driver.module.ModuleTask;
import eu.cloudnetservice.cloudnet.driver.module.driver.DriverModule;
import eu.cloudnetservice.cloudnet.driver.service.ServiceEnvironmentType;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.module.listener.PluginIncludeListener;
import eu.cloudnetservice.modules.bridge.WorldPosition;
import eu.cloudnetservice.modules.signs.GlobalChannelMessageListener;
import eu.cloudnetservice.modules.signs.Sign;
import eu.cloudnetservice.modules.signs.SignManagement;
import eu.cloudnetservice.modules.signs._deprecated.SignConstants;
import eu.cloudnetservice.modules.signs.configuration.SignsConfiguration;
import eu.cloudnetservice.modules.signs.node.configuration.NodeSignsConfigurationHelper;
import java.util.Collection;

public class CloudNetSignsModule extends DriverModule {

  protected static final String DATABASE_NAME = "cloudnet_signs";

  private static final Logger LOGGER = LogManager.logger(CloudNetSignsModule.class);

  protected Database database;
  protected SignsConfiguration configuration;

  @ModuleTask(order = 50)
  public void initialize() {
    this.database = CloudNet.instance().databaseProvider().database(DATABASE_NAME);
  }

  @ModuleTask(order = 40)
  public void loadConfiguration() {
    this.configuration = NodeSignsConfigurationHelper.read(this.configPath());
  }

  @ModuleTask(order = 30)
  public void handleInitialization() {
    SignManagement management = new NodeSignManagement(this.configuration, this.configPath(), this.database);
    management.registerToServiceRegistry();

    this.registerListener(new GlobalChannelMessageListener(management), new NodeSignsListener(management));
    this.registerListener(new PluginIncludeListener(
      "cloudnet-signs",
      CloudNetSignsModule.class,
      service -> ServiceEnvironmentType.minecraftServer(service.serviceId().environment())
        && this.configuration.configurationEntries().stream()
        .anyMatch(entry -> service.serviceConfiguration().groups().contains(entry.targetGroup()))));
  }

  @Deprecated
  @ModuleTask(order = 20)
  public void handleDatabaseConvert() {
    this.convertDatabaseIfNecessary();
  }

  @ModuleTask(order = 40, event = ModuleLifeCycle.STOPPED)
  public void handleStopping() throws Exception {
    this.database.close();
    CloudNet.instance().eventManager().unregisterListeners(this.getClass().getClassLoader());
  }

  @ModuleTask(event = ModuleLifeCycle.RELOADING)
  public void handleReload() {
    var management = this.serviceRegistry().firstProvider(SignManagement.class);
    if (management != null) {
      management.signsConfiguration(NodeSignsConfigurationHelper.read(this.configPath()));
    }
  }

  @Deprecated
  private void convertDatabaseIfNecessary() {
    // load old database document
    var database = CloudNet.instance().databaseProvider().database("cloudNet_module_configuration");
    var document = database.get("signs_store");
    // when the document is null the conversation already happened
    if (document != null) {
      // notify the user about the change
      LOGGER.warning("Detected old signs database, running conversation...");
      // remove the old document from the database
      database.delete("signs_store");
      // check if the old sign document even contains the signs
      Collection<eu.cloudnetservice.modules.signs._deprecated.Sign> oldSigns = document.get("signs",
        SignConstants.COLLECTION_SIGNS);
      if (oldSigns != null) {
        // convert the old sign entries
        var management = CloudNet.instance().serviceRegistry().firstProvider(SignManagement.class);
        for (var oldSign : oldSigns) {
          management.createSign(new Sign(
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
