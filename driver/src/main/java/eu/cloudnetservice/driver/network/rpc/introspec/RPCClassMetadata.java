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

package eu.cloudnetservice.driver.network.rpc.introspec;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import eu.cloudnetservice.driver.network.rpc.annotation.RPCTimeout;
import java.lang.invoke.TypeDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class RPCClassMetadata {

  private final Class<?> target;
  private final Duration rpcTimeout;

  private final Table<String, String, RPCMethodMetadata> methods; // name, desc -> method

  private RPCClassMetadata(@NonNull Class<?> target) {
    this.target = target;
    this.rpcTimeout = parseRPCTimeout(target.getAnnotation(RPCTimeout.class));
    this.methods = HashBasedTable.create();
  }

  private RPCClassMetadata(
    @NonNull Class<?> target,
    @NonNull Duration rpcTimeout,
    @NonNull Table<String, String, RPCMethodMetadata> methods
  ) {
    this.target = target;
    this.rpcTimeout = rpcTimeout;
    this.methods = methods;
  }

  static @Nullable Duration parseRPCTimeout(@Nullable RPCTimeout annotation) {
    if (annotation == null) {
      return null;
    } else if (annotation.timeout() == 0L) {
      return Duration.ZERO;
    } else {
      var timeoutUnit = annotation.unit().toChronoUnit();
      return Duration.of(annotation.timeout(), timeoutUnit);
    }
  }

  public static @NonNull RPCClassMetadata introspect(@NonNull Class<?> target) {
    var metadata = new RPCClassMetadata(target);
    metadata.introspectMethods(target);
    return metadata;
  }

  private void introspectMethods(@NonNull Class<?> target) {
    for (var method : target.getDeclaredMethods()) {
      if (method.isSynthetic()) {
        // skip compiler generated methods
        continue;
      }

      // ignore members that cannot be overridden anyway
      if (this.methodVisibleToRoot(method)) {
        var metadata = RPCMethodMetadata.fromMethod(method);
        var methodDescriptor = metadata.methodType().descriptorString();
        if (!this.methods.contains(metadata.name(), methodDescriptor)) {
          // only register each method once, starting at the highest point in the tree
          this.methods.put(metadata.name(), methodDescriptor, metadata);
        }
      } else if (Modifier.isAbstract(method.getModifiers())) {
        throw new IllegalStateException(String.format(
          "Detected abstract method %s in %s that is not visible to root type %s",
          method.getName(), method.getDeclaringClass().getName(), this.target.getName()));
      }
    }

    // introspect methods from super classes/interfaces
    this.introspectSuper(target, this::introspectMethods);
  }

  private boolean methodVisibleToRoot(@NonNull Method method) {
    if (this.target == method.getDeclaringClass()) {
      // methods that are defined in the same class as our target can always be accessed directly
      return true;
    }

    var modifiers = method.getModifiers();
    if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) {
      // the method is always accessible to the target class in case it's public or protected
      return true;
    }

    if (Modifier.isPrivate(modifiers)) {
      // private members in other classes are never visible to our root
      return false;
    }

    // check if the method and introspecting class are in the same package
    var introspectingPackage = this.target.getPackage();
    var declaringPackage = method.getDeclaringClass().getPackage();
    return introspectingPackage == declaringPackage;
  }

  private void introspectSuper(@NonNull Class<?> target, @NonNull Consumer<Class<?>> introspectCallback) {
    // introspect superclass
    var superclass = target.getSuperclass();
    if (superclass != null && superclass != Object.class) {
      introspectCallback.accept(superclass);
    }

    // introspect super interfaces
    for (var superInterface : target.getInterfaces()) {
      introspectCallback.accept(superInterface);
    }
  }

  public @NonNull Class<?> targetClass() {
    return this.target;
  }

  public @Nullable Duration defaultRPCTimeout() {
    return this.rpcTimeout;
  }

  // @throws UnsupportedOperationException
  public void unregisterMethod(@NonNull String name, @NonNull TypeDescriptor typeDescriptor) {
    this.methods.remove(name, typeDescriptor.descriptorString());
  }

  public @Nullable RPCMethodMetadata findMethod(@NonNull String name, @NonNull TypeDescriptor typeDescriptor) {
    return this.methods.get(name, typeDescriptor.descriptorString());
  }

  public @NonNull List<RPCMethodMetadata> findMethods(@NonNull Predicate<RPCMethodMetadata> filter) {
    return this.methods.values().stream().filter(filter).toList();
  }

  public @NonNull RPCClassMetadata freeze() {
    var frozenMethods = ImmutableTable.copyOf(this.methods);
    return new RPCClassMetadata(this.target, this.rpcTimeout, frozenMethods);
  }
}
