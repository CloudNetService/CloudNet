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

import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodHandles.Lookup.ClassOption;
import java.lang.reflect.Field;
import lombok.NonNull;

/**
 * A class definer for modern jvm implementation (Java 15+) which makes use of the newly added defineHiddenClass method
 * in the Lookup class.
 *
 * @since 4.0
 */
final class LookupClassDefiner implements ClassDefiner {

  /**
   * The jvm trusted lookup instance. It allows access to every lookup even if the access to these classes is denied for
   * the current module.
   */
  private static final Lookup TRUSTED_LOOKUP;

  static {
    Lookup trustedLookup = null;

    if (UnsafeAccess.available()) {
      try {
        // get the trusted lookup field
        var implLookup = Lookup.class.getDeclaredField("IMPL_LOOKUP");
        // get the lookup base and offset
        var base = UnsafeAccess.UNSAFE_CLASS
          .getMethod("staticFieldBase", Field.class)
          .invoke(UnsafeAccess.THE_UNSAFE_INSTANCE, implLookup);
        var offset = (long) UnsafeAccess.UNSAFE_CLASS
          .getMethod("staticFieldOffset", Field.class)
          .invoke(UnsafeAccess.THE_UNSAFE_INSTANCE, implLookup);
        // get the trusted lookup from the field
        trustedLookup = (Lookup) UnsafeAccess.UNSAFE_CLASS
          .getMethod("getObject", Object.class, long.class)
          .invoke(UnsafeAccess.THE_UNSAFE_INSTANCE, base, offset);
      } catch (Throwable ignored) {
      }
    }
    // set the static final fields
    TRUSTED_LOOKUP = trustedLookup;
  }

  /**
   * Get if the lookup class definer requirements are met to use the definer in the current jvm.
   *
   * @return if the lookup class definer requirements are met to use the definer in the current jvm.
   */
  public static boolean available() {
    return TRUSTED_LOOKUP != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Class<?> defineClass(@NonNull String name, @NonNull Class<?> parent, byte[] bytecode) {
    try {
      return TRUSTED_LOOKUP.in(parent).defineHiddenClass(bytecode, false, ClassOption.NESTMATE).lookupClass();
    } catch (Throwable throwable) {
      throw new IllegalStateException("Exception defining class " + name, throwable);
    }
  }
}
