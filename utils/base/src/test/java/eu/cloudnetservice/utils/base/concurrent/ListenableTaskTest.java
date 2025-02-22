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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ListenableTaskTest {

  @Test
  void testTaskRunAndReset() {
    var handlerCalled = new AtomicBoolean();

    var task = new ListenableTask<>(() -> "Hello World");
    task.thenAccept($ -> handlerCalled.set(true));

    task.run(false);
    Assertions.assertFalse(task.isDone());
    Assertions.assertFalse(handlerCalled.get());

    task.run(true);
    Assertions.assertTrue(task.isDone());
    Assertions.assertEquals("Hello World", TaskUtil.getOrDefault(task, null));
    Assertions.assertTrue(handlerCalled.get());
  }

  @Test
  void testTaskRunAndResetExceptionTerminatesInstantly() {
    var handlerResult = new AtomicReference<Throwable>();

    var task = new ListenableTask<String>(() -> {
      throw new RuntimeException("Hello World");
    });
    task.exceptionally(th -> {
      handlerResult.set(th);
      return null;
    });

    task.run(false);
    Assertions.assertTrue(task.isDone());
    Assertions.assertNotNull(handlerResult.get());
    Assertions.assertInstanceOf(RuntimeException.class, handlerResult.get());
    Assertions.assertEquals("Hello World", handlerResult.get().getMessage());
  }
}
