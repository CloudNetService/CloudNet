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

package eu.cloudnetservice.driver.impl.network.rpc.introspec;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import eu.cloudnetservice.driver.network.rpc.annotation.RPCIgnore;
import eu.cloudnetservice.driver.network.rpc.annotation.RPCTimeout;
import java.lang.invoke.TypeDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * Collected metadata for classes that can be called via rpc.
 *
 * @since 4.0
 */
public final class RPCClassMetadata {

  // filter to ignore all methods that can be overridden from java.lang.Object
  private static final Predicate<Method> STANDARD_IGNORED_METHOD_FILTER = m ->
    (m.getName().equals("clone") && m.getParameterCount() == 0)
      || (m.getName().equals("hashCode") && m.getParameterCount() == 0 && m.getReturnType() == int.class)
      || (m.getName().equals("finalize") && m.getParameterCount() == 0 && m.getReturnType() == void.class)
      || (m.getName().equals("toString") && m.getParameterCount() == 0 && m.getReturnType() == String.class)
      || (m.getName().equals("equals") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == Object.class);

  private final Class<?> target;
  private final Duration rpcTimeout;

  private final Table<String, String, DefaultRPCMethodMetadata> methods; // name, desc -> method

  /**
   * Constructs a new class metadata instance with the given target class.
   *
   * @param target the target class of this metadata.
   * @throws NullPointerException if the given target class is null.
   */
  private RPCClassMetadata(@NonNull Class<?> target) {
    this.target = target;
    this.rpcTimeout = parseRPCTimeout(target.getAnnotation(RPCTimeout.class));
    this.methods = HashBasedTable.create();
  }

  /**
   * Constructs a full instance of a class metadata from the given parameters.
   *
   * @param target     the target class of this metadata.
   * @param rpcTimeout the class-wide default timeout of the rpc methods.
   * @param methods    the methods that can be called via rpc in the given target class.
   * @throws NullPointerException if the given target class or method table is null.
   */
  private RPCClassMetadata(
    @NonNull Class<?> target,
    @Nullable Duration rpcTimeout,
    @NonNull Table<String, String, DefaultRPCMethodMetadata> methods
  ) {
    this.target = target;
    this.rpcTimeout = rpcTimeout;
    this.methods = methods;
  }

  /**
   * Parses the given rpc timeout annotation. While some timeouts relly make no sense to apply, this method just assumes
   * the user knows that he is doing and applies every timeout that is at least 1.
   *
   * @param annotation the annotation to parse, can be null.
   * @return the parsed rpc timeout or null if the parsing wasn't applicable.
   */
  static @Nullable Duration parseRPCTimeout(@Nullable RPCTimeout annotation) {
    if (annotation != null && annotation.timeout() >= 1) {
      var timeoutUnit = annotation.unit().toChronoUnit();
      return Duration.of(annotation.timeout(), timeoutUnit);
    } else {
      return null;
    }
  }

  /**
   * Validates that the given target class is actually visible to the outside world and not some sort of hidden class in
   * which methods cannot be called anyway.
   *
   * @param target the class to validate.
   * @throws NullPointerException     if the given target class is null.
   * @throws IllegalArgumentException if the given target class is some sort of hidden class.
   */
  static void validateTargetClass(@NonNull Class<?> target) {
    if (target.isHidden()
      || target.isPrimitive()
      || target.isLocalClass()
      || target.isAnonymousClass()) {
      throw new IllegalArgumentException(String.format(
        "class %s cannot be used with rpc: must not be hidden, primitive, local or anonymous",
        target.getName()));
    }
  }

  /**
   * Introspects all methods in the class hierarchy of the given target class and wraps them into a class metadata.
   *
   * @param target the target class to introspect.
   * @return the generated class metadata for the given class.
   * @throws NullPointerException     if the given target class is null.
   * @throws IllegalArgumentException if some precondition, to ensure functionality with rpc, fails.
   * @throws IllegalStateException    if some precondition, to ensure functionality with rpc, fails.
   */
  public static @NonNull RPCClassMetadata introspect(@NonNull Class<?> target) {
    validateTargetClass(target);
    var metadata = new RPCClassMetadata(target);
    metadata.introspectMethods(target, new HashSet<>());
    return metadata;
  }

  /**
   * Introspects the methods located in the given class. This method is recursively called for all classes and
   * interfaces in the class hierarchy of the given target, hence the visited class set to prevent infinite loops due to
   * circular class references.
   *
   * @param target         the target class to introspect for methods that can be used with rpc.
   * @param visitedClasses the classes that were already introspected in the process.
   * @throws NullPointerException     if the given target class or visited class set is null.
   * @throws IllegalArgumentException if some precondition, to ensure functionality with rpc, fails.
   * @throws IllegalStateException    if some precondition, to ensure functionality with rpc, fails.
   */
  private void introspectMethods(@NonNull Class<?> target, @NonNull Set<Class<?>> visitedClasses) {
    for (var method : target.getDeclaredMethods()) {
      if (method.isSynthetic()) {
        // skip compiler generated methods
        continue;
      }

      // ignore members that cannot be overridden anyway
      if (this.methodVisibleToRoot(method)) {
        if (!method.isAnnotationPresent(RPCIgnore.class) && !STANDARD_IGNORED_METHOD_FILTER.test(method)) {
          var metadata = DefaultRPCMethodMetadata.fromMethod(method);
          var methodDescriptor = metadata.methodType().descriptorString();
          if (!this.methods.contains(metadata.name(), methodDescriptor)) {
            // only register each method once, starting at the highest point in the tree
            this.methods.put(metadata.name(), methodDescriptor, metadata);
          }
        }
      } else if (Modifier.isAbstract(method.getModifiers())) {
        throw new IllegalStateException(String.format(
          "Detected abstract method %s in %s that is not visible to root type %s",
          method.getName(), method.getDeclaringClass().getName(), this.target.getName()));
      }
    }

    // introspect methods from super classes/interfaces
    this.introspectSuper(target, visitedClasses, clazz -> this.introspectMethods(clazz, visitedClasses));
  }

