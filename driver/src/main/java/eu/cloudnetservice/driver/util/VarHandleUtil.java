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

package eu.cloudnetservice.driver.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * Small utility to make internal work with var handles easier.
 *
 * @since 4.0
 */
@ApiStatus.Internal
public final class VarHandleUtil {

  private VarHandleUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Finds a var handle for the field with the given name and type in the given class.
   *
   * @param lookup         the lookup for the class to find the handle in.
   * @param declaringClass the declaring class of the field to find the handle for.
   * @param name           the name of the field to find the handle for.
   * @param fieldType      the type of the field to find the handle for.
   * @return a var handle for the given field in the given class.
   * @throws NullPointerException  if the given lookup, declaring class, field name or field type is null.
   * @throws IllegalStateException if the target field is unknown or the given lookup instance has no access to it.
   */
  public static @NonNull VarHandle lookup(
    @NonNull MethodHandles.Lookup lookup,
    @NonNull Class<?> declaringClass,
    @NonNull String name,
    @NonNull Class<?> fieldType
  ) {
    try {
      return lookup.findVarHandle(declaringClass, name, fieldType);
    } catch (NoSuchFieldException | IllegalAccessException exception) {
      throw new IllegalStateException(String.format(
        "Unable to get VarHandle for field %s in %s",
        name,
        declaringClass.getName()));
    }
  }
}
