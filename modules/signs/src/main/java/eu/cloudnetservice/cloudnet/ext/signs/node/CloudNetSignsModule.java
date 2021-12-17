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

package eu.cloudnetservice.cloudnet.ext.signs.node;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.database.Database;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import de.dytanic.cloudnet.driver.module.ModuleTask;
import de.dytanic.cloudnet.driver.module.driver.DriverModule;
import de.dytanic.cloudnet.ext.bridge.WorldPosition;
import de.dytanic.cloudnet.ext.signs.SignConstants;
import eu.cloudnetservice.cloudnet.ext.signs.GlobalChannelMessageListener;
import eu.cloudnetservice.cloudnet.ext.signs.Sign;
import eu.cloudnetservice.cloudnet.ext.signs.SignManagement;
import eu.cloudnetservice.cloudnet.ext.signs.configuration.SignsConfiguration;
import eu.cloudnetservice.cloudnet.ext.signs.node.configuration.NodeSignsConfigurationHelper;
import java.nio.file.Path;
import java.util.Collection;

public class CloudNetSignsModule extends DriverModule {

  protected static final String DATABASE_NAME = "cloudnet_signs";

  private static final Logger LOGGER = LogManager.getLogger(CloudNetSignsModule.class);

  protected Database database;
  protected Path configurationPath;
  protected SignsConfiguration configuration;

  @ModuleTask(order = 50)
  public void initialize() {
    this.database = CloudNet.getInstance().databaseProvider().database(DATABASE_NAME);
    this.configurationPath = this.moduleWrapper().dataDirectory().resolve("config.json");
  }

  @ModuleTask(order = 40)
  public void loadConfiguration() {
    this.configuration = NodeSignsConfigurationHelper.read(this.configurationPath);
  }

  @ModuleTask(order = 30)
  public void handleInitialization() {
    SignManagement management = new NodeSignManagement(this.configuration, this.configurationPath, this.database);
    management.registerToServiceRegistry();

    this.registerListener(new GlobalChannelMessageListener(management), new NodeSignsListener(management));
  }

  @Deprecated
  @ModuleTask(order = 20)
  public void handleDatabaseConvert() {
    this.convertDatabaseIfNecessary();
  }

  @ModuleTask(order = 40, event = ModuleLifeCycle.STOPPED)
  public void handleStopping() throws Exception {
    this.database.close();
    CloudNet.getInstance().eventManager().unregisterListeners(this.getClass().getClassLoader());
  }

  @Deprecated
  private void convertDatabaseIfNecessary() {
    // load old database document
    var database = CloudNet.getInstance().databaseProvider().database("cloudNet_module_configuration");
    var document = database.get("signs_store");
    // when the document is null the conversation already happened
    if (document != null) {
      // notify the user about the change
      LOGGER.warning("Detected old signs database, running conversation...");
      // remove the old document from the database
      database.delete("signs_store");
      // check if the old sign document even contains the signs
      Collection<de.dytanic.cloudnet.ext.signs.Sign> oldSigns = document.get("signs", SignConstants.COLLECTION_SIGNS);
      if (oldSigns != null) {
        // convert the old sign entries
        var management = CloudNet.getInstance().servicesRegistry().getFirstService(SignManagement.class);
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
