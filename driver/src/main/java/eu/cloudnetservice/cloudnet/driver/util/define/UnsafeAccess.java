/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.driver.util.define;

/**
 * Gives access to the Unsafe class of the jvm which allows access to previously inaccessible magic.
 *
 * @since 4.0
 */
final class UnsafeAccess {

  /**
   * The unsafe class object if present.
   */
  static final Class<?> UNSAFE_CLASS;
  /**
   * The jvm static Unsafe instance if present.
   */
  static final Object THE_UNSAFE_INSTANCE;

  static {
    Class<?> unsafeClass = null;
    Object theUnsafeInstance = null;

    try {
      // get the unsafe class
      unsafeClass = Class.forName("sun.misc.Unsafe");
      // get the unsafe instance
      var theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
      theUnsafeField.setAccessible(true);
      theUnsafeInstance = theUnsafeField.get(null);
    } catch (Exception ignored) {
    }
    // assign to the static final fields
    UNSAFE_CLASS = unsafeClass;
    THE_UNSAFE_INSTANCE = theUnsafeInstance;
  }

  /**
   * Creating an instance of this helper class is not allowed, results in {@link UnsupportedOperationException}.
   *
   * @throws UnsupportedOperationException on invocation
   */
  private UnsafeAccess() {
    throw new UnsupportedOperationException();
  }

  /**
   * Get if the Unsafe class and instance were successfully loaded.
   *
   * @return if the Unsafe class and instance were successfully loaded.
   */
  static boolean available() {
    return UNSAFE_CLASS != null && THE_UNSAFE_INSTANCE != null;
  }
}
