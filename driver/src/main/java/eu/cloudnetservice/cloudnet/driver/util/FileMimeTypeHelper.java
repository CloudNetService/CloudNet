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

package eu.cloudnetservice.cloudnet.driver.util;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.NonNull;

/**
 * The FileMimeTypeHelper provides util methods to guess the file mime type from a file.
 * <p>
 * The fallback file type is "application/octet-stream" for all methods.
 *
 * @author Aldin S. (0utplay@cloudnetservice.eu)
 * @author Pasqual Koschmieder. (derklaro@cloudnetservice.eu)
 * @see Files#probeContentType(Path)
 * @see URLConnection#guessContentTypeFromName(String)
 * @since 4.0
 */
public final class FileMimeTypeHelper {

  /**
   * Creating an instance of this helper class is not allowed, results in {@link UnsupportedOperationException}.
   *
   * @throws UnsupportedOperationException on invocation
   */
  private FileMimeTypeHelper() {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the file mime type by guessing it for the file at the given filePath.
   *
   * @param filePath the path to the file to guess the mime type for.
   * @return the guessed mime type for the file, defaults to "application/octet-stream".
   * @throws NullPointerException if filePath is null.
   * @see URLConnection#guessContentTypeFromName(String)
   */
  public static @NonNull String fileType(@NonNull String filePath) {
    var mimeType = URLConnection.guessContentTypeFromName(filePath);
    return mimeType == null ? "application/octet-stream" : mimeType;
  }

  /**
   * Returns the file mime type by guessing it for the file at the given path.
   *
   * @param path the path to the file to guess the mime type for.
   * @return the guessed mime type for the file, defaults to "application/octet-stream".
   * @throws NullPointerException if path is null.
   * @see Files#probeContentType(Path)
   */
  public static @NonNull String fileType(@NonNull Path path) {
    try {
      var mimeType = Files.probeContentType(path);
      return mimeType == null ? "application/octet-stream" : mimeType;
    } catch (IOException exception) {
      return "application/octet-stream";
    }
  }
}
