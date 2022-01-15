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

package eu.cloudnetservice.cloudnet.driver.network.rpc.defaults.object.data;

import com.google.common.base.Verify;
import eu.cloudnetservice.cloudnet.driver.network.buffer.DataBuf;
import eu.cloudnetservice.cloudnet.driver.network.rpc.object.ObjectMapper;
import eu.cloudnetservice.cloudnet.driver.network.rpc.object.ObjectSerializer;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A serializer for all data classes and arrays which have no special serializer available.
 *
 * @since 4.0
 */
public class DataClassSerializer implements ObjectSerializer<Object> {

  private final Lock readLock = new ReentrantLock();

  private final DataClassInvokerGenerator generator = new DataClassInvokerGenerator();
  private final Map<Type, DataClassInformation> cachedClassInformation = new HashMap<>();

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable Object read(
    @NonNull DataBuf source,
    @NonNull Type type,
    @NonNull ObjectMapper caller
  ) {
    // ensure that the given type is a class & unwrap
    Verify.verify(type instanceof Class<?>, "Cannot call data class serializer on non-class");
    var clazz = (Class<?>) type;
    // check if the type is an array
    if (clazz.isArray()) {
      return this.readArray(source, clazz, caller);
    }
    // get the class information and deserialize the object
    var information = this.readOrCreateInformation(type);
    return information.instanceCreator().makeInstance(source, caller);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(
    @NonNull DataBuf.Mutable dataBuf,
    @NonNull Object object,
    @NonNull Type type,
    @NonNull ObjectMapper caller
  ) {
    // ensure that the given type is a class & unwrap
    Verify.verify(type instanceof Class<?>, "Cannot call data class serializer on non-class");
    var clazz = (Class<?>) type;
    // check if the type is an array
    if (clazz.isArray()) {
      this.writeArray(dataBuf, object, caller);
      return;
    }
    // get the class information and serialize the object
    var information = this.readOrCreateInformation(type);
    information.informationWriter().writeInformation(dataBuf, object, caller);
  }

  /**
   * Reads an array from the given data buf source.
   *
   * @param source the buffer to read the array from.
   * @param clazz  the type of the array to read.
   * @param caller the object mapper used for deserialization of the entries.
   * @return the read array from the buffer.
   * @throws NullPointerException if either the given buffer, type or object mapper is null.
   */
  protected @Nullable Object readArray(@NonNull DataBuf source, @NonNull Class<?> clazz, @NonNull ObjectMapper caller) {
    // read the array component type information
    var arrayType = clazz.getComponentType();
    // read the serialized array information
    var size = source.readInt();
    var array = Array.newInstance(arrayType, size);
    // read the objects of the component type from the buffer
    for (var i = 0; i < size; i++) {
      Array.set(array, i, caller.readObject(source, arrayType));
    }
    // read done, return
    return array;
  }

  /**
   * Writes the given array to the given data buffer.
   *
   * @param dataBuf the buffer to write the array to.
   * @param object  the array to write.
   * @param caller  the object mapper to serialize the array to.
   * @throws NullPointerException if the given buffer, array or object mapper is null.
   */
  protected void writeArray(@NonNull DataBuf.Mutable dataBuf, @NonNull Object object, @NonNull ObjectMapper caller) {
    // read the array information
    var arraySize = Array.getLength(object);
    // write the information about the array into the buffer
    dataBuf.writeInt(arraySize);
    for (var i = 0; i < arraySize; i++) {
      caller.writeObject(dataBuf, Array.get(object, i));
    }
  }

  /**
   * Gets or creates a new data class information reader for the given type.
   *
   * @param targetType the type of the class to create the data class information for.
   * @return the created data class information, either computed or from cache.
   * @throws NullPointerException if the given type is null.
   */
  protected @NonNull DataClassInformation readOrCreateInformation(@NonNull Type targetType) {
    // lock to no generate a data class information twice for no reason
    this.readLock.lock();
    // get or generate a new class information for the given type
    try {
      return this.cachedClassInformation.computeIfAbsent(
        targetType,
        $ -> DataClassInformation.createClassInformation((Class<?>) targetType, this.generator));
    } finally {
      this.readLock.unlock();
    }
  }
}
