/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.common.tuple;

import org.jetbrains.annotations.UnknownNullability;

/**
 * A pair consisting of two elements: the first and second one. Both of these elements are allowed to be null. While the
 * tuple implementation itself is immutable, there is no restriction if a type stored in a pair is immutable or not. If
 * mutable values are stored in a pair the pair itself effectively becomes mutable.
 *
 * @param first  the first value of the tuple.
 * @param second the second value of the tuple.
 * @param <F>    the first element type.
 * @param <S>    the second element type.
 * @since 4.0
 */
public record Tuple2<F, S>(@UnknownNullability F first, @UnknownNullability S second) {

}
