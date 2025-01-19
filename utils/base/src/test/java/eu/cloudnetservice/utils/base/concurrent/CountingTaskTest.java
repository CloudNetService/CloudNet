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

package eu.cloudnetservice.utils.base.concurrent;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CountingTaskTest {

  @Test
  void testCountingTask() {
    var listenerResult = new AtomicInteger();

    var task = new CountingTask<>(12345, 3);
    task.thenAccept(listenerResult::set);

    this.assertiveCountDown(task);
    this.assertiveCountDown(task);

    task.countDown();
    Assertions.assertTrue(task.isDone());
    Assertions.assertEquals(12345, task.getNow(null));
    Assertions.assertEquals(12345, listenerResult.get());
  }

  private void assertiveCountDown(CountingTask<?> task) {
    task.countDown();
    Assertions.assertFalse(task.isDone());
    Assertions.assertNull(task.getNow(null));
  }
}
