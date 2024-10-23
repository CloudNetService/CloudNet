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

package eu.cloudnetservice.utils.base;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus;

/**
 * A collection of utilities to instantiate data classes. For internal use only.
 *
 * @since 4.0
 */
@ApiStatus.Internal
public final class ClassAllocationUtil {

  // method types used for lookups
  private static final MethodType NO_ARGS_CONSTRUCTOR_TYPE = MethodType.methodType(void.class);
  private static final MethodType SUPPLIER_GET_SIGNATURE = MethodType.methodType(Object.class);
  private static final MethodType SUPPLIER_FACTORY_SIGNATURE = MethodType.methodType(Supplier.class);

  // allocator that uses method handles and the no-args constructor of a class
  private static final Function<Class<?>, Supplier<Object>> LOOKUP_ALLOCATOR;
  // allocator that uses sun.misc.Unsafe.allocateInstance to construct an instance of a class
  private static final Function<Class<?>, Supplier<Object>> UNSAFE_ALLOCATOR;

  static {
    try {
      var trustedLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
      var trustedLookup = (MethodHandles.Lookup) getInaccessibleFieldValue(trustedLookupField);
      LOOKUP_ALLOCATOR = makeLookupAlloc(trustedLookup);
    } catch (Exception exception) {
      // this is not allowed to happen as our lookup allocator is the based of
      // doing this and must always be available.
      throw new ExceptionInInitializerError(exception);
    }

    // make an allocator using sun.misc.Unsafe without making any direct references to it
    // in case it's not available for some reason in the current JVM
    // allocateInstance is one of the few "supported" methods of Unsafe as there is no
    // replacement for it yet. If there is in the future, we will migrate to that
    Function<Class<?>, Supplier<Object>> unsafeAllocator;
    try {
      var unsafeClass = Class.forName("sun.misc.Unsafe");
      var unsafeField = unsafeClass.getDeclaredField("theUnsafe");
      var theUnsafe = getInaccessibleFieldValue(unsafeField);
      unsafeAllocator = makeUnsafeAlloc(theUnsafe);
    } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException _) {
      unsafeAllocator = null;
    }
    UNSAFE_ALLOCATOR = unsafeAllocator;
  }

  private ClassAllocationUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Gets the value of a static field in a class, resetting the field access permissions after obtaining it. This is
   * done to prevent other code with insufficient permission to access a field because this code left the door open.
   *
   * @param field the static field to get the value of.
   * @return the value of the given field.
   * @throws IllegalAccessException if the field is inaccessible to the caller.
   * @throws NullPointerException   if the given field is null.
   */
  private static @NonNull Object getInaccessibleFieldValue(@NonNull Field field) throws IllegalAccessException {
    field.setAccessible(true);
    return field.get(null);
  }

  /**
   * Makes an allocator for a no-args constructor based on the given lookup.
   *
   * @param lookup the lookup to use to find the no-args constructor in the requested class.
   * @return the allocator factory function which uses no-args constructors resolved by the given lookup.
   * @throws NullPointerException if the given lookup is null.
   */
  private static @NonNull Function<Class<?>, Supplier<Object>> makeLookupAlloc(@NonNull MethodHandles.Lookup lookup) {
    return targetClass -> {
      try {
        // try to find a no-args constructor that can be invoked
        var targetClassLookup = lookup.in(targetClass);
        var noArgsConstructor = targetClassLookup.findConstructor(targetClass, NO_ARGS_CONSTRUCTOR_TYPE);
        var callSite = LambdaMetafactory.metafactory(
          targetClassLookup,
          "get",
          SUPPLIER_FACTORY_SIGNATURE,
          SUPPLIER_GET_SIGNATURE,
          noArgsConstructor,
          SUPPLIER_FACTORY_SIGNATURE);
        //noinspection unchecked
        return (Supplier<Object>) callSite.getTarget().invokeExact();
      } catch (NoSuchMethodException _) {
        // unable to find a no-args constructor in the target class
        return null;
      } catch (Throwable throwable) {
        throw new AssertionError("unable to construct supplier factory", throwable);
      }
    };
  }

  /**
   * Makes an allocator that uses sun.misc.Unsafe.allocateInstance to obtain a class instance. This means that no
   * constructors are run when obtaining the class instance, which is fine as this is only used to obtain instances of
   * data classes (whose fields are initialized later).
   *
   * @param theUnsafe the singleton instance of sun.misc.Unsafe.
   * @return the allocator factory function which creates new class instances using sun.misc.Unsafe.
   * @throws NullPointerException if the given unsafe instance is null.
   */
  private static @NonNull Function<Class<?>, Supplier<Object>> makeUnsafeAlloc(@NonNull Object theUnsafe) {
    return targetClass -> () -> {
      try {
        var unsafe = (sun.misc.Unsafe) theUnsafe;
        return unsafe.allocateInstance(targetClass);
      } catch (InstantiationException exception) {
        throw new IllegalStateException("unable to allocate instance class", exception);
      }
    };
  }

  /**
   * Constructs an instance for the given target class. The result of this method invocation should be cached by the
   * caller, to prevent many allocations of the same thing.
   *
   * @param targetClass the target class to get an instance allocator for.
   * @return an instance factory for the given target class.
   * @throws IllegalStateException if no instance factory can handle the given target class.
   * @throws NullPointerException  if the given target class is null.
   */
  public static @NonNull Supplier<Object> makeInstanceFactory(@NonNull Class<?> targetClass) {
    // try to use the class no-args constructor first
    var lookupAllocator = LOOKUP_ALLOCATOR.apply(targetClass);
    if (lookupAllocator != null) {
      return lookupAllocator;
    }

    // try to use the unsafe allocator
    if (UNSAFE_ALLOCATOR != null) {
      return UNSAFE_ALLOCATOR.apply(targetClass);
    }

    throw new IllegalStateException(String.format("unable to create instance factory for %s", targetClass.getName()));
  }
}
