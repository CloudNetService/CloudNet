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

package eu.cloudnetservice.cloudnet.node.console.animation.progressbar;

import com.google.common.primitives.Longs;
import eu.cloudnetservice.cloudnet.common.function.ThrowableConsumer;
import eu.cloudnetservice.cloudnet.common.log.LogManager;
import eu.cloudnetservice.cloudnet.common.log.Logger;
import eu.cloudnetservice.cloudnet.node.CloudNet;
import eu.cloudnetservice.cloudnet.node.console.Console;
import eu.cloudnetservice.cloudnet.node.console.animation.progressbar.wrapper.WrappedInputStream;
import eu.cloudnetservice.cloudnet.node.console.animation.progressbar.wrapper.WrappedIterator;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import kong.unirest.Unirest;
import lombok.NonNull;

public final class ConsoleProgressWrappers {

  private static final Logger LOGGER = LogManager.logger(ConsoleProgressWrappers.class);

  private ConsoleProgressWrappers() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull <T> Iterator<T> wrapIterator(
    @NonNull Collection<T> collection,
    @NonNull String task,
    @NonNull String unitName
  ) {
    return wrapIterator(collection, CloudNet.instance().console(), task, unitName);
  }

  public static @NonNull <T> Iterator<T> wrapIterator(
    @NonNull Collection<T> collection,
    @NonNull Console console,
    @NonNull String task,
    @NonNull String unitName
  ) {
    return console.animationRunning() ? collection.iterator() : new WrappedIterator<>(
      collection.iterator(),
      console,
      ConsoleProgressAnimation.createDefault(task, unitName, 1, collection.size()));
  }

  public static void wrapDownload(@NonNull String url, @NonNull ThrowableConsumer<InputStream, IOException> handler) {
    wrapDownload(url, CloudNet.instance().console(), handler);
  }

  public static void wrapDownload(
    @NonNull String url,
    @NonNull Console console,
    @NonNull ThrowableConsumer<InputStream, IOException> streamHandler
  ) {
    Unirest
      .get(url)
      .connectTimeout(5000)
      .thenConsume(rawResponse -> {
        if (rawResponse.getStatus() == 200) {
          var stream = rawResponse.getContent();
          var contentLength = Longs.tryParse(rawResponse.getHeaders().getFirst("Content-Length"));

          try {
            streamHandler.accept(console.animationRunning() ? stream : new WrappedInputStream(
              stream,
              console,
              ConsoleProgressAnimation.createDefault(
                "Downloading",
                "MB",
                1024 * 1024,
                contentLength == null ? stream.available() : contentLength)));
          } catch (IOException exception) {
            LOGGER.severe("Exception downloading file from %s", exception, url);
          }
        }
      });
  }
}
