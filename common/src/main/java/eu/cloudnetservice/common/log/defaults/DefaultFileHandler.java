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

package eu.cloudnetservice.common.log.defaults;

import eu.cloudnetservice.common.io.FileUtil;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import lombok.NonNull;

/**
 * Represents the default file handler implementation of CloudNet, allowing more precise configuration of the resulting
 * log files which are created and then written to.
 *
 * @since 4.0
 */
public final class DefaultFileHandler extends FileHandler {

  public static final int DEFAULT_COUNT = 8;
  public static final int DEFAULT_LIMIT = 1 << 22;

  /**
   * Constructs a new default file handler instance.
   *
   * @param pattern the pattern for naming the output files.
   * @param limit   the maximum number of bytes to write to a log file.
   * @param count   the amount files to create before re-using the first one.
   * @param append  if the handler should append to the selected log file.
   * @throws IOException              if an I/O error occurs while opening the log files.
   * @throws NullPointerException     if the given naming pattern is null.
   * @throws IllegalArgumentException if pattern is empty, limit &lt; 0 or count &lt; 1.
   */
  private DefaultFileHandler(@NonNull String pattern, int limit, int count, boolean append) throws IOException {
    super(pattern, limit, count, append);
    // default options
    this.setLevel(Level.ALL);
    this.setEncoding(StandardCharsets.UTF_8.name());
  }

  /**
   * Constructs a new file handler using the given file name pattern and append settings. This method uses a maximum
   * file size of {@code 4194304} bytes and a maximum file count of 8.
   *
   * @param pattern the pattern for naming the output files.
   * @param append  if the handler should append to the selected log file.
   * @return the created file handler.
   * @throws IllegalStateException    if an I/O error occurs while opening the log files.
   * @throws NullPointerException     if the given naming pattern is null.
   * @throws IllegalArgumentException if pattern is empty, limit &lt; 0 or count &lt; 1.
   */
  public static @NonNull DefaultFileHandler newInstance(@NonNull String pattern, boolean append) {
    return DefaultFileHandler.newInstance(pattern, DEFAULT_LIMIT, DEFAULT_COUNT, append);
  }

  /**
   * Constructs a new file handler using the given file name pattern, limit, count and append settings.
   *
   * @param pattern the pattern for naming the output files.
   * @param limit   the maximum number of bytes to write to a log file.
   * @param count   the amount files to create before re-using the first one.
   * @param append  if the handler should append to the selected log file.
   * @return the created file handler.
   * @throws IllegalStateException    if an I/O error occurs while opening the log files.
   * @throws NullPointerException     if the given naming pattern is null.
   * @throws IllegalArgumentException if pattern is empty, limit &lt; 0 or count &lt; 1.
   */
  public static @NonNull DefaultFileHandler newInstance(@NonNull String pattern, int limit, int count, boolean append) {
    return DefaultFileHandler.newInstance(Path.of(pattern), limit, count, append);
  }

  /**
   * Constructs a new file handler using the given file name pattern and append settings. This method uses a maximum
   * file size of {@code 4194304} bytes and a maximum file count of 8.
   *
   * @param pattern the pattern for naming the output files.
   * @param append  if the handler should append to the selected log file.
   * @return the created file handler.
   * @throws IllegalStateException    if an I/O error occurs while opening the log files.
   * @throws NullPointerException     if the given naming pattern is null.
   * @throws IllegalArgumentException if pattern is empty, limit &lt; 0 or count &lt; 1.
   */
  public static @NonNull DefaultFileHandler newInstance(@NonNull Path pattern, boolean append) {
    return DefaultFileHandler.newInstance(pattern, DEFAULT_LIMIT, DEFAULT_COUNT, append);
  }

  /**
   * Constructs a new file handler using the given file name pattern, limit, count and append settings.
   *
   * @param pattern the pattern for naming the output files.
   * @param limit   the maximum number of bytes to write to a log file.
   * @param count   the amount files to create before re-using the first one.
   * @param append  if the handler should append to the selected log file.
   * @return the created file handler.
   * @throws IllegalStateException    if an I/O error occurs while opening the log files.
   * @throws NullPointerException     if the given naming pattern is null.
   * @throws IllegalArgumentException if pattern is empty, limit &lt; 0 or count &lt; 1.
   */
  public static @NonNull DefaultFileHandler newInstance(@NonNull Path pattern, int limit, int count, boolean append) {
    try {
      // check if the parent directory referenced by the pattern exists
      FileUtil.createDirectory(pattern.getParent());
      return new DefaultFileHandler(pattern.toAbsolutePath().toString(), limit, count, append);
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to create file handler instance", exception);
    }
  }

  /**
   * Sets the formatter of this handler and returns the same instance as used to call the method, for chaining.
   *
   * @param formatter the formatter to use for this handler.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given formatter is null.
   */
  public @NonNull DefaultFileHandler withFormatter(@NonNull Formatter formatter) {
    super.setFormatter(formatter);
    return this;
  }
}
