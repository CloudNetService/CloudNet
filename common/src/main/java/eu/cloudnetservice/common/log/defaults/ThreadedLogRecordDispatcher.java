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

package eu.cloudnetservice.common.log.defaults;

import eu.cloudnetservice.common.log.LogRecordDispatcher;
import eu.cloudnetservice.common.log.Logger;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.LogRecord;
import lombok.NonNull;

/**
 * A log record dispatcher which dispatches requested log records on a single thread one at a time.
 *
 * @since 4.0
 */
public final class ThreadedLogRecordDispatcher extends Thread implements LogRecordDispatcher {

  public static final String THREAD_NAME_FORMAT = "Log record dispatcher %s";

  private final Logger logger;
  private final BlockingQueue<LogRecord> queue;

  /**
   * Constructs a new threaded log record dispatcher instance. This automatically starts the thread.
   *
   * @param logger     the logger to which log records should get logged.
   * @param threadName the name of the thread to use.
   * @throws NullPointerException if the given logger or thread name is null.
   */
  private ThreadedLogRecordDispatcher(@NonNull Logger logger, @NonNull String threadName) {
    super(threadName);
    this.setDaemon(true);
    this.setPriority(Thread.MIN_PRIORITY);

    this.logger = logger;
    this.queue = new LinkedBlockingQueue<>();

    this.start();
  }

  /**
   * Creates a new threaded log record dispatcher instance using the given logger as the target and formatting the
   * default thread name format using the given loggers name.
   *
   * @param logger the logger this dispatcher should pump requests to.
   * @return a new threaded log record dispatcher instance.
   * @throws NullPointerException if the given logger is null.
   */
  public static @NonNull ThreadedLogRecordDispatcher forLogger(@NonNull Logger logger) {
    return ThreadedLogRecordDispatcher.newInstance(logger, String.format(THREAD_NAME_FORMAT, logger.getName()));
  }

  /**
   * Creates a new threaded log record dispatcher instance using the given logger as the target and the given thread
   * name.
   *
   * @param logger     the logger this dispatcher should pump requests to.
   * @param threadName the name of the dispatcher thread to use.
   * @return a new threaded log record dispatcher instance.
   * @throws NullPointerException if the given logger or thread name is null.
   */
  public static @NonNull ThreadedLogRecordDispatcher newInstance(@NonNull Logger logger, @NonNull String threadName) {
    return new ThreadedLogRecordDispatcher(logger, threadName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void dispatchRecord(@NonNull Logger logger, @NonNull LogRecord record) {
    if (!super.isInterrupted()) {
      this.queue.add(record);
    }
  }

  /**
   * {@inheritDoc}
   */
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
    // reset the interrupted state of the thread
    Thread.currentThread().interrupt();
  }
}
