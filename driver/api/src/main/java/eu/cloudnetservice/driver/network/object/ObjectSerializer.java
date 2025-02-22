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

package eu.cloudnetservice.driver.network.object;

import eu.cloudnetservice.driver.network.buffer.DataBuf;
import java.lang.reflect.Type;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A serializer for a specific type of object to be serialized into a network buffer.
 *
 * @param <T> the type of the object to serialize.
 * @since 4.0
 */
public interface ObjectSerializer<T> {

  /**
   * Reads the object from the buffer. This method should only read the content from the buffer which was written to it
   * previously, never more as the given buffer is not a slice of the buffer representing the object being read.
   * Although this method returns an object, the returned object must always match the given type during object
   * construction. The object return type is just a helper, but the return value will be cast to the given type
   * resulting in errors if provided with the wrong type of object.
   *
   * @param source the buffer to read the object from.
   * @param type   the type of the object to read.
   * @param caller the mapper to which this serializer is registered and from which the deserialize request call came.
   * @return the deserialized object of the serializer type T, or null if not readable.
   * @throws NullPointerException if either the given source, type or mapper is null.
   */
  @Nullable Object read(@NonNull DataBuf source, @NonNull Type type, @NonNull ObjectMapper caller);

  /**
   * Writes the given object into the given buffer for network transfer and later deserialization.
   *
   * @param dataBuf the buffer to write the data to.
   * @param object  the object to serialize.
   * @param type    the raw type of the object.
   * @param caller  the object mapper to which this serializer belongs and which requested the serialisation.
   * @throws NullPointerException if one of the given arguments is null.
   */
  void write(@NonNull DataBuf.Mutable dataBuf, @NonNull T object, @NonNull Type type, @NonNull ObjectMapper caller);

  /**
   * A method being called before a serializer is chosen to serialize the requested object. If true is returned, the
   * serializer will be used for serialization of the object (default behaviour), when false is returned the object
   * mapper tries to find another serializer for the object.
   *
   * @param object the object which should get written to the buffer.
   * @param caller the object mapper to which the serialisation request was made and this serializer belongs to.
   * @return true if the given object should be serialized by this serializer, false otherwise.
   * @throws NullPointerException if either the given object or mapper is null.
   */
  default boolean preWriteCheckAccepts(@NonNull T object, @NonNull ObjectMapper caller) {
    return true;
  }

  /**
   * A method being called before a serializer is chosen to deserialize an object of the given type. If true is
   * returned, the serializer will be used for deserialization of the object (default behaviour), when false is returned
   * the object mapper tries to find another deserializer for the object.
   *
   * @param type   the type of the object which should get deserialized.
   * @param caller the object mapper to which the deserialization request was made and this serializer belongs to.
   * @return true if the given object should be deserialized by this serializer, false otherwise.
   * @throws NullPointerException if either the given object type or mapper is null.
   */
  default boolean preReadCheckAccepts(@NonNull Type type, @NonNull ObjectMapper caller) {
    return true;
  }
}
