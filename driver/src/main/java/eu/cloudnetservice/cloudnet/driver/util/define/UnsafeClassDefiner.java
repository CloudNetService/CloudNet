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

import java.lang.reflect.Method;
import lombok.NonNull;

/**
 * A class defining method for legacy jvm implementations (Java 7 - 14) which is deprecated since Java 15 in honor of
 * the Lookup class defining method. This method uses the {@code defineAnonymousClass} class provided by the jvm
 * internal {@code Unsafe} class.
 *
 * @author Pasqual K.
 * @since 1.0
 */
final class UnsafeClassDefiner implements ClassDefiner {

  /**
   * The method which allows the access to the {@code defineAnonymousClass} method in {@code Unsafe}.
   */
  private static final Method DEFINE_ANONYMOUS_CLASS;

  static {
    Method defineAnonymousClass = null;

    if (UnsafeAccess.available()) {
      try {
        // find the define method
        var defineAnonymousClassMethod = UnsafeAccess.UNSAFE_CLASS.getDeclaredMethod("defineAnonymousClass",
          Class.class,
          byte[].class,
          Object[].class);
        defineAnonymousClassMethod.setAccessible(true);
        // assign the method
        defineAnonymousClass = defineAnonymousClassMethod;
      } catch (Throwable ignored) {
      }
    }
    // assign the class fields
    DEFINE_ANONYMOUS_CLASS = defineAnonymousClass;
  }

  /**
   * Checks if the {@code defineAnonymousClass} is available and this defining method can be used.
   *
   * @return if the {@code defineAnonymousClass} is available and this defining method can be used.
   */
  public static boolean available() {
    return DEFINE_ANONYMOUS_CLASS != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Class<?> defineClass(@NonNull String name, @NonNull Class<?> parent, byte[] bytecode) {
    try {
      // Use unsafe to define the class
      return (Class<?>) DEFINE_ANONYMOUS_CLASS.invoke(UnsafeAccess.THE_UNSAFE_INSTANCE, parent, bytecode, null);
    } catch (Throwable throwable) {
      throw new IllegalStateException("Unable to define class " + name, throwable);
    }
  }
}
