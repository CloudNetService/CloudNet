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

import java.util.function.Predicate;
import lombok.NonNull;

/**
 * A registry to register class transformers to. Each registered transformer is only called once.
 *
 * @since 4.0
 */
public interface TransformerRegistry {

  /**
   * Registers a transformer for a class with the given package prefix and the given class name (both case-sensitive).
   *
   * @param packagePrefix the prefix of the package the class to transform is in.
   * @param classname     the name of the class to transform.
   * @param transformer   the transformer to apply to the class.
   * @throws NullPointerException if the given package prefix, class name or transformer is null.
   */
  void registerTransformer(@NonNull String packagePrefix, @NonNull String classname, @NonNull Transformer transformer);

  /**
   * Registers a transformer for a class which matches the given filter.
   *
   * @param filter      the filter for the class name of the class to transform.
   * @param transformer the transformer to apply to the class.
   * @throws NullPointerException if the given filter or transformer is null.
   */
  void registerTransformer(@NonNull Predicate<String> filter, @NonNull Transformer transformer);
}
