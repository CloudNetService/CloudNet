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

package de.dytanic.cloudnet.network.chunk;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.language.I18n;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.network.chunk.ChunkedPacketHandler.Callback;
import de.dytanic.cloudnet.driver.network.chunk.data.ChunkSessionInformation;
import java.io.InputStream;
import java.nio.file.Files;
import org.jetbrains.annotations.NotNull;

final class StaticServiceDeployCallback implements Callback {

  public static final StaticServiceDeployCallback INSTANCE = new StaticServiceDeployCallback();

  private static final Logger LOGGER = LogManager.logger(StaticServiceDeployCallback.class);

  private StaticServiceDeployCallback() {
  }

  @Override
  public void handleSessionComplete(
    @NotNull ChunkSessionInformation information,
    @NotNull InputStream dataInput
  ) {
    // read the information for the deployment of the static service
    var service = information.transferInformation().readString();
    var overwriteService = information.transferInformation().readBoolean();

    var serviceManager = CloudNet.instance().cloudServiceProvider();
    // only copy the static service running with the same name
    if (serviceManager.localCloudService(service) == null) {
      var servicePath = serviceManager.persistentServicesDirectory().resolve(service);
      // check if the service path exists, and we can overwrite it
      if (Files.exists(servicePath) && !overwriteService) {
        LOGGER.severe(I18n.trans("command-cluster-push-static-services-existing"));
        return;
      }
      // delete the old contents
      FileUtils.delete(servicePath);
      // recreate the directory
      FileUtils.createDirectory(servicePath);
      // extract the received data to the given path of the service
      FileUtils.extract(dataInput, servicePath);
      LOGGER.info(I18n.trans("command-cluster-push-static-services-received-success"));
    } else {
      LOGGER.severe(I18n.trans("command-cluster-push-static-services-running"));
    }
  }
}
