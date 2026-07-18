/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.node.impl.service.defaults.wrapper;

import eu.cloudnetservice.utils.base.io.FileUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.NonNull;

/**
 * Helper to unpack the wrapper file that is shaded into the current node jar.
 *
 * @since 4.0
 */
public final class WrapperFileProvider {

  private static final Lock WRAPPER_COPY_LOCK = new ReentrantLock();
  private static final Path WRAPPER_FILE_PATH = FileUtil.TEMP_DIR.resolve("caches").resolve("wrapper.jar");

  static {
    // ensure that the file gets deleted initially, so that the first
    // access of the wrapper always unpacks the latest version from the jar
    FileUtil.delete(WRAPPER_FILE_PATH);
  }

  private WrapperFileProvider() {
    throw new UnsupportedOperationException();
  }

  /**
   * Unpacks the wrapper jar if it doesn't exist on the file system already. If another thread is unpacking the file
   * already, the call blocks until the other thread completes the operation. The method returns the path to the
   * unpacked wrapper file.
   *
   * @return the path to the unpacked wrapper file.
   * @throws NullPointerException  if the wrapper.jar file is missing in the current node jar.
   * @throws IllegalStateException if the wrapper.jar cannot be unpacked to the file system.
   */
  public static @NonNull Path unpackWrapperFile() {
    if (Files.notExists(WRAPPER_FILE_PATH)) {
      WRAPPER_COPY_LOCK.lock();
      try {
        if (Files.notExists(WRAPPER_FILE_PATH)) {
          try (var stream = WrapperFileProvider.class.getClassLoader().getResourceAsStream("wrapper.jar")) {
            Objects.requireNonNull(stream, "Shaded \"wrapper.jar\" file missing, custom build?");
            Files.createDirectories(WRAPPER_FILE_PATH.getParent());
            Files.copy(stream, WRAPPER_FILE_PATH, StandardCopyOption.REPLACE_EXISTING);
          } catch (IOException exception) {
            throw new IllegalStateException("Unable to unpack shaded wrapper file", exception);
          }
        }
      } finally {
        WRAPPER_COPY_LOCK.unlock();
      }
    }

    return WRAPPER_FILE_PATH;
  }
}
