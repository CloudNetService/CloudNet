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

package eu.cloudnetservice.wrapper.impl.transform.unsafe;

import java.lang.invoke.MethodHandles;
import java.util.function.Supplier;

/**
 * Various constants used by unsafe replacement operations.
 *
 * @since 4.0
 */
final class OpConstants {

  /**
   * Supplier of the jvm-static trusted lookup instance, only initialized on the first access.
   */
  static final Supplier<MethodHandles.Lookup> TRUSTED_LOOKUP = StableValue.supplier(() -> {
    try {
      var trustedLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
      trustedLookupField.setAccessible(true);
      return (MethodHandles.Lookup) trustedLookupField.get(null);
    } catch (NoSuchFieldException | IllegalAccessException exception) {
      throw new ExceptionInInitializerError(exception);
    }
  });

  /**
   * Plain access mode without any memory ordering guarantees. Supported by get and put operations.
   */
  static final int MODE_PLAIN = 0;
  /**
   * Volatile access mode. Supported by get and put operations.
   */
  static final int MODE_VOLATILE = 1;
  /**
   * Release access mode (ordered put). Only supported by put operations.
   */
  static final int MODE_RELEASE = 2;

  private OpConstants() {
    throw new UnsupportedOperationException();
  }
}
