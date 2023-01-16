/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.network.rpc.defaults.object.serializers;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.network.buffer.DataBuf;
import eu.cloudnetservice.driver.network.buffer.DataBufable;
import eu.cloudnetservice.driver.network.rpc.defaults.handler.invoker.MethodInvoker;
import eu.cloudnetservice.driver.network.rpc.defaults.handler.invoker.MethodInvokerGenerator;
import eu.cloudnetservice.driver.network.rpc.exception.MissingNoArgsConstructorException;
import eu.cloudnetservice.driver.network.rpc.object.ObjectMapper;
import eu.cloudnetservice.driver.network.rpc.object.ObjectSerializer;
import java.lang.reflect.Type;
import java.util.Objects;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an object serializer used to write subclasses of {@code DataBufable} into the outgoing buffer.
 *
 * @since 4.0
 */
public class DataBufableObjectSerializer implements ObjectSerializer<DataBufable> {

  private static final Object[] NO_ARGS = new Object[0];

  private final MethodInvokerGenerator generator = new MethodInvokerGenerator();
  private final Cache<Type, MethodInvoker> cachedConstructors = Caffeine.newBuilder().build();

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable DataBufable read(
    @NonNull DataBuf source,
    @NonNull Type type,
    @NonNull ObjectMapper caller
  ) {
    // ensure that the type is a class
    Preconditions.checkState(type instanceof Class<?>, "Call to data buf reader as non-class");
    // try to read the data from into class
    var clazz = (Class<?>) type;
    // find a no-args constructor method invoker of the class
    var invoker = this.cachedConstructors.get(
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
    var object = Objects.requireNonNull((DataBufable) invoker.callMethod(NO_ARGS));
    object.readData(source);
    return object;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(
    @NonNull DataBuf.Mutable dataBuf,
    @NonNull DataBufable object,
    @NonNull Type type,
    @NonNull ObjectMapper caller
  ) {
    object.writeData(dataBuf);
  }
}
