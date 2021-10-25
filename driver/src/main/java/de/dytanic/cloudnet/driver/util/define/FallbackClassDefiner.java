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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

/**
 * A class definer which defines classes using a class loader. This is a fallback method which will not work as expected
 * on modern jvm implementations which have higher access check requirements. In normal cases this definer should never
 * get used as the {@link UnsafeClassDefiner} should handle jvm implementations up to {@code 15} and the newer lookup
 * based {@link LookupClassDefiner} handles jvm implementations from {@code 15}.
 *
 * @author Pasqual K.
 * @since 1.0
 */
final class FallbackClassDefiner implements ClassDefiner {

  /**
   * The cached defining class loaders for each class loader of the parent classes to define the class in.
   */
  private final Map<ClassLoader, DefiningClassLoader> cache = new ConcurrentHashMap<>();

  /**
   * {@inheritDoc}
   */
  @Override
  public @NotNull Class<?> defineClass(@NotNull String name, @NotNull Class<?> parent, byte[] bytecode) {
    return this.cache.computeIfAbsent(parent.getClassLoader(), DefiningClassLoader::new).defineClass(name, bytecode);
  }

  /**
   * A class loader which gives access to the normally protected {@code defineClass} method.
   *
   * @author Pasqual K.
   * @since 1.0
   */
  private static final class DefiningClassLoader extends ClassLoader {

    /**
     * Creates a new defining class loader for the parent class loader of the holding class.
     *
     * @param parent the parent class loader for delegation.
     */
    public DefiningClassLoader(ClassLoader parent) {
      super(parent);
    }

    /**
     * An exposed method which allows converting the given {@code bytecode} into an instance of class delegating the
     * call to {@link ClassLoader#defineClass(String, byte[], int, int)}.
     *
     * @param name     the expected name of the class.
     * @param byteCode the bytecode of the class to define.
     * @return the constructed class object from the given {@code bytecode}.
     */
    public @NotNull Class<?> defineClass(@NotNull String name, byte[] byteCode) {
      return super.defineClass(name, byteCode, 0, byteCode.length);
    }
  }
}
