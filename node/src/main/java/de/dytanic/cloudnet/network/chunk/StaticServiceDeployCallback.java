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
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.network.chunk.ChunkedPacketHandler.Callback;
import de.dytanic.cloudnet.driver.network.chunk.data.ChunkSessionInformation;
import de.dytanic.cloudnet.service.ICloudServiceManager;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

final class StaticServiceDeployCallback implements Callback {

  public static final StaticServiceDeployCallback INSTANCE = new StaticServiceDeployCallback();

  private static final Logger LOGGER = LogManager.getLogger(StaticServiceDeployCallback.class);

  private StaticServiceDeployCallback() {
  }

  @Override
  public void handleSessionComplete(
    @NotNull ChunkSessionInformation information,
    @NotNull InputStream dataInput
  ) {
    // read the information for the deployment of the static service
    String service = information.getTransferInformation().readString();
    boolean overwriteService = information.getTransferInformation().readBoolean();

    ICloudServiceManager serviceManager = CloudNet.getInstance().getCloudServiceProvider();
    // only copy the static service running with the same name
    if (serviceManager.getLocalCloudService(service) == null) {
      Path servicePath = serviceManager.getPersistentServicesDirectoryPath().resolve(service);

      if (Files.exists(servicePath) && !overwriteService) {
        LOGGER.severe("Folder exists");
        return;
      }

      FileUtils.delete(servicePath);
      FileUtils.createDirectory(servicePath);
      FileUtils.extract(dataInput, servicePath);

      LOGGER.info("Extracting");

    } else {
      LOGGER.severe("Service exists"); //TODO
    }
  }
}
