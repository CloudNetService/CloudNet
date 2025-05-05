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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

public class CompletedTaskTest {

  @Test
  void testFutureCompletedNormally() {
    CompletableFuture<Integer> task = CompletableFuture.completedFuture(12345);

    Assertions.assertTrue(task.isDone());
    Assertions.assertEquals(12345, TaskUtil.getOrDefault(task, null));
    Assertions.assertDoesNotThrow((ThrowingSupplier<Integer>) task::get);

    var then = task.thenApply(i -> i == 12345 ? "Hello World" : "No world");
    Assertions.assertTrue(then.isDone());
    Assertions.assertEquals("Hello World", then.getNow(null));
    Assertions.assertDoesNotThrow((ThrowingSupplier<String>) then::get);

    var thenThen = then.thenApply(s -> {
      throw new RuntimeException(s.equals("Hello World") ? "Google" : "Bing");
    });
    Assertions.assertTrue(thenThen.isDone());
    Assertions.assertNull(thenThen.exceptionally(_ -> null).join());

    this.validateExceptionalResult(thenThen, RuntimeException.class, "Google");
  }

  private void validateExceptionalResult(CompletableFuture<?> task, Class<? extends Throwable> type, String message) {
    try {
      task.get();
      Assertions.fail();
    } catch (ExecutionException | InterruptedException exception) {
      Assertions.assertInstanceOf(ExecutionException.class, exception);
      Assertions.assertInstanceOf(type, exception.getCause());
      Assertions.assertEquals(message, exception.getCause().getMessage());
    }
  }
}
