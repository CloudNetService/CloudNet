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

package eu.cloudnetservice.driver.network.rpc.defaults;

import com.google.common.reflect.TypeToken;
import eu.cloudnetservice.driver.network.rpc.annotation.RPCIgnore;
import eu.cloudnetservice.driver.network.rpc.defaults.handler.invoker.MethodInvoker;
import eu.cloudnetservice.driver.network.rpc.defaults.handler.invoker.MethodInvokerGenerator;
import eu.cloudnetservice.driver.network.rpc.exception.CannotDecideException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

/**
 * Holds the full information about a specific method needed for rpc to work properly.
 *
 * @since 4.0
 */
public class MethodInformation {

  private final String name;
  private final Type returnType;
  private final Type[] arguments;
  private final boolean voidMethod;
  private final Object sourceInstance;
  private final Class<?> rawReturnType;
  private final Class<?> definingClass;
  private final MethodInvoker methodInvoker;

  /**
   * Constructs a new method information instance.
   *
   * @param name           the name of the method.
   * @param rType          the return type of the method.
   * @param arguments      the argument types of the method.
   * @param sourceInstance the original instance the method was located in or null if not bound.
   * @param definingClass  the class in which the method is located.
   * @param generator      the generator to use to make a method invoker or null if no method invoker is needed.
   * @throws NullPointerException if one of the required constructor parameters is null.
   */
  protected MethodInformation(
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

  /**
   * Finds a method in the source class with the given name and argument count, throwing an exception if multiple
   * methods are matching, or no method is matching. This method excludes all methods from the search which are
   * annotated with {@code @RPCIgnore}.
   *
   * @param instance      the instance to which the method should get bound or null if not bound.
   * @param sourceClass   the class in which to search for the method.
   * @param name          the name of the method to find.
   * @param generator     the generator used for later method invoker generating, null if no generator is needed.
   * @param argumentCount the amount of arguments the target method must have to match.
   * @return the information of the method matching the required properties.
   * @throws NullPointerException  if either the given source class or method name is null.
   * @throws CannotDecideException if either none or multiple methods are matching the given filters.
   */
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

  /**
   * Get the name of the underlying method.
   *
   * @return the name of the underlying method.
   */
  public @NonNull String name() {
    return this.name;
  }

  /**
   * Get the return type of the underlying method.
   *
   * @return the name of the underlying method.
   */
  public @NonNull Type returnType() {
    return this.returnType;
  }

  /**
   * Get the raw return type of the underlying method. For example a method with a return type of {@code
   * Collection&lt;String&gt;} would result in {@code Collection}.
   *
   * @return the raw return type of the method.
   */
  public @NonNull Class<?> rawReturnType() {
    return this.rawReturnType;
  }

  /**
   * Get the type of each argument the method has.
   *
   * @return the type of each argument the method has.
   */
  public Type @NonNull [] arguments() {
    return this.arguments;
  }

  /**
   * Get if the method has a void return type.
   *
   * @return true if the method has a void return type, false otherwise.
   */
  public boolean voidMethod() {
    return this.voidMethod;
  }

  /**
   * Get the instance to which this method is bound, used for chained rpc call lookups to process on the instance
   * returned by the previous rpc rather than a global one.
   *
   * @return the instance to which the method is bound, or null if not bound.
   */
  public @UnknownNullability Object sourceInstance() {
    return this.sourceInstance;
  }

  /**
   * Get the class in which the method is defined.
   *
   * @return the class in which the method is defined.
   */
  public @NonNull Class<?> definingClass() {
    return this.definingClass;
  }

  /**
   * Get a method invoker for the class, if generated. The invoker can only be generated during construction of this
   * class not afterwards.
   *
   * @return a method invoker for the class, if generated.
   */
  public @UnknownNullability MethodInvoker methodInvoker() {
    return this.methodInvoker;
  }
}
