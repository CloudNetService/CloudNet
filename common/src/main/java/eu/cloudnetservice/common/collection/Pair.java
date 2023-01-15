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

package eu.cloudnetservice.common.collection;

import org.jetbrains.annotations.UnknownNullability;

/**
 * This pair wraps two values, that are allowed to have different types, into a pair allowing accessing both using
 * {@link #first()} and {@link #second()}.
 *
 * @param first the first value of the pair.
 * @param second the second value of the pair.
 * @param <F> the type of the first value.
 * @param <S> the type of the second value.
 * @since 4.0
 */
public record Pair<F, S>(@UnknownNullability F first, @UnknownNullability S second) {

}
