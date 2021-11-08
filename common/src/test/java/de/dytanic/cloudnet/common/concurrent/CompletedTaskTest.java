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

package de.dytanic.cloudnet.common.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

public class CompletedTaskTest {

  @Test
  void testFutureCompletedNormally() {
    ITask<Integer> task = CompletedTask.done(12345);

    Assertions.assertTrue(task.isDone());
    Assertions.assertEquals(12345, task.getDef(null));
    Assertions.assertDoesNotThrow((ThrowingSupplier<Integer>) task::get);

    ITask<String> then = task.map(i -> i == 12345 ? "Hello World" : "No world");
    Assertions.assertTrue(then.isDone());
    Assertions.assertEquals("Hello World", then.getDef(null));
    Assertions.assertDoesNotThrow((ThrowingSupplier<String>) then::get);

    ITask<Double> thenThen = then.map(s -> {
      throw new RuntimeException(s.equals("Hello World") ? "Google" : "Bing");
    });
    Assertions.assertTrue(thenThen.isDone());
    Assertions.assertNull(thenThen.getDef(null));

    this.validateExceptionalResult(thenThen, RuntimeException.class, "Google");
  }

  @Test
  void testFutureCompletedCancelled() {
    ITask<Integer> task = CompletedTask.cancelled();

    Assertions.assertTrue(task.isDone());
    Assertions.assertTrue(task.isCancelled());
    Assertions.assertEquals(123, task.getDef(123));
    Assertions.assertThrows(CancellationException.class, task::get);

    ITask<String> then = task.map(i -> i == 12345 ? "Hello World" : "No world");
    Assertions.assertTrue(then.isDone());
    Assertions.assertTrue(then.isCancelled());
    Assertions.assertEquals("Hello World 1234", then.getDef("Hello World 1234"));
    Assertions.assertThrows(CancellationException.class, then::get);
  }

  @Test
  void testFutureCompletedExceptionally() {
    ITask<Float> task = CompletedTask.exceptionally(new UnsupportedOperationException("Hello World"));

    Assertions.assertTrue(task.isDone());
    this.validateExceptionalResult(task, UnsupportedOperationException.class, "Hello World");

    ITask<Void> then = task.map(r -> null);
    this.validateExceptionalResult(then, UnsupportedOperationException.class, "Hello World");
  }

  private void validateExceptionalResult(ITask<?> task, Class<? extends Throwable> expected, String expectedMessage) {
    try {
      task.get();
      Assertions.fail();
    } catch (ExecutionException | InterruptedException exception) {
      Assertions.assertInstanceOf(ExecutionException.class, exception);
      Assertions.assertInstanceOf(expected, exception.getCause());
      Assertions.assertEquals(expectedMessage, exception.getCause().getMessage());
    }
  }
}
