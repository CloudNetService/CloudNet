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

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

public class TaskUtilTest {

  @Test
  @Timeout(10)
  void testFutureCompletedNormally() {
    var result = new AtomicInteger();
    var then = TaskUtil.supplyAsync(() -> {
      if (result.getAndIncrement() != 0) {
        throw new RuntimeException("Bing");
      }
      return "Google";
    }).thenApply(r -> {
      if (r.equals("Google")) {
        return result.incrementAndGet();
      } else {
        throw new IllegalStateException("Bing2");
      }
    }).thenApply(r -> {
      if (r != 2) {
        throw new UnsupportedOperationException("Bing3");
      } else {
        return 5f;
      }
    });

    Assertions.assertEquals(5f, then.join());
    Assertions.assertTrue(then.isDone());
  }

  @Test
  @Timeout(10)
  void testFutureExceptionalCompletion() {
    var listenerResult = new AtomicReference<Throwable>();

    var future = new CompletableFuture<>();
    future.exceptionally(throwable -> {
      listenerResult.set(throwable);
      return null;
    });

    Assertions.assertFalse(future.isDone());
    Assertions.assertNull(future.getNow(null));

    future.completeExceptionally(new UnsupportedOperationException("Hello world"));
    Assertions.assertTrue(future.isDone());
    Assertions.assertEquals("def", TaskUtil.getOrDefault(future, "def"));

    Assertions.assertNotNull(listenerResult.get());
    Assertions.assertInstanceOf(UnsupportedOperationException.class, listenerResult.get());
    Assertions.assertEquals("Hello world", listenerResult.get().getMessage());
  }

  @Test
  @Timeout(10)
  void testFutureTimeout() {
    var future = TaskUtil.supplyAsync(() -> {
      Thread.sleep(2000);
      return "success";
    });

    Assertions.assertFalse(future.isDone());
    Assertions.assertNull(TaskUtil.getOrDefault(future, Duration.ofSeconds(1), null));
    Assertions.assertEquals("success", TaskUtil.getOrDefault(future, null));
  }

  @Test
  @Timeout(10)
  void testFinishedFuture() {
    var future = TaskUtil.finishedFuture(new Throwable());
    Assertions.assertTrue(future.isDone());
    Assertions.assertTrue(future.isCompletedExceptionally());
    Assertions.assertEquals("def", TaskUtil.getOrDefault(future, "def"));

    var successfulFuture = TaskUtil.finishedFuture("result");
    Assertions.assertTrue(successfulFuture.isDone());
    Assertions.assertFalse(successfulFuture.isCompletedExceptionally());
    Assertions.assertEquals("result", TaskUtil.getOrDefault(successfulFuture, null));
  }
}
