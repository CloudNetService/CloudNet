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

package eu.cloudnetservice.node.service.defaults.log;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.inject.Singleton;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.NonNull;

@Singleton
public final class ProcessServiceLogReadScheduler {

  private static final int LOG_READ_DELAY_MS = Integer.getInteger("cloudnet.process-log-read-delay", 25);
  private static final int READ_WORKER_MAXIMUM = Integer.getInteger("cloudnet.process-log-worker-maximum", 25);
  private static final int READ_ACTIONS_PER_WORKER = Integer.getInteger("cloudnet.process-log-actions-per-worker", 5);

  private final AtomicInteger runningReaderActions;
  private final ScheduledThreadPoolExecutor executor;

  public ProcessServiceLogReadScheduler() {
    var threadFactory = new ThreadFactoryBuilder()
      .setDaemon(true)
      .setPriority(Thread.NORM_PRIORITY)
      .setNameFormat("process-log-reader-%d")
      .build();
    this.executor = new ScheduledThreadPoolExecutor(1, threadFactory, new ThreadPoolExecutor.DiscardPolicy());
    this.runningReaderActions = new AtomicInteger(0);
  }

  public void schedule(@NonNull ProcessServiceLogCache logCache) {
    var runningReaderActions = this.runningReaderActions.getAndIncrement();
    if (runningReaderActions != 0 && runningReaderActions % READ_ACTIONS_PER_WORKER == 0) {
      var expectedWorkerCount = (runningReaderActions / READ_ACTIONS_PER_WORKER) + 1;
      this.adjustWorkerCount(expectedWorkerCount);
    }

    var readTask = new ProcessServiceLogReadTask(logCache, this);
    this.executor.scheduleWithFixedDelay(readTask, 0, LOG_READ_DELAY_MS, TimeUnit.MILLISECONDS);
  }

  private void notifyLogCacheReadEnd() {
    var runningReaderActions = this.runningReaderActions.decrementAndGet();
    if (runningReaderActions != 0 && runningReaderActions % READ_ACTIONS_PER_WORKER == 0) {
      var expectedWorkerCount = runningReaderActions / READ_ACTIONS_PER_WORKER;
      this.adjustWorkerCount(expectedWorkerCount);
    }
  }

  private void adjustWorkerCount(int expectedWorkerCount) {
    var newCorePoolSize = Math.min(expectedWorkerCount, READ_WORKER_MAXIMUM);
    if (this.executor.getCorePoolSize() != newCorePoolSize) {
      this.executor.setCorePoolSize(expectedWorkerCount);
    }
  }

  private record ProcessServiceLogReadTask(
    @NonNull ProcessServiceLogCache logCache,
    @NonNull ProcessServiceLogReadScheduler scheduler
  ) implements Runnable {

    private static final RuntimeException CANCEL_EXCEPTION = new RuntimeException("cancelled, reached stream EOF");

    @Override
    public void run() {
      // read the content from the stream, in case the stream closed notify the
      // scheduler about this and stop scheduling the next by throwing an exception
      var streamsStillOpen = this.logCache.readProcessOutputContent();
      if (!streamsStillOpen) {
        this.scheduler.notifyLogCacheReadEnd();
        throw CANCEL_EXCEPTION;
      }
    }
  }
}
