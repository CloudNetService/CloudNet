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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

public class CompletableTaskTest {

  @Test
  @Timeout(10)
  void testFutureCompletedNormally() {
    var result = new AtomicInteger();
    var then = CompletableTask.supply(() -> {
      if (result.getAndIncrement() != 0) {
        throw new RuntimeException("Bing");
      }
      return "Google";
    }).map(r -> {
      if (r.equals("Google")) {
        return result.incrementAndGet();
      } else {
        throw new IllegalStateException("Bing2");
      }
    }).map(r -> {
      if (r != 2) {
        throw new UnsupportedOperationException("Bing3");
      } else {
        return 5f;
      }
    });

    Assertions.assertEquals(5f, then.getDef(0f));
    Assertions.assertTrue(then.isDone());
  }

  @Test
  @Timeout(10)
  void testFutureCompletion() {
    var future = new CompletableTask<String>();
    Assertions.assertFalse(future.isDone());
    Assertions.assertNull(future.getNow(null));

    future.complete("Hello world");
    Assertions.assertTrue(future.isDone());
    Assertions.assertEquals("Hello world", future.getNow(null));
  }

  @Test
  @Timeout(10)
  void testFutureCancellation() {
    var listenerCalled = new AtomicBoolean();

    var future = new CompletableTask<String>();
    future.onCancelled($ -> listenerCalled.set(true));

    Assertions.assertFalse(future.isDone());
    Assertions.assertNull(future.getNow(null));

    future.cancel(true);
    Assertions.assertTrue(future.isDone());
    Assertions.assertTrue(future.isCancelled());
    Assertions.assertTrue(listenerCalled.get());
    Assertions.assertThrows(CancellationException.class, future::get);
  }

  @Test
  @Timeout(10)
  void testFutureExceptionalCompletion() {
    var listenerResult = new AtomicReference<Throwable>();

    var future = new CompletableTask<String>();
    future.onFailure(listenerResult::set);

    Assertions.assertFalse(future.isDone());
    Assertions.assertNull(future.getNow(null));

    future.completeExceptionally(new UnsupportedOperationException("Hello world"));
    Assertions.assertTrue(future.isDone());
    Assertions.assertThrows(ExecutionException.class, future::get);

    Assertions.assertNotNull(listenerResult.get());
    Assertions.assertInstanceOf(UnsupportedOperationException.class, listenerResult.get());
    Assertions.assertEquals("Hello world", listenerResult.get().getMessage());
  }
}
