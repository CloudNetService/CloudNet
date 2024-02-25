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

package eu.cloudnetservice.driver.network.rpc.defaults.object.data;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.rpc.object.ObjectMapper;
import eu.cloudnetservice.driver.network.rpc.object.ObjectSerializer;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A serializer for all data classes and arrays which have no special serializer available.
 *
 * @since 4.0
 */
public class DataClassSerializer implements ObjectSerializer<Object> {

  private final LoadingCache<Type, DataClassInformation> dataClassInformationCache = Caffeine.newBuilder()
    .build(key -> DataClassInformation.createClassInformation((Class<?>) key));

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
    Preconditions.checkState(type instanceof Class<?>, "Cannot call data class serializer on non-class");
    var clazz = (Class<?>) type;
    // check if the type is an array
    if (clazz.isArray()) {
      return this.readArray(source, clazz, caller);
    }
    // get the class information and deserialize the object
    return this.dataClassInformationCache.get(type).instanceCreator().makeInstance(source, caller);
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
    Preconditions.checkState(type instanceof Class<?>, "Cannot call data class serializer on non-class");
    var clazz = (Class<?>) type;
    // check if the type is an array
    if (clazz.isArray()) {
      this.writeArray(dataBuf, object, caller);
      return;
    }
    // get the class information and serialize the object
    this.dataClassInformationCache.get(type).informationWriter().writeInformation(dataBuf, object, caller);
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
}
