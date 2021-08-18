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

package de.dytanic.cloudnet.driver.network.rpc.defaults.object.serializers;

import com.google.common.base.Verify;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.rpc.object.ObjectMapper;
import de.dytanic.cloudnet.driver.network.rpc.object.ObjectSerializer;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClassObjectSerializer implements ObjectSerializer<Class<?>> {

  @Override
  public @Nullable Object read(
    @NotNull DataBuf source,
    @NotNull Type type,
    @NotNull ObjectMapper caller
  ) {
    // verify & unwrap the type
    Verify.verify(type instanceof Class, "Cannot call class serializer without providing a class type");
    Class<?> clazz = (Class<?>) type;
    // special case are arrays: their root type is a class - but they are actually an array so we need to check here
    if (clazz.isArray()) {
      // read the array component type information
      Type arrayType = clazz.getComponentType();
      // read the serialized array information
      int size = source.readInt();
      Object array = Array.newInstance(clazz, size);
      // read the objects of the component type from the buffer
      for (int i = 0; i < size; i++) {
        Array.set(array, i, caller.readObject(source, arrayType));
      }
      // read done, return
      return array;
    }
    // not an array, just return the class
    return clazz;
  }

  @Override
  public void writeObject(
    @NotNull DataBuf.Mutable dataBuf,
    @NotNull Object object,
    @NotNull Type type,
    @NotNull ObjectMapper caller
  ) {
    // verify & unwrap the type
    Verify.verify(type instanceof Class, "Cannot call class serializer without providing a class type");
    Class<?> clazz = (Class<?>) type;
    // check if we need to write information (only needed if an array is provided as the class type will be present on
    // the caller site when calling the read method)
    if (clazz.isArray()) {
      // read the array information
      int arraySize = Array.getLength(object);
      // write the information about the array into the buffer
      dataBuf.writeInt(arraySize);
      for (int i = 0; i < arraySize; i++) {
        caller.writeObject(dataBuf, Array.get(object, i));
      }
    }
  }
}
