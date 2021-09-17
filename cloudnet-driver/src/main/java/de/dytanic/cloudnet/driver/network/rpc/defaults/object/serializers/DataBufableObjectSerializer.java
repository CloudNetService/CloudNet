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
import de.dytanic.cloudnet.driver.network.buffer.DataBufable;
import de.dytanic.cloudnet.driver.network.rpc.defaults.handler.invoker.MethodInvoker;
import de.dytanic.cloudnet.driver.network.rpc.defaults.handler.invoker.MethodInvokerGenerator;
import de.dytanic.cloudnet.driver.network.rpc.exception.MissingNoArgsConstructorException;
import de.dytanic.cloudnet.driver.network.rpc.object.ObjectMapper;
import de.dytanic.cloudnet.driver.network.rpc.object.ObjectSerializer;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DataBufableObjectSerializer implements ObjectSerializer<DataBufable> {

  private static final Object[] NO_ARGS = new Object[0];

  private final MethodInvokerGenerator generator = new MethodInvokerGenerator();
  private final Map<Type, MethodInvoker> cachedConstructors = new ConcurrentHashMap<>();

  @Override
  public @Nullable DataBufable read(
    @NotNull DataBuf source,
    @NotNull Type type,
    @NotNull ObjectMapper caller
  ) {
    // ensure that the type is a class
    Verify.verify(type instanceof Class<?>, "Call to data buf reader as non-class");
    // try to read the data from into class
    Class<?> clazz = (Class<?>) type;
    // find a no-args constructor method invoker of the class
    MethodInvoker invoker = this.cachedConstructors.computeIfAbsent(
      type,
      $ -> {
        try {
          // lookup the no args constructor before generating
          clazz.getDeclaredConstructor();
          // constructor is there, generate
          return this.generator.makeNoArgsConstructorInvoker(clazz);
        } catch (NoSuchMethodException exception) {
          throw new MissingNoArgsConstructorException(clazz);
        }
      });
    // create an instance of the class and read the data of the buffer into it
    // (just to suppress the warning the invoke method is wrapped into requireNonNull, it will never return null)
    DataBufable object = Objects.requireNonNull((DataBufable) invoker.callMethod(NO_ARGS));
    object.readData(source);
    return object;
  }

  @Override
  public void write(
    @NotNull DataBuf.Mutable dataBuf,
    @NotNull DataBufable object,
    @NotNull Type type,
    @NotNull ObjectMapper caller
  ) {
    object.writeData(dataBuf);
  }
}
