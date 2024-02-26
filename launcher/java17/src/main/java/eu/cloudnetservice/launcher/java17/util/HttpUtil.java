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

package eu.cloudnetservice.launcher.java17.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import lombok.NonNull;

public final class HttpUtil {

  private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
    .followRedirects(HttpClient.Redirect.NORMAL)
    .connectTimeout(Duration.ofSeconds(20))
    .build();
  private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    + "(KHTML, like Gecko) Chrome/97.0.4692.56 Safari/537.36";

  private HttpUtil() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull <T> HttpResponse<T> get(
    @NonNull URI url,
    @NonNull HttpResponse.BodyHandler<T> body
  ) throws Exception {
    var request = HttpRequest.newBuilder()
      .GET()
      .uri(url)
      .timeout(Duration.ofMinutes(1))
      .header("user-agent", USER_AGENT)
      .build();
    return HTTP_CLIENT.send(request, body);
  }

  public static @NonNull HttpResponse.BodyHandler<Path> handlerForFile(@NonNull Path filePath) {
    // we need to create the directory ourselves if it doesn't exist yet
    var parent = filePath.getParent();
    if (parent != null && Files.notExists(parent)) {
      try {
        Files.createDirectories(parent);
      } catch (IOException exception) {
        // fail-fast here, no need to download as the file write will fail as well
        throw new UncheckedIOException("Unable to create directory " + parent, exception);
      }
    }
    // redirect to BodyHandlers.ofFile with a few options set by default
    return HttpResponse.BodyHandlers.ofFile(
      filePath,
      StandardOpenOption.CREATE,
      StandardOpenOption.WRITE,
      StandardOpenOption.TRUNCATE_EXISTING);
  }
}
