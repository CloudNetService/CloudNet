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

import lombok.NonNull;

/**
 * A holder class for the best class definer of the current jvm.
 *
 * @author Pasqual K.
 * @since 1.0
 */
public final class ClassDefiners {

  /**
   * The jvm static instance of the best definer for the current jvm implementation.
   */
  private static final ClassDefiner DEFINER;

  static {
    // check the lookup definer first - the unsafe defining method is available for newer jvm implementation but should
    // not be used.
    if (LookupClassDefiner.available()) {
      DEFINER = new LookupClassDefiner();
    } else if (UnsafeClassDefiner.available()) {
      DEFINER = new UnsafeClassDefiner();
    } else {
      DEFINER = new FallbackClassDefiner();
    }
  }

  private ClassDefiners() {
    throw new UnsupportedOperationException();
  }

  /**
   * Get the jvm static instance of the best definer for the current jvm implementation.
   *
   * @return the jvm static instance of the best definer for the current jvm implementation.
   */
  public static @NonNull ClassDefiner current() {
    return DEFINER;
  }
}
