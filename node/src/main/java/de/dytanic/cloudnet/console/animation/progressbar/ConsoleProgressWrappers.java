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

package de.dytanic.cloudnet.console.animation.progressbar;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.io.HttpConnectionProvider;
import de.dytanic.cloudnet.console.IConsole;
import de.dytanic.cloudnet.console.animation.progressbar.wrapper.WrappedInputStream;
import de.dytanic.cloudnet.console.animation.progressbar.wrapper.WrappedIterator;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Iterator;
import org.jetbrains.annotations.NotNull;

public final class ConsoleProgressWrappers {

  private ConsoleProgressWrappers() {
    throw new UnsupportedOperationException();
  }

  public static @NotNull <T> Iterator<T> wrapIterator(
    @NotNull Collection<T> collection,
    @NotNull String task,
    @NotNull String unitName
  ) {
    return wrapIterator(collection, CloudNet.getInstance().getConsole(), task, unitName);
  }

  public static @NotNull <T> Iterator<T> wrapIterator(
    @NotNull Collection<T> collection,
    @NotNull IConsole console,
    @NotNull String task,
    @NotNull String unitName
  ) {
    return console.isAnimationRunning() ? collection.iterator() : new WrappedIterator<>(
      collection.iterator(),
      console,
      new ConsoleProgressAnimation(
        '█',
        ' ',
        " ▏▎▍▌▋▊▉",
        '[',
        ']',
        task + " (%ratio%): %percent%  ",
        " %speed% (%elapsed%/%eta%)",
        1,
        unitName,
        new DecimalFormat("#.0"),
        collection.size()));
  }

  public static @NotNull InputStream wrapDownload(@NotNull String url) throws IOException {
    return wrapDownload(url, CloudNet.getInstance().getConsole());
  }

  public static @NotNull InputStream wrapDownload(@NotNull String url, @NotNull IConsole console) throws IOException {
    HttpURLConnection connection = HttpConnectionProvider.provideConnection(url);
    connection.setUseCaches(false);
    connection.connect();

    InputStream stream = connection.getInputStream();
    long contentLength = connection.getHeaderFieldLong("Content-Length", stream.available());

    return console.isAnimationRunning() ? stream : new WrappedInputStream(stream, console, new ConsoleProgressAnimation(
      '█',
      ' ',
      " ▏▎▍▌▋▊▉",
      '[',
      ']',
      "Downloading (%ratio%): %percent%  ",
      " %speed% (%elapsed%/%eta%)",
      1024 * 1024,
      "MB",
      new DecimalFormat("#.0"),
      contentLength));
  }
}
