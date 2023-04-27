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

package eu.cloudnetservice.common.function;

import java.util.function.Function;
import lombok.NonNull;

@FunctionalInterface
public interface TriFunction<T, U, V, R> {

  R apply(T t, U u, V v);

  default <K> @NonNull TriFunction<T, U, V, K> andThen(@NonNull Function<? super R, ? extends K> after) {
    return (T t, U u, V v) -> after.apply(this.apply(t, u, v));
  }
}