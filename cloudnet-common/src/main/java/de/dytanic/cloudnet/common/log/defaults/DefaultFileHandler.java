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

package de.dytanic.cloudnet.common.log.defaults;

import de.dytanic.cloudnet.common.io.FileUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import org.jetbrains.annotations.NotNull;

public final class DefaultFileHandler extends FileHandler {

  public static final int DEFAULT_COUNT = 8;
  public static final int DEFAULT_LIMIT = 1 << 25;

  private DefaultFileHandler(String pattern, int limit, int count, boolean append) throws IOException {
    super(pattern, limit, count, append);
    // default options
    this.setLevel(Level.ALL);
    this.setEncoding(StandardCharsets.UTF_8.name());
  }

  public static @NotNull DefaultFileHandler newInstance(@NotNull String pattern, boolean append) {
    return DefaultFileHandler.newInstance(pattern, DEFAULT_LIMIT, DEFAULT_COUNT, append);
  }

  public static @NotNull DefaultFileHandler newInstance(@NotNull String pattern, int limit, int count, boolean append) {
    return DefaultFileHandler.newInstance(Paths.get(pattern), limit, count, append);
  }

  public static @NotNull DefaultFileHandler newInstance(@NotNull Path pattern, boolean append) {
    return DefaultFileHandler.newInstance(pattern, DEFAULT_LIMIT, DEFAULT_COUNT, append);
  }

  public static @NotNull DefaultFileHandler newInstance(@NotNull Path pattern, int limit, int count, boolean append) {
    try {
      // check if the parent directory referenced by the pattern exists
      FileUtils.createDirectoryReported(pattern.getParent());
      return new DefaultFileHandler(pattern.toAbsolutePath().toString(), limit, count, append);
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to create file handler instance", exception);
    }
  }

  public @NotNull DefaultFileHandler withFormatter(@NotNull Formatter formatter) {
    super.setFormatter(formatter);
    return this;
  }
}
