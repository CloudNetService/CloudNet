/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.driver.network.rpc.defaults.object.data;

import com.google.common.base.Verify;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.rpc.object.ObjectMapper;
import de.dytanic.cloudnet.driver.network.rpc.object.ObjectSerializer;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DataClassSerializer implements ObjectSerializer<Object> {

  private final Lock readLock = new ReentrantLock();

  private final DataClassInvokerGenerator generator = new DataClassInvokerGenerator();
  private final Map<Type, DataClassInformation> cachedClassInformation = new ConcurrentHashMap<>();

  @Override
  public @Nullable Object read(
    @NotNull DataBuf source,
    @NotNull Type type,
    @NotNull ObjectMapper caller
  ) {
    // ensure that the given type is a class & unwrap
    Verify.verify(type instanceof Class<?>, "Cannot call data class serializer on non-class");
    var clazz = (Class<?>) type;
    // check if the type is an array
    if (clazz.isArray()) {
      return this.readArray(source, clazz, caller);
    }
    // ensure that only one thread generates data at the same time
    this.readLock.lock();
    try {
      // get the class information
      var information = this.cachedClassInformation.computeIfAbsent(
        type,
        $ -> DataClassInformation.createClassInformation((Class<?>) type, this.generator));
      // let the instance creator do the stuff
      return information.getInstanceCreator().makeInstance(source, caller);
    } finally {
      this.readLock.unlock();
    }
  }

  @Override
  public void write(
    @NotNull DataBuf.Mutable dataBuf,
    @NotNull Object object,
    @NotNull Type type,
    @NotNull ObjectMapper caller
  ) {
    // ensure that the given type is a class & unwrap
    Verify.verify(type instanceof Class<?>, "Cannot call data class serializer on non-class");
    var clazz = (Class<?>) type;
    // check if the type is an array
    if (clazz.isArray()) {
      this.writeArray(dataBuf, object, caller);
      return;
    }
    // get the class information
    var information = this.cachedClassInformation.computeIfAbsent(
      type,
      $ -> DataClassInformation.createClassInformation((Class<?>) type, this.generator));
    // let the information writer do the stuff
    information.getInformationWriter().writeInformation(dataBuf, object, caller);
  }

  protected @Nullable Object readArray(@NotNull DataBuf source, @NotNull Class<?> clazz, @NotNull ObjectMapper caller) {
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

  protected void writeArray(@NotNull DataBuf.Mutable dataBuf, @NotNull Object object, @NotNull ObjectMapper caller) {
    // read the array information
    var arraySize = Array.getLength(object);
    // write the information about the array into the buffer
    dataBuf.writeInt(arraySize);
    for (var i = 0; i < arraySize; i++) {
      caller.writeObject(dataBuf, Array.get(object, i));
    }
  }
}
