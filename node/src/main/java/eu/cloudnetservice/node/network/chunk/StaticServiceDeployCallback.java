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

package eu.cloudnetservice.node.network.chunk;

import eu.cloudnetservice.common.io.FileUtil;
import eu.cloudnetservice.common.io.ZipUtil;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.driver.network.chunk.ChunkedPacketHandler;
import eu.cloudnetservice.driver.network.chunk.data.ChunkSessionInformation;
import eu.cloudnetservice.node.service.CloudServiceManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.InputStream;
import java.nio.file.Files;
import lombok.NonNull;

@Singleton
final class StaticServiceDeployCallback implements ChunkedPacketHandler.Callback {

  private static final Logger LOGGER = LogManager.logger(StaticServiceDeployCallback.class);

  private final CloudServiceManager cloudServiceManager;

  @Inject
  public StaticServiceDeployCallback(@NonNull CloudServiceManager cloudServiceManager) {
    this.cloudServiceManager = cloudServiceManager;
  }

  @Override
  public void handleSessionComplete(
    @NonNull ChunkSessionInformation information,
    @NonNull InputStream dataInput
  ) {
    // read the information for the deployment of the static service
    var service = information.transferInformation().readString();
    var overwriteService = information.transferInformation().readBoolean();

    // only copy the static service running with the same name
    if (this.cloudServiceManager.localCloudService(service) == null) {
      var servicePath = this.cloudServiceManager.persistentServicesDirectory().resolve(service);
      // check if the service path exists, and we can overwrite it
      if (Files.exists(servicePath) && !overwriteService) {
        LOGGER.severe(I18n.trans("command-cluster-push-static-service-existing", service));
        return;
      }

      // delete the old contents
      FileUtil.delete(servicePath);
      // recreate the directory
      FileUtil.createDirectory(servicePath);
      // extract the received data to the given path of the service
      ZipUtil.extract(dataInput, servicePath);
      LOGGER.info(I18n.trans("command-cluster-push-static-service-received-success", service));
    } else {
      LOGGER.severe(I18n.trans("command-cluster-push-static-service-running-remote", service));
    }
  }
}
