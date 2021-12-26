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

package eu.cloudnetservice.launcher.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.BodySubscriber;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import lombok.NonNull;

public final class HttpUtils {

  private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
    .followRedirects(Redirect.NORMAL)
    .connectTimeout(Duration.ofSeconds(20))
    .build();
  private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/97.0.4692.56 Safari/537.36";

  private HttpUtils() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull <T> HttpResponse<T> get(@NonNull URI url, @NonNull BodyHandler<T> body) throws Exception {
    var request = HttpRequest.newBuilder()
      .GET()
      .uri(url)
      .timeout(Duration.ofSeconds(5))
      .header("user-agent", USER_AGENT)
      .build();
    return HTTP_CLIENT.send(request, body);
  }

  public static @NonNull BodyHandler<Path> handlerForFile(@NonNull Path filePath) {
    return BodyHandlers.ofFile(
      filePath,
      StandardOpenOption.CREATE,
      StandardOpenOption.WRITE,
      StandardOpenOption.TRUNCATE_EXISTING);
  }

  public static @NonNull <T> BodyHandler<T> handlerFromSubscriber(@NonNull BodySubscriber<T> subscriber) {
    return info -> {
      // only apply the subscriber for successful responses
      if (info.statusCode() == 200) {
        return subscriber;
      } else {
        throw new IllegalStateException("Expected response code 200, got " + info.statusCode());
      }
    };
  }
}
