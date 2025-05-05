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

package eu.cloudnetservice.node.impl.console.animation.progressbar;

import com.google.common.primitives.Longs;
import eu.cloudnetservice.node.impl.console.Console;
import eu.cloudnetservice.node.impl.console.animation.progressbar.wrapper.WrappedInputStream;
import eu.cloudnetservice.node.impl.console.animation.progressbar.wrapper.WrappedIterator;
import io.vavr.CheckedConsumer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import kong.unirest.core.Unirest;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public final class ConsoleProgressWrappers {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleProgressWrappers.class);
  // the log
  private static final double LOG_10_FILE_SIZE_BASE = 3.010299956639812;
  private static final String[] FILE_SIZE_UNIT_NAMES = new String[]{"B", "KB", "MB", "GB", "TB", "PB", "EB"};

  private final Console console;

  @Inject
  public ConsoleProgressWrappers(@NonNull Console console) {
    this.console = console;
  }

  public @NonNull <T> Iterator<T> wrapIterator(
    @NonNull Collection<T> collection,
    @NonNull String task,
    @NonNull String unitName
  ) {
    return this.wrapIterator(collection, this.console, task, unitName);
  }

  public @NonNull <T> Iterator<T> wrapIterator(
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

  public void wrapDownload(@NonNull String url, @NonNull CheckedConsumer<InputStream> streamHandler) {
    Unirest
      .get(url)
      .requestTimeout(5000)
      .thenConsume(rawResponse -> {
        if (rawResponse.getStatus() == 200) {
          var stream = rawResponse.getContent();
          var contentLength = Longs.tryParse(rawResponse.getHeaders().getFirst("Content-Length"));

          try {
            // gets the unit multiplier, 1 represents kb, 2 mb etc
            var contentSize = contentLength == null ? stream.available() : contentLength;
            var unitMultiplier = (int) (Math.log10(contentSize) / LOG_10_FILE_SIZE_BASE);

            streamHandler.accept(this.console.animationRunning() ? stream : new WrappedInputStream(
              stream,
              this.console,
              ConsoleProgressAnimation.createDefault(
                "Downloading",
                FILE_SIZE_UNIT_NAMES[unitMultiplier],
                (int) Math.pow(1024, unitMultiplier),
                contentSize)));
          } catch (Throwable exception) {
            LOGGER.error("Exception downloading file from {}", url, exception);
          }
        }
      });
  }
}
