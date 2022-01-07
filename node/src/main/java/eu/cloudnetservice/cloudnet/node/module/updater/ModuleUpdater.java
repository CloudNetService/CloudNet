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

package eu.cloudnetservice.cloudnet.node.module.updater;

import eu.cloudnetservice.cloudnet.common.io.FileUtils;
import eu.cloudnetservice.cloudnet.common.language.I18n;
import eu.cloudnetservice.cloudnet.common.log.LogManager;
import eu.cloudnetservice.cloudnet.common.log.Logger;
import eu.cloudnetservice.cloudnet.driver.module.DefaultModuleProvider;
import eu.cloudnetservice.ext.updater.Updater;
import eu.cloudnetservice.ext.updater.util.ChecksumUtils;
import java.nio.file.StandardCopyOption;
import kong.unirest.Unirest;
import lombok.NonNull;

public final class ModuleUpdater implements Updater<ModuleUpdaterContext> {

  private static final Logger LOGGER = LogManager.logger(ModuleUpdater.class);

  @Override
  public void executeUpdates(@NonNull ModuleUpdaterContext context) {
    FileUtils.walkFileTree(DefaultModuleProvider.DEFAULT_MODULE_DIR, ($, file) -> {
      // check if we already know an associated module
      var moduleName = context.moduleNames().get(file.toAbsolutePath());
      if (moduleName != null) {
        // check if the module is an official module which gets updates from remote
        context.modules().findByName(moduleName).ifPresent(moduleEntry -> {
          // validate using the current checksum if the file is up-to-date
          var currentChecksum = ChecksumUtils.fileShaSum(file);
          if (!moduleEntry.sha3256().equals(currentChecksum)) {
            // there is an update available - download it!
            Unirest
              .get(moduleEntry.url(context.updaterRepo(), context.updaterBranch()))
              .asFile(file.toString(), StandardCopyOption.REPLACE_EXISTING);
            // validate the checksum now
            var newModuleChecksum = ChecksumUtils.fileShaSum(file);
            if (!moduleEntry.sha3256().equals(newModuleChecksum)) {
              LOGGER.warning(I18n.trans("cloudnet-load-modules-invalid-checksum", moduleName));
              FileUtils.delete(file);
            }
          }
        });
      }
    }, false, "*.{jar,war,zip}");
  }
}
