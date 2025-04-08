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
import eu.cloudnetservice.driver.network.rpc.exception.MissingObjectSerializerException;
import java.lang.reflect.Type;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

/**
 * Represents the registry for object serializers and is the base class to read/write an object to a buffer.
 *
 * @since 4.0
 */
public interface ObjectMapper {

  /**
   * Unregisters all object serializers which were registered for the given type, include all super types of the given
   * type if requested.
   *
   * @param type       the base type of the mappers to unregister.
   * @param superTypes if all super types of the base type should get included when unregistering.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given base type is null.
   */
  @NonNull
  ObjectMapper unregisterBinding(@NonNull Type type, boolean superTypes);

  /**
   * Unregisters all object serializers whose classes were loaded by the given class loader.
   *
   * @param classLoader the class loader of the object serializers to unregister.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if the given class loader is null.
   */
  @NonNull
  ObjectMapper unregisterBindings(@NonNull ClassLoader classLoader);

  /**
   * Registers the given object serializer to this mapper, including all super types of the given type if requested.
   * This call will silently be ignored if previously a mapper was registered for one of the requested types.
   *
   * @param type       the type which can be (de-) serialized using the given serializer.
   * @param serializer the serializer to use for the type.
   * @param superTypes if all super types of the given type should get associated with the given serializer as well.
   * @param <T>        the generic type of the object serializer to register.
   * @return the same instance as used to call the method, for chaining.
   * @throws NullPointerException if either the given type or serializer is null.
   */
  @NonNull
  <T> ObjectMapper registerBinding(
    @NonNull Type type,
    @NonNull ObjectSerializer<T> serializer,
    boolean superTypes);

  /**
   * Writes the given object into the given data buffer using the previously registered object serializers.
   *
   * @param dataBuf the buffer to serialize the data to.
   * @param object  the object to serialize into the buffer, can be null.
   * @param <T>     the generic type of the object to serialize.
   * @return the same buffer used to call the method, for further writing.
   * @throws NullPointerException             if the given buffer is null.
   * @throws MissingObjectSerializerException if no serializer was found to serialize the given object.
   */
  @NonNull
  <T> DataBuf.Mutable writeObject(@NonNull DataBuf.Mutable dataBuf, @Nullable T object);

  /**
   * Reads an object from the given buffer by the given type using the previously registered serializers. The returned
   * object might be null if the object written to the buffer was null as well.
   *
   * @param dataBuf the buffer from which the object should get deserialized.
   * @param type    the type of the object to deserialize.
   * @param <T>     the generic type of the object.
   * @return the deserialized object from the buffer, can be null.
   * @throws NullPointerException             if either the given buffer or type is null.
   * @throws MissingObjectSerializerException if no serializer was found to deserialize the given object.
   */
  @UnknownNullability
  <T> T readObject(@NonNull DataBuf dataBuf, @NonNull Type type);
}
