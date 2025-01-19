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

/**
 * A transformer for a class which gets called before the class is actually put into usage. A transformer can
 * dynamically decide if it wants to transform a class, and can be applied to multiple classes, depending on the return
 * value of {@link #classTransformWillingness(String)}.
 *
 * @since 4.0
 */
public interface ClassTransformer {

  /**
   * Provides the class transform that will be used to transform the class data. This method should only be called if a
   * prior check to {@link #classTransformWillingness(String)} did not indicate a rejection of the class.
   *
   * @return the class transform to apply to the target class.
   */
  @NonNull
  ClassTransform provideClassTransform();

  /**
   * Checks if this class transformer is willing to transform the class with the given internal name. If this method is
   * not returning a rejection status, the {@link #provideClassTransform()} method is called and the transform gets
   * applied to the target class.
   *
   * @param internalClassName the internal class name of the class being checked for transformation.
   * @return the willingness of transformation for the class with the given name.
   * @throws NullPointerException if the given internal class name is null.
   */
  @NonNull
  TransformWillingness classTransformWillingness(@NonNull String internalClassName);

  /**
   * The acceptance states of a class transformer for a given class. A transformer can either accept the class to
   * indicate that it wants to transform it, or reject it to leave it as-is.
   *
   * @since 4.0
   */
  enum TransformWillingness {

    /**
     * Indicates that the class transformer wants to transform the given target class.
     */
    ACCEPT,
    /**
     * Indicates that the class transformer wants to transform the given target class and has no intention to transform
     * other classes (meaning it will be unregistered and not called anymore).
     */
    ACCEPT_ONCE,
    /**
     * Indicates that the transformer has no intention to change the given target class.
     */
    REJECT,
  }
}
