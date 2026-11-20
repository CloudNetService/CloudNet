/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.utils.base;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LazyConstantSupplierTest {

  @Test
  void testValueIsComputedLazilyAndCached() {
    var expected = new Object();
    var fallback = new Object();
    var invocations = new AtomicInteger();
    var supplier = new LazyConstantSupplier<>(() -> {
      invocations.incrementAndGet();
      return expected;
    });

    Assertions.assertFalse(supplier.initialized());
    Assertions.assertSame(fallback, supplier.orElse(fallback));
    Assertions.assertEquals(0, invocations.get());
    Assertions.assertSame(expected, supplier.get());
    Assertions.assertSame(expected, supplier.get());
    Assertions.assertTrue(supplier.initialized());
    Assertions.assertSame(expected, supplier.orElse(fallback));
    Assertions.assertEquals(1, invocations.get());
  }

  @Test
  void testNullValueIsRejected() {
    var supplier = new LazyConstantSupplier<>(() -> null);
    var exception = Assertions.assertThrowsExactly(NullPointerException.class, supplier::get);
    Assertions.assertEquals("computed value", exception.getMessage());
    Assertions.assertFalse(supplier.initialized());
  }

  @Test
  void testRecursiveInitializationIsRejected() {
    var supplierReference = new AtomicReference<LazyConstantSupplier<Object>>();
    var supplier = new LazyConstantSupplier<>(() -> supplierReference.get().get());
    supplierReference.set(supplier);

    var exception = Assertions.assertThrowsExactly(IllegalStateException.class, supplier::get);
    Assertions.assertEquals("Initialization of LazyConstantSupplier is recursive", exception.getMessage());
  }

  @Test
  void testValueIsComputedOnceDuringConcurrentAccess() {
    var expected = new Object();
    var invocations = new AtomicInteger();
    var supplier = new LazyConstantSupplier<>(() -> {
      invocations.incrementAndGet();
      return expected;
    });

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var futures = IntStream.range(0, 1000)
        .mapToObj(_ -> CompletableFuture.supplyAsync(supplier, executor))
        .toList();
      CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
      futures.forEach(future -> Assertions.assertSame(expected, future.join()));
    }

    Assertions.assertEquals(1, invocations.get());
  }
}
