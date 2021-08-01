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

package eu.cloudnetservice.cloudnet.ext.npcs.node.listener;

import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.common.io.HttpConnectionProvider;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.util.DefaultModuleHelper;
import de.dytanic.cloudnet.event.service.CloudServicePreStartEvent;
import eu.cloudnetservice.cloudnet.ext.npcs.node.CloudNetNPCModule;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

public class IncludePluginListener {

  private static final String PROTOCOLLIB_DOWNLOAD_URL = "https://ci.dmulloy2.net/job/ProtocolLib/lastSuccessfulBuild/artifact/target/ProtocolLib.jar";
  private static final Path PROTOCOLLIB_CACHE_PATH = Paths
    .get(System.getProperty("cloudnet.tempDir", "temp"), "caches", "ProtocolLib.jar");

  private static final Logger LOGGER = LogManager.getLogger(IncludePluginListener.class);

  private final CloudNetNPCModule npcModule;

  public IncludePluginListener(CloudNetNPCModule npcModule) {
    this.npcModule = npcModule;
    this.downloadProtocolLib();
  }

  private void downloadProtocolLib() {
    try {
      URLConnection urlConnection = HttpConnectionProvider.provideConnection(PROTOCOLLIB_DOWNLOAD_URL, 2_000);
      urlConnection.setUseCaches(false);
      urlConnection.connect();

      try (InputStream inputStream = urlConnection.getInputStream()) {
        Files.copy(inputStream, PROTOCOLLIB_CACHE_PATH, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (IOException exception) {
      LOGGER.warning("Unable to download ProtocolLib!", exception);
    }
  }

  @EventListener
  public void handle(CloudServicePreStartEvent event) {
    if (!event.getCloudService().getServiceConfiguration().getServiceId().getEnvironment().isMinecraftJavaServer()) {
      return;
    }

    boolean installPlugin = this.npcModule.getNPCConfiguration().getConfigurations().stream()
      .anyMatch(npcConfigurationEntry -> Arrays.asList(event.getCloudService().getServiceConfiguration().getGroups())
        .contains(npcConfigurationEntry.getTargetGroup()));

    Path pluginsFolder = event.getCloudService().getDirectoryPath().resolve("plugins");
    FileUtils.createDirectoryReported(pluginsFolder);

    Path targetFile = pluginsFolder.resolve("cloudnet-npcs.jar");
    FileUtils.deleteFileReported(targetFile);

    if (installPlugin) {
      Path protocolLibTargetPath = pluginsFolder.resolve("ProtocolLib.jar");
      if (Files.notExists(protocolLibTargetPath) && Files.exists(PROTOCOLLIB_CACHE_PATH)) {
        try {
          Files.copy(PROTOCOLLIB_CACHE_PATH, protocolLibTargetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
          LOGGER.severe("Unable to copy ProtocolLib!", exception);
          return;
        }
      }

      if (DefaultModuleHelper.copyCurrentModuleInstanceFromClass(IncludePluginListener.class, targetFile)) {
        DefaultModuleHelper.copyPluginConfigurationFileForEnvironment(
          IncludePluginListener.class,
          event.getCloudService().getServiceConfiguration().getProcessConfig().getEnvironment(),
          targetFile
        );
      }
    }
  }
}
