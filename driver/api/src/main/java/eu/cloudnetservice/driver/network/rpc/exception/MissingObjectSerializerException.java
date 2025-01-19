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

package eu.cloudnetservice.driver.network.rpc.exception;

import java.lang.reflect.Type;
import lombok.NonNull;

/**
 * An illegal state of the object serializer thrown when no serializer for the class to (de-) serialize is found.
 *
 * @since 4.0
 */
public class MissingObjectSerializerException extends IllegalStateException {

  /**
   * Constructs a new missing object serializer exception.
   *
   * @param type the type for which no serializer could be found.
   * @throws NullPointerException if the given type is null.
   */
  public MissingObjectSerializerException(@NonNull Type type) {
    super(String.format("Missing object type serializer for %s", type.getTypeName()));
  }
}
