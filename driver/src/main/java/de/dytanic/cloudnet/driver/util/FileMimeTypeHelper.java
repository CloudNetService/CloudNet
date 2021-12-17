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

package de.dytanic.cloudnet.driver.util;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.NonNull;

public final class FileMimeTypeHelper {

  private FileMimeTypeHelper() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull String fileType(@NonNull String filePath) {
    var mimeType = URLConnection.guessContentTypeFromName(filePath);
    return mimeType == null ? "application/octet-stream" : mimeType;
  }

  public static @NonNull String fileType(@NonNull Path path) {
    try {
      var mimeType = Files.probeContentType(path);
      return mimeType == null ? "application/octet-stream" : mimeType;
    } catch (IOException exception) {
      return "application/octet-stream";
    }
  }
}
