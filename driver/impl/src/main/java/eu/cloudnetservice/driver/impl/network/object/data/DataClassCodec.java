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

package eu.cloudnetservice.driver.impl.network.object.data;

import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.object.ObjectMapper;
import lombok.NonNull;

/**
 * The blueprint of the internally generated class to serialize & deserialize the fields of an object into a buffer.
 *
 * @since 4.0
 */
public interface DataClassCodec {

  /**
   * Deserializes the target object of this codec from the given buffer using the given object mapper to deserialize the
   * field values. This method will call the all-args constructor of the target class with the deserialized arguments
   * and is not implemented for subclasses.
   *
   * @param source the source from which the class field data should be read.
   * @param mapper the mapper to use to deserialize field values.
   * @return the constructed object using the deserialized values and colling the all-args constructor.
   * @throws NullPointerException if the given source or mapper is null.
   */
  @NonNull
  Object deserialize(@NonNull DataBuf source, @NonNull ObjectMapper mapper);

  /**
   * Serializes the target object of this codec from the given instance into the given buffer using the given object
   * mapper to serialize the field values. Each class in the hierarchy of the root target has it's only serializer to
   * allow private field serialization.
   *
   * @param target   the target buffer into which the field values should be serialized.
   * @param mapper   the object mapper to use to serialize the field values.
   * @param instance the object instance that should be serialized.
   * @throws NullPointerException if the given target buffer, object mapper or instance is null.
   */
  void serialize(@NonNull DataBuf.Mutable target, @NonNull ObjectMapper mapper, @NonNull Object instance);
}
