/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

import lombok.NonNull;
import org.objectweb.asm.tree.ClassNode;

/**
 * A transformer for classes which are being loaded during the runtime. A transformer which is registered to a
 * transformer registry gets called BEFORE a class definition, making it able to change the class as required. Class
 * re-transformations are not posted to transformers.
 *
 * @since 4.0
 */
@FunctionalInterface
public interface Transformer {

  /**
   * Applies the transformation to the given class node. The passed in class node will get re-encoded and represents the
   * class after transformation (and therefore will be the final defined class).
   * <p>
   * This transformer is only called once, after that this transformer will automatically get removed from the
   * registered transformers (as the class is defined and transformers will never handle re-transformations).
   *
   * @param classname the fully qualified name of the transforming class.
   * @param classNode the class node in its original form, before any transformation was applied to it.
   * @throws NullPointerException if the given class name or class node is null.
   */
  void transform(@NonNull String classname, @NonNull ClassNode classNode);
}
