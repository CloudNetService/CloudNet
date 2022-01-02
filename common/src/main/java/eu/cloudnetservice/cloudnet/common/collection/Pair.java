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

package eu.cloudnetservice.cloudnet.common.collection;

import org.jetbrains.annotations.UnknownNullability;

/**
 * This class can capture 2 references of 2 types and set or clear the data using setFirst() / getFirst() and
 * setSecond() / getSecond(). It can be used to return multiple objects of a method, or to easily capture multiple
 * objects without creating their own class.
 *
 * @param <F> the first type, which you want to define
 * @param <S> the second type which you want to define
 */
public record Pair<F, S>(@UnknownNullability F first, @UnknownNullability S second) {

}
