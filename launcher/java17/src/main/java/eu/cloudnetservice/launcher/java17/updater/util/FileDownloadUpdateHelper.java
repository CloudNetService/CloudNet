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

package eu.cloudnetservice.launcher.java17.updater.util;

import eu.cloudnetservice.ext.updater.util.ChecksumUtil;
import eu.cloudnetservice.launcher.java17.util.HttpUtil;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.NonNull;

public final class FileDownloadUpdateHelper {

  private FileDownloadUpdateHelper() {
    throw new UnsupportedOperationException();
  }

  public static boolean updateFile(
    @NonNull URI downloadUri,
    @NonNull Path target,
    @NonNull String expectedChecksum,
    @NonNull String displayName,
    boolean onlyIfRequired
  ) throws Exception {
    // if the target file exists we can pre-validate the checksum to determine if an update is required
    if (Files.exists(target)) {
      var currentChecksum = ChecksumUtil.fileShaSum(target);
      if (currentChecksum.equals(expectedChecksum) || onlyIfRequired) {
        // already using the latest file
        // CHECKSTYLE.OFF: Launcher has no proper logger
        System.out.printf("Skipping download of \"%s\" because the file is already up-to-date%n", displayName);
        // CHECKSTYLE.ON
        return false;
      }
    }
    // load the new file
    // CHECKSTYLE.OFF: Launcher has no proper logger
    System.out.printf("Downloading update of file \"%s\" from %s... %n", displayName, downloadUri);
    // CHECKSTYLE.ON
    HttpUtil.get(downloadUri, HttpUtil.handlerForFile(target));
    // ensure that the file is now correct
    var newChecksum = ChecksumUtil.fileShaSum(target);
    if (!newChecksum.equals(expectedChecksum)) {
      throw new IllegalStateException("Suspicious checksum for file " + target);
    }
    // we did the update!
    return true;
  }
}
