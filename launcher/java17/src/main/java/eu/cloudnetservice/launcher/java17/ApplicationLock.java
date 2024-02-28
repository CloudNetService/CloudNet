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

package eu.cloudnetservice.launcher.java17;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import lombok.NonNull;

final class ApplicationLock {

  private FileLock lock;
  private FileChannel channel;

  public boolean acquireLock(@NonNull Path launcherDir) {
    try {
      // open the channel to the lock file and try to lock it
      this.channel = FileChannel.open(
        launcherDir.resolve("app.lock"),
        StandardOpenOption.CREATE,
        StandardOpenOption.WRITE);
      this.lock = this.channel.tryLock();
      // the returned lock must be exclusive, so "null" indicates that we were unable to lock the file
      return this.lock != null;
    } catch (IOException exception) {
      // should not happen, just explode
      throw new UncheckedIOException(exception);
    }
  }

  public void releaseLock() {
    try {
      // release and close the file channel
      this.lock.release();
      this.channel.close();
    } catch (Exception ignored) {
      // silently ignore exceptions thrown by this method because
      // they don't matter for the shutdown progress to advance.
    }
  }
}
