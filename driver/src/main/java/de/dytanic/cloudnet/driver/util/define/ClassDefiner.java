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

import org.jetbrains.annotations.NotNull;

/**
 * Internal utility class to define classes in the runtime.
 *
 * @author Pasqual K.
 * @since 1.0
 */
@FunctionalInterface
public interface ClassDefiner {

  /**
   * Defines the given class {@code bytecode} and return the constructed class object.
   *
   * @param name     the name of the class to construct.
   * @param parent   the parent class of this class as we are assuming to define an anonymous class.
   * @param bytecode the bytecode of the class to define.
   * @return the constructed class object from the given {@code bytecode}.
   * @throws IllegalStateException if the class defining failed.
   */
  @NotNull Class<?> defineClass(@NotNull String name, @NotNull Class<?> parent, byte[] bytecode);
}
