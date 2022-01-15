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

package eu.cloudnetservice.modules.rest;

import com.google.common.collect.Iterables;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public final class RestUtil {

  private RestUtil() {
    throw new UnsupportedOperationException();
  }

  public static <T> @Nullable T first(@Nullable Iterable<T> iterable) {
    return first(iterable, null);
  }

  public static <T> @UnknownNullability T first(@Nullable Iterable<T> iterable, T def) {
    if (iterable == null) {
      return def;
    } else {
      return Iterables.getFirst(iterable, def);
    }
  }
}
