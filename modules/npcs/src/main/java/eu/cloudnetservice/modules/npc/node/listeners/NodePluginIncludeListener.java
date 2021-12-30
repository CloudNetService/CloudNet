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

package eu.cloudnetservice.modules.npc.node.listeners;

import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.console.animation.progressbar.ConsoleProgressWrappers;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.service.ServiceEnvironmentType;
import de.dytanic.cloudnet.driver.util.DefaultModuleHelper;
import de.dytanic.cloudnet.event.service.CloudServicePreProcessStartEvent;
import eu.cloudnetservice.modules.npc.AbstractNPCManagement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.NonNull;

public final class NodePluginIncludeListener {

  private static final Path PROTOCOLLIB_CACHE_PATH = FileUtils.TEMP_DIR.resolve("caches/ProtocolLib.jar");

  private final AbstractNPCManagement management;
  private final AtomicBoolean didDownloadProtocolLib = new AtomicBoolean();

  public NodePluginIncludeListener(@NonNull AbstractNPCManagement management) {
    this.management = management;
    // try to download protocol lib
    ConsoleProgressWrappers.wrapDownload(
      "https://ci.dmulloy2.net/job/ProtocolLib/lastSuccessfulBuild/artifact/target/ProtocolLib.jar",
      stream -> {
        FileUtils.createDirectory(PROTOCOLLIB_CACHE_PATH.getParent());
        // copy the input to the file
        try (var out = Files.newOutputStream(PROTOCOLLIB_CACHE_PATH)) {
          FileUtils.copy(stream, out);
        }
        // success!
        this.didDownloadProtocolLib.set(true);
      });
  }

  @EventListener
  public void includePluginIfNecessary(@NonNull CloudServicePreProcessStartEvent event) {
    if (this.didDownloadProtocolLib.get()) {
      var type = event.service().serviceConfiguration().serviceId().environment();
      if (ServiceEnvironmentType.isMinecraftServer(type)) {
        // check if we have an entry for the current group
        var hasEntry = this.management.npcConfiguration().entries().stream()
          .anyMatch(entry -> event.serviceConfiguration().groups().contains(entry.targetGroup()));
        if (hasEntry) {
          var pluginsDirectory = event.service().directory().resolve("plugins");
          // copy protocol lib
          var protocolLibPath = pluginsDirectory.resolve("ProtocolLib.jar");
          FileUtils.copy(PROTOCOLLIB_CACHE_PATH, protocolLibPath);
          // copy the plugin
          var pluginPath = pluginsDirectory.resolve("cloudnet-npcs.jar");
          FileUtils.delete(pluginPath);
          if (DefaultModuleHelper.copyCurrentModuleInstanceFromClass(NodePluginIncludeListener.class, pluginPath)) {
            DefaultModuleHelper.copyPluginConfigurationFileForEnvironment(
              NodePluginIncludeListener.class,
              type,
              pluginPath);
          }
        }
      }
    }
  }
}
