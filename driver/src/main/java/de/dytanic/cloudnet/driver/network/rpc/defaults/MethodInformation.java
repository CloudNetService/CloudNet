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

package de.dytanic.cloudnet.driver.network.rpc.defaults;

import com.google.common.reflect.TypeToken;
import de.dytanic.cloudnet.driver.network.rpc.annotation.RPCIgnore;
import de.dytanic.cloudnet.driver.network.rpc.defaults.handler.invoker.MethodInvoker;
import de.dytanic.cloudnet.driver.network.rpc.defaults.handler.invoker.MethodInvokerGenerator;
import de.dytanic.cloudnet.driver.network.rpc.exception.CannotDecideException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class MethodInformation {

  private final String name;
  private final Type returnType;
  private final Type[] arguments;
  private final boolean voidMethod;
  private final Object sourceInstance;
  private final Class<?> rawReturnType;
  private final Class<?> definingClass;
  private final MethodInvoker methodInvoker;

  public MethodInformation(
    @NonNull String name,
    @NonNull Type rType,
    @NonNull Type[] arguments,
    @Nullable Object sourceInstance,
    @NonNull Class<?> definingClass,
    @Nullable MethodInvokerGenerator generator
  ) {
    this.name = name;
    this.returnType = rType;
    this.arguments = arguments;
    this.voidMethod = rType.equals(void.class);
    this.sourceInstance = sourceInstance;
    this.rawReturnType = TypeToken.of(rType).getRawType();
    this.definingClass = definingClass;
    this.methodInvoker = generator == null ? null : generator.makeMethodInvoker(this);
  }

  public static @NonNull MethodInformation find(
    @Nullable Object instance,
    @NonNull Class<?> sourceClass,
    @NonNull String name,
    @Nullable MethodInvokerGenerator generator,
    int argumentCount
  ) {
    // filter all technically possible methods
    Method method = null;
    for (var declaredMethod : sourceClass.getDeclaredMethods()) {
      // check if the method might be a candidate
      if (declaredMethod.getName().equals(name)
        && !declaredMethod.isAnnotationPresent(RPCIgnore.class)
        && declaredMethod.getParameterCount() == argumentCount) {
        if (method != null) {
          // we found more than one method we could call, fail here
          throw new CannotDecideException(name);
        } else {
          method = declaredMethod;
        }
      }
    }
    // we found no method with that name, fail
    if (method == null) {
      throw new CannotDecideException(name);
    }
    // create the method information based on the found information
    return new MethodInformation(
      name,
      method.getGenericReturnType(),
      method.getGenericParameterTypes(),
      instance,
      method.getDeclaringClass(),
      generator);
  }

  public @NonNull String name() {
    return this.name;
  }

  public @NonNull Type returnType() {
    return this.returnType;
  }

  public @NonNull Class<?> rawReturnType() {
    return this.rawReturnType;
  }

  public Type @NonNull [] arguments() {
    return this.arguments;
  }

  public boolean voidMethod() {
    return this.voidMethod;
  }

  public Object sourceInstance() {
    return this.sourceInstance;
  }

  public @NonNull Class<?> definingClass() {
    return this.definingClass;
  }

  public MethodInvoker methodInvoker() {
    return this.methodInvoker;
  }
}