  /**
   * Validates that the given method is always visible to the root class and a possible nest mate class implementation
   * of the root class. This means that the given method is either public or protected, or the defining class resides in
   * the same package as the root class. Private members are always marked as not visible.
   *
   * @param method the method that should be checked for visibility.
   * @return true if the given method is visible to the root type, false otherwise.
   * @throws NullPointerException if the given method is null.
   */
  private boolean methodVisibleToRoot(@NonNull Method method) {
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

  /**
   * Calls the given introspect callback for the super class and super interfaces of the given target class unless they
   * were already visited. This method marks a class as visited before calling the introspect callback on it.
   *
   * @param target             the target class to visit the superclass and super interfaces of.
   * @param visitedClasses     the classes that were already visited in the process.
   * @param introspectCallback the callback to call with the non-visited super classes.
   * @throws NullPointerException if the given target class, visited class set or introspect callback is null.
   */
  private void introspectSuper(
    @NonNull Class<?> target,
    @NonNull Set<Class<?>> visitedClasses,
    @NonNull Consumer<Class<?>> introspectCallback
  ) {
    // introspect superclass
    var superclass = target.getSuperclass();
    if (superclass != null && superclass != Object.class) {
      if (visitedClasses.add(superclass)) {
        introspectCallback.accept(superclass);
      }
    }

    // introspect super interfaces
    for (var superInterface : target.getInterfaces()) {
      if (visitedClasses.add(superInterface)) {
        introspectCallback.accept(superInterface);
      }
    }
  }

  /**
   * Get the class that is targeted by this rpc class metadata.
   *
   * @return the class that is targeted by this rpc class metadata.
   */
  public @NonNull Class<?> targetClass() {
    return this.target;
  }

  /**
   * Get the default rpc timeout that should be applied to all methods in the target class. Can be null if no such
   * timeout was explicitly declared using {@link RPCTimeout} on the target class.
   *
   * @return the default rpc timeout that should be applied to all methods in the target class, can be null.
   */
  public @Nullable Duration defaultRPCTimeout() {
    return this.rpcTimeout;
  }

  /**
   * Get an immutable collection of all methods that can be used for rpc in the target class.
   *
   * @return all methods that can be used for rpc in the target class.
   */
  @UnmodifiableView
  public @NonNull Collection<DefaultRPCMethodMetadata> methods() {
    return Collections.unmodifiableCollection(this.methods.values());
  }

  /**
   * Freezes this metadata view which means that further modifications to the underlying data structures will return in
   * an exception and not succeed. If this metadata is already frozen, the same instance is returned as no work needs to
   * be done.
   *
   * @return this metadata but frozen.
   */
  public @NonNull RPCClassMetadata freeze() {
    if (this.methods instanceof ImmutableTable) {
      // already frozen
      return this;
    } else {
      // freeze the method view (copy & disallow changed)
      var frozenMethods = ImmutableTable.copyOf(this.methods);
      return new RPCClassMetadata(this.target, this.rpcTimeout, frozenMethods);
    }
  }

  /**
   * Unregisters the given method by name and descriptor for this class, for example to specifically ignore it.
   *
   * @param name           the name of the method to unregister.
   * @param typeDescriptor the type descriptor of the method to unregister.
   * @throws NullPointerException          if the given name or type descriptor is null.
   * @throws UnsupportedOperationException if this class metadata is frozen.
   */
  public void unregisterMethod(@NonNull String name, @NonNull TypeDescriptor typeDescriptor) {
    this.methods.remove(name, typeDescriptor.descriptorString());
  }

  /**
   * Returns all methods that are known to this class metadata that match the given filter.
   *
   * @param filter the filter to apply to each method metadata known to this class metadata.
   * @return the method metas that matched the given filter.
   * @throws NullPointerException if the given filter is null.
   */
  public @NonNull List<DefaultRPCMethodMetadata> findMethods(@NonNull Predicate<DefaultRPCMethodMetadata> filter) {
    return this.methods.values().stream().filter(filter).toList();
  }

  /**
   * Returns the method that matches the given name and type descriptor, or null if no such method exists.
   *
   * @param name           the name of the method metadata to get.
   * @param typeDescriptor the type descriptor of the method metadata to get.
   * @return the meta of the method with the given name and descriptor, of null if no such method exists.
   * @throws NullPointerException if the given name or type descriptor is null.
   */
  public @Nullable DefaultRPCMethodMetadata findMethod(@NonNull String name, @NonNull TypeDescriptor typeDescriptor) {
    return this.methods.get(name, typeDescriptor.descriptorString());
  }
}
