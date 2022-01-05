/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.driver.util.define;

import lombok.NonNull;

/**
 * A holder class for the best class definer of the current jvm.
 *
 * @author Aldin S. (0utplay@cloudnetservice.eu)
 * @author Pasqual Koschmieder. (derklaro@cloudnetservice.eu)
 * @since 4.0
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
    } else {
      DEFINER = new FallbackClassDefiner();
    }
  }

  /**
   * Creating an instance of this helper class is not allowed, results in {@link UnsupportedOperationException}.
   *
   * @throws UnsupportedOperationException on invocation
   */
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
