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

/**
 * Gives access to the {@code Unsafe} class of the jvm which allows access to previously inaccessible magic.
 *
 * @author Pasqual K.
 * @since 1.0
 */
final class UnsafeAccess {

  /**
   * The {@code unsafe} class object if present.
   */
  static final Class<?> UNSAFE_CLASS;
  /**
   * The jvm static {@code Unsafe} instance if present.
   */
  static final Object THE_UNSAFE_INSTANCE;

  static {
    Class<?> unsafeClass = null;
    Object theUnsafeInstance = null;

    try {
      // get the unsafe class
      unsafeClass = Class.forName("sun.misc.Unsafe");
      // get the unsafe instance
      var theUnsafeField = unsafeClass.getDeclaredField("theUnsafe");
      theUnsafeField.setAccessible(true);
      theUnsafeInstance = theUnsafeField.get(null);
    } catch (Exception ignored) {
    }
    // assign to the static final fields
    UNSAFE_CLASS = unsafeClass;
    THE_UNSAFE_INSTANCE = theUnsafeInstance;
  }

  private UnsafeAccess() {
    throw new UnsupportedOperationException();
  }

  /**
   * Get if the {@code Unsafe} class and instance were successfully loaded.
   *
   * @return if the {@code Unsafe} class and instance were successfully loaded.
   */
  static boolean isAvailable() {
    return UNSAFE_CLASS != null && THE_UNSAFE_INSTANCE != null;
  }
}
