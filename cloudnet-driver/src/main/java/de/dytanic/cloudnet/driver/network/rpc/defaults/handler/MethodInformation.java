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

package de.dytanic.cloudnet.driver.network.rpc.defaults.handler;

import de.dytanic.cloudnet.driver.network.rpc.annotation.RPCIgnore;
import de.dytanic.cloudnet.driver.network.rpc.defaults.handler.invoker.MethodInvoker;
import de.dytanic.cloudnet.driver.network.rpc.defaults.handler.invoker.MethodInvokerGenerator;
import de.dytanic.cloudnet.driver.network.rpc.exception.CannotDecideException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import org.jetbrains.annotations.NotNull;

public class MethodInformation {

  private final String name;
  private final Type returnType;
  private final Type[] arguments;
  private final Object sourceInstance;
  private final Class<?> definingClass;
  private final MethodInvoker methodInvoker;

  public MethodInformation(
    String name,
    Type rType,
    Type[] arguments,
    Object sourceInstance,
    Class<?> definingClass,
    MethodInvokerGenerator generator
  ) {
    this.name = name;
    this.returnType = rType;
    this.arguments = arguments;
    this.sourceInstance = sourceInstance;
    this.definingClass = definingClass;
    this.methodInvoker = generator.makeInvoker(this);
  }

  public static @NotNull MethodInformation find(
    @NotNull Object instance,
    @NotNull String name,
    @NotNull MethodInvokerGenerator generator
  ) {
    // filter all technically possible methods
    Method method = null;
    for (Method declaredMethod : instance.getClass().getDeclaredMethods()) {
      // check if the method might be a candidate
      if (declaredMethod.getName().equals(name)
        && Modifier.isPublic(declaredMethod.getModifiers())
        && !declaredMethod.isAnnotationPresent(RPCIgnore.class)
      ) {
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

  public @NotNull String getName() {
    return this.name;
  }

  public @NotNull Type getReturnType() {
    return this.returnType;
  }

  public Type @NotNull [] getArguments() {
    return this.arguments;
  }

  public @NotNull Object getSourceInstance() {
    return this.sourceInstance;
  }

  public @NotNull Class<?> getDefiningClass() {
    return this.definingClass;
  }

  public @NotNull MethodInvoker getMethodInvoker() {
    return this.methodInvoker;
  }
}
