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

package eu.cloudnetservice.wrapper.transform;

import java.lang.classfile.ClassTransform;
import lombok.NonNull;

public interface ClassTransformer {
  // TODO: fixup this shit naming...

  @NonNull
  ClassTransform provideTransformer();

  /**
   * Checks if this class transformer is willing to transform the class with the given name.
   *
   * @param internalClassName
   * @return
   */
  @NonNull
  TransformAcceptance checkClassAcceptance(@NonNull String internalClassName);

  /**
   * The acceptance states of a class transformer for a given class. A transformer can either accept the class to
   * indicate that it wants to transform it, or reject it to leave it as-is.
   *
   * @since 4.0
   */
  enum TransformAcceptance {

    /**
     * Indicates that the class transformer wants to transform the given target class.
     */
    ACCEPT,
    /**
     * Indicates that the class transformer wants to transform the given target class and has doesn't want to transform
     * other classes (meaning it will be unregistered and not called anymore).
     */
    ACCEPT_ONCE,
    /**
     * Indicates that the transformer has no intention to change the given target class.
     */
    REJECT,
  }
}
