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

package eu.cloudnetservice.launcher.java17.updater.updaters;

import eu.cloudnetservice.ext.updater.Updater;
import eu.cloudnetservice.ext.updater.util.GitHubUtil;
import eu.cloudnetservice.launcher.java17.updater.LauncherUpdaterContext;
import eu.cloudnetservice.launcher.java17.updater.util.FileDownloadUpdateHelper;
import lombok.NonNull;

public final class LauncherPatcherUpdater implements Updater<LauncherUpdaterContext> {

  @Override
  public void executeUpdates(@NonNull LauncherUpdaterContext context, boolean onlyIfRequired) throws Exception {
    // get the new checksum of the file
    var checksum = context.checksums().getProperty("launcher-patcher");
    var launcherPatcherPath = context.launcher().workingDirectory().resolve("launcher-patcher.jar");
    var downloadUri = GitHubUtil.buildUri(context.repo(), context.branch(), "launcher-patcher.jar");
    // download the new launcher patcher file
    FileDownloadUpdateHelper.updateFile(downloadUri, launcherPatcherPath, checksum, "launcher-patcher", onlyIfRequired);
  }
}
