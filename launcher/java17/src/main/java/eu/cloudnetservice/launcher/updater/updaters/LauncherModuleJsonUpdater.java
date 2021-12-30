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

package eu.cloudnetservice.launcher.updater.updaters;

import eu.cloudnetservice.launcher.updater.LauncherUpdaterContext;
import eu.cloudnetservice.launcher.updater.util.FileDownloadUpdateHelper;
import eu.cloudnetservice.updater.Updater;
import eu.cloudnetservice.updater.util.GitHubUtil;
import lombok.NonNull;

public final class LauncherModuleJsonUpdater implements Updater<LauncherUpdaterContext> {

  @Override
  public void executeUpdates(@NonNull LauncherUpdaterContext context) throws Exception {
    // get the new checksum of the file
    var checksum = context.checksums().getProperty("modules-json");
    var moduleJsonPath = context.launcher().workingDirectory().resolve("modules.json");
    var downloadUri = GitHubUtil.buildUri(context.repo(), context.branch(), "modules.json");
    // download the new modules.json
    FileDownloadUpdateHelper.updateFile(downloadUri, moduleJsonPath, checksum, "modules.json");
  }
}
