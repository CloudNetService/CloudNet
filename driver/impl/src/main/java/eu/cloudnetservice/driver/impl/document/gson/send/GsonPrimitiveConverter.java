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

package eu.cloudnetservice.driver.impl.document.gson.send;

import com.google.gson.JsonPrimitive;
import eu.cloudnetservice.driver.document.send.element.PrimitiveElement;
import lombok.NonNull;

/**
 * A class to simple convert between gson primitive types, java primitives or primitive elements.
 *
 * @since 4.0
 */
final class GsonPrimitiveConverter {

  private GsonPrimitiveConverter() {
    throw new UnsupportedOperationException();
  }

  /**
   * Wraps the given primitive type into a gson json primitive instance.
   *
   * @param value the java primitive type to wrap into a gson value.
   * @return the gson json primitive instance wrapping the given value.
   * @throws NullPointerException     if the given value is null.
   * @throws IllegalArgumentException if the given value type is not a json primitive.
   */
  public static @NonNull JsonPrimitive wrapAsPrimitive(@NonNull Object value) {
    if (value instanceof String string) {
      return new JsonPrimitive(string);
    }

    if (value instanceof Number number) {
      return new JsonPrimitive(number);
    }

    if (value instanceof Boolean bool) {
      return new JsonPrimitive(bool);
    }

    if (value instanceof Character c) {
      return new JsonPrimitive(c);
    }

    throw new IllegalArgumentException(
      "Invalid primitive type " + value.getClass() + " must be one of: String, Number, Boolean, Character");
  }

  /**
   * Converts the given gson json primitive into a primitive element.
   *
   * @param key       the key to use for the primitive element.
   * @param primitive the gson json primitive value to unwrap and re-pack into a primitive element.
   * @return a primitive element constructed from the given key and gson json primitive.
   * @throws NullPointerException     if the given key or gson json primitive is null.
   * @throws IllegalArgumentException if the given gson json primitive contains an unknown json primitive type.
   */
  public static @NonNull PrimitiveElement unwrapJsonPrimitive(@NonNull String key, @NonNull JsonPrimitive primitive) {
    if (primitive.isString()) {
      return new PrimitiveElement(key, primitive.getAsString());
    }

    if (primitive.isNumber()) {
      return new PrimitiveElement(key, primitive.getAsNumber());
    }

    if (primitive.isBoolean()) {
      return new PrimitiveElement(key, primitive.getAsBoolean());
    }

    throw new IllegalArgumentException("Unable to read inner value of " + primitive);
  }
}
