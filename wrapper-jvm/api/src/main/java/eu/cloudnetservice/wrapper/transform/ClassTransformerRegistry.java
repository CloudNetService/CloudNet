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

import lombok.NonNull;

/**
 * A registry for class transformers that should be called when a new class gets loaded.
 *
 * @since 4.0
 */
public interface ClassTransformerRegistry {

  /**
   * Registers the given transformer into this registry. There is no guarantee that calling this method with the same
   * transformer twice will result in a single registration.
   *
   * @param transformer the transformer to register into this registry.
   * @throws NullPointerException if the given transformer is null.
   */
  void registerTransformer(@NonNull ClassTransformer transformer);
}
