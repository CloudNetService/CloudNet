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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import de.dytanic.cloudnet.common.log.LogRecordDispatcher;
import de.dytanic.cloudnet.common.log.Logger;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.LogRecord;
import lombok.NonNull;

public final class ThreadedLogRecordDispatcher extends Thread implements LogRecordDispatcher {

  public static final String THREAD_NAME_FORMAT = "Log record dispatcher %s";

  private final Logger logger;
  private final BlockingQueue<LogRecord> queue;

  private ThreadedLogRecordDispatcher(@NonNull Logger logger, @NonNull String threadName) {
    super(threadName);
    this.setDaemon(true);
    this.setPriority(Thread.MIN_PRIORITY);

    this.logger = logger;
    this.queue = new LinkedBlockingQueue<>();

    this.start();
  }

  public static @NonNull ThreadedLogRecordDispatcher forLogger(@NonNull Logger logger) {
    return ThreadedLogRecordDispatcher.newInstance(logger, String.format(THREAD_NAME_FORMAT, logger.getName()));
  }

  public static @NonNull ThreadedLogRecordDispatcher newInstance(@NonNull Logger logger, @NonNull String threadName) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(threadName), "Thread name must not be null or empty");
    return new ThreadedLogRecordDispatcher(logger, threadName);
  }

  @Override
  public void dispatchRecord(@NonNull LogRecord record) {
    if (!super.isInterrupted()) {
      this.queue.add(record);
    }
  }

  @Override
  public void run() {
    while (!super.isInterrupted()) {
      try {
        var logRecord = this.queue.take();
        this.logger.forceLog(logRecord);
      } catch (InterruptedException exception) {
        break;
      }
    }
    // log all waiting records now
    for (var logRecord : this.queue) {
      this.logger.forceLog(logRecord);
    }
  }
}
