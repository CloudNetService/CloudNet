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

package eu.cloudnetservice.ext.platforminject.runtime.util;

import dev.derklaro.aerogel.binding.BindingBuilder;
import dev.derklaro.aerogel.binding.BindingConstructor;
import lombok.NonNull;

public final class BindingUtil {

  private BindingUtil() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull BindingConstructor fixedBindingWithBound(@NonNull Object object, @NonNull Class<?>... other) {
    // check if there are other types supplied
    if (other.length == 0) {
      return BindingBuilder.create().toInstance(object);
    } else {
      return BindingBuilder.create().bindFully(object.getClass()).bindAllFully(other).toInstance(object);
    }
  }
}
