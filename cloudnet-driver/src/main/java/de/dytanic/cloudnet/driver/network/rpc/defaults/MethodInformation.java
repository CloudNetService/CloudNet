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
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import org.jetbrains.annotations.NotNull;
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
    @NotNull String name,
    @NotNull Type rType,
    @NotNull Type[] arguments,
    @Nullable Object sourceInstance,
    @NotNull Class<?> definingClass,
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

  public static @NotNull MethodInformation find(
    @Nullable Object instance,
    @NotNull Class<?> sourceClass,
    @NotNull String name,
    @Nullable MethodInvokerGenerator generator
  ) {
    // filter all technically possible methods
    Method method = null;
    for (Method declaredMethod : sourceClass.getDeclaredMethods()) {
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

  public @NotNull Class<?> getRawReturnType() {
    return this.rawReturnType;
  }

  public Type @NotNull [] getArguments() {
    return this.arguments;
  }

  public boolean isVoidMethod() {
    return this.voidMethod;
  }

  public Object getSourceInstance() {
    return this.sourceInstance;
  }

  public @NotNull Class<?> getDefiningClass() {
    return this.definingClass;
  }

  public MethodInvoker getMethodInvoker() {
    return this.methodInvoker;
  }
}
