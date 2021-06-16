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

package de.dytanic.cloudnet.common.logging;

import de.dytanic.cloudnet.common.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.jetbrains.annotations.NotNull;

/**
 * A standard file logger for this LoggingAPI. All important configurations can be made in the constructor
 */
public final class DefaultFileLogHandler extends AbstractLogHandler {

  public static final long SIZE_8MB = 8 * 1024 * 1024;

  private final Path directory;
  private final String pattern;
  private final long maxBytes;

  private Path entry;
  private OutputStream outputStream;
  private long writtenBytes = 0L;

  private Path errorFile;
  private OutputStream errorWriter;
  private long writtenErrorBytes = 0L;

  /**
   * The default constructor with all important configuration
   *
   * @param directory the default storage for the log files
   * @param pattern   the file pattern, for the log files like "app.log" will be to than "app.log.0"
   * @param maxBytes  the maximum bytes, that a log file should have, to switch to the next log file
   */
  @Deprecated
  public DefaultFileLogHandler(File directory, String pattern, long maxBytes) {
    this(directory.toPath(), pattern, maxBytes);
  }

  public DefaultFileLogHandler(Path directory, String pattern, long maxBytes) {
    if (directory == null) {
      directory = Paths.get(System.getProperty("cloudnet.logging.fallback.log.directory", "logs"));
    }

    this.directory = directory;
    FileUtils.createDirectoryReported(this.directory);

    this.pattern = pattern;
    this.maxBytes = maxBytes;

    this.entry = this.init(this.selectLogFile(null, this.pattern));
  }

  /**
   * Enables/disables the error log file (directory/error.log)
   *
   * @param enableErrorLog if the file should be created and filled with every error in the console
   */
  public DefaultFileLogHandler setEnableErrorLog(boolean enableErrorLog) throws IOException {
    if (enableErrorLog && this.errorWriter == null) {
      this.errorFile = this.initErrorWriter(this.selectLogFile(null, "error.%d.log"));
      this.errorWriter = Files.newOutputStream(this.errorFile);
    } else if (!enableErrorLog && this.errorWriter != null) {
      this.errorWriter.close();
      this.errorWriter = null;
    }
    return this;
  }

  @Override
  public void handle(@NotNull LogEntry logEntry) throws Exception {
    if (this.outputStream == null) {
      // handler is not available
      return;
    }

    if (this.entry == null || Files.size(this.entry) > this.maxBytes) {
      this.entry = this.init(this.selectLogFile(this.outputStream, this.pattern));
    }

    String formatted = this.getFormatter().format(logEntry);
    byte[] formattedBytes = formatted.getBytes(StandardCharsets.UTF_8);
    this.writtenBytes += formattedBytes.length;

    if (this.writtenBytes > this.maxBytes) {
      this.entry = this.init(this.selectLogFile(this.outputStream, this.pattern));
      this.writtenBytes = 0;
    }

    if (this.writeTo(this.outputStream, formattedBytes)) {
      this.entry = this.init(this.selectLogFile(this.outputStream, this.pattern));
      this.writtenBytes = 0;
    }

    if (this.errorWriter != null && logEntry.getLogLevel().getLevel() >= 126
      && logEntry.getLogLevel().getLevel() <= 127) {
      if (this.errorFile == null || Files.size(this.errorFile) > this.maxBytes) {
        this.errorFile = this.initErrorWriter(this.selectLogFile(this.errorWriter, "error.%d.log"));
      }

      this.writtenErrorBytes += formattedBytes.length;
      if (this.writtenErrorBytes > this.maxBytes) {
        this.errorFile = this.initErrorWriter(this.selectLogFile(this.errorWriter, "error.%d.log"));
        this.writtenErrorBytes = 0;
      }

      if (this.writeTo(this.errorWriter, formattedBytes)) {
        this.errorFile = this.initErrorWriter(this.selectLogFile(this.errorWriter, "error.log"));
        this.writtenErrorBytes = 0;
      }
    }
  }

  @Override
  public void close() throws Exception {
    if (this.outputStream != null) {
      this.outputStream.flush();
      this.outputStream.close();
      this.outputStream = null;
    }
    if (this.errorWriter != null) {
      this.errorWriter.flush();
      this.errorWriter.close();
      this.errorWriter = null;
    }
  }

  public Path getDirectory() {
    return this.directory;
  }

  public String getPattern() {
    return this.pattern;
  }

  public long getMaxBytes() {
    return this.maxBytes;
  }

  public Path getEntry() {
    return this.entry;
  }

  public long getWrittenBytes() {
    return this.writtenBytes;
  }

  private Path selectLogFile(OutputStream currentTargetStream, String pattern) {
    if (currentTargetStream != null) {
      try {
        currentTargetStream.close();
      } catch (IOException exception) {
        exception.printStackTrace();
      }
    }

    Path path;
    int index = 0;
    while (true) {
      try {
        path = this.directory.resolve(String.format(pattern, index++));
        if (Files.notExists(path)) {
          Files.createFile(path);
          return path;
        }
        if (!Files.isDirectory(path) && Files.size(path) < this.maxBytes) {
          return path;
        }
      } catch (IOException exception) {
        exception.printStackTrace();
      }
    }
  }

  private Path init(Path file) {
    try {
      this.outputStream = Files.newOutputStream(file, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    } catch (IOException exception) {
      exception.printStackTrace();
    }
    return file;
  }

  private Path initErrorWriter(Path file) {
    try {
      this.errorWriter = Files.newOutputStream(file, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    } catch (IOException exception) {
      exception.printStackTrace();
    }
    return file;
  }

  private boolean writeTo(OutputStream target, byte[] content) {
    try {
      target.write(content);
      target.flush();
      return false;
    } catch (IOException exception) {
      return true;
    }
  }
}
