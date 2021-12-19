/*
 * This file is part of aerogel, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2021 Pasqual K. and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package de.dytanic.cloudnet.driver.util.define;

import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import lombok.NonNull;

/**
 * A class definer for modern jvm implementation (Java 15+) which makes use of the newly added {@code defineHiddenClass}
 * method in the {@code Lookup} class.
 *
 * @author Pasqual K.
 * @since 1.0
 */
final class LookupClassDefiner implements ClassDefiner {

  /**
   * The jvm trusted lookup instance. It allows access to every lookup even if the access to these classes is denied for
   * the current module.
   */
  private static final Lookup TRUSTED_LOOKUP;
  /**
   * The created option array to define a class.
   */
  private static final Object HIDDEN_CLASS_OPTIONS;
  /**
   * The method to define a hidden class using a lookup instance.
   */
  private static final Method DEFINE_HIDDEN_METHOD;

  static {
    Lookup trustedLookup = null;
    Object hiddenClassOptions = null;
    Method defineHiddenMethod = null;

    if (UnsafeAccess.available()) {
      try {
        // get the trusted lookup field
        var implLookup = Lookup.class.getDeclaredField("IMPL_LOOKUP");
        // get the lookup base and offset
        var base = UnsafeAccess.UNSAFE_CLASS
          .getMethod("staticFieldBase", Field.class)
          .invoke(UnsafeAccess.THE_UNSAFE_INSTANCE, implLookup);
        var offset = (long) UnsafeAccess.UNSAFE_CLASS
          .getMethod("staticFieldOffset", Field.class)
          .invoke(UnsafeAccess.THE_UNSAFE_INSTANCE, implLookup);
        // get the trusted lookup from the field
        trustedLookup = (Lookup) UnsafeAccess.UNSAFE_CLASS
          .getMethod("getObject", Object.class, long.class)
          .invoke(UnsafeAccess.THE_UNSAFE_INSTANCE, base, offset);
        // get the options for defining hidden clases
        hiddenClassOptions = classOptionArray();
        // get the method to define a hidden class
        var defineHiddenClassMethod = Lookup.class.getMethod("defineHiddenClass",
          byte[].class,
          boolean.class,
          hiddenClassOptions.getClass());
        defineHiddenClassMethod.setAccessible(true);
        // convert to method handle
        defineHiddenMethod = defineHiddenClassMethod;
      } catch (Throwable ignored) {
      }
    }
    // set the static final fields
    TRUSTED_LOOKUP = trustedLookup;
    HIDDEN_CLASS_OPTIONS = hiddenClassOptions;
    DEFINE_HIDDEN_METHOD = defineHiddenMethod;
  }

  /**
   * Creates a new array of class options which is used to define a class in a lookup.
   *
   * @return the created class option array to define a class.
   * @throws Exception if any exception occurs during the array lookup.
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  private static @NonNull Object classOptionArray() throws Exception {
    // the ClassOption enum is a subclass of the Lookup class
    Class optionClass = Class.forName(Lookup.class.getName() + "$ClassOption");
    // create an array of these options (for now always one option)
    var resultingOptionArray = Array.newInstance(optionClass, 1);
    // set the first option to NESTMATE
    Array.set(resultingOptionArray, 0, Enum.valueOf(optionClass, "NESTMATE"));
    // that's it
    return resultingOptionArray;
  }

  /**
   * Get if the lookup class definer requirements are met to use the definer in the current jvm.
   *
   * @return if the lookup class definer requirements are met to use the definer in the current jvm.
   */
  public static boolean available() {
    return TRUSTED_LOOKUP != null && HIDDEN_CLASS_OPTIONS != null && DEFINE_HIDDEN_METHOD != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Class<?> defineClass(@NonNull String name, @NonNull Class<?> parent, byte[] bytecode) {
    try {
      // define the method using the method handle
      var lookup = (Lookup) DEFINE_HIDDEN_METHOD.invoke(
        TRUSTED_LOOKUP.in(parent),
        bytecode,
        false,
        HIDDEN_CLASS_OPTIONS);
      // get the class from the lookup
      return lookup.lookupClass();
    } catch (Throwable throwable) {
      throw new IllegalStateException("Exception defining class " + name, throwable);
    }
  }
}
