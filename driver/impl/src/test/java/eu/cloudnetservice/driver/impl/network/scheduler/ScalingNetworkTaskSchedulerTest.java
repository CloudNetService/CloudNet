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

package eu.cloudnetservice.driver.impl.network.scheduler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ScalingNetworkTaskSchedulerTest {

  @Test
  void testScalingOfSchedulerIfCoreThreadsAreBlocked() throws InterruptedException {
    var executedTasks = new CountDownLatch(5);
    var scheduler = new ScalingNetworkTaskScheduler(Executors.defaultThreadFactory(), 2);

    // block the two core threads of the scheduler for an infinite amount of time
    Runnable blockingAction = () -> {
      try {
        Thread.sleep(Long.MAX_VALUE);
      } catch (InterruptedException _) {
      }
    };
    scheduler.execute(blockingAction);
    scheduler.execute(blockingAction);

    // schedule more tasks into the thread pool than the wrapped core thread pool
    // can handle, this triggers the tasks to be put into the fallback scheduler
    for (var taskId = 0; taskId < 5; taskId++) {
      scheduler.execute(executedTasks::countDown);
    }

    var countReachedZero = executedTasks.await(30, TimeUnit.SECONDS);
    Assertions.assertTrue(countReachedZero);
  }
}
