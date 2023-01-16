/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.ext.platforminject.runtime.util;

import java.util.function.Supplier;
import lombok.NonNull;

public final class LazyClassInstantiationUtil {

  private LazyClassInstantiationUtil() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull <T> Supplier<T> makeLazyLoader(@NonNull Class<?> caller, @NonNull String relativeName) {
    var fullName = String.format("%s.%s", caller.getPackageName(), relativeName);
    return makeLazyLoader(caller.getClassLoader(), fullName);
  }

  @SuppressWarnings("unchecked")
  public static @NonNull <T> Supplier<T> makeLazyLoader(@NonNull ClassLoader loader, @NonNull String className) {
    return () -> {
      try {
        // find the class and the no arg constructor of it
        var clazz = Class.forName(className, false, loader);
        var constructor = clazz.getDeclaredConstructor();

        // instantiate the class
        constructor.setAccessible(true);
        return (T) constructor.newInstance();
      } catch (ReflectiveOperationException exception) {
        throw new IllegalStateException("Unable to instantiate " + className + " with no-args constructor", exception);
      }
    };
  }
}
