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

package eu.cloudnetservice.ext.platforminject.util;

import com.google.common.collect.ObjectArrays;
import dev.derklaro.aerogel.BindingConstructor;
import eu.cloudnetservice.driver.inject.InjectUtil;
import lombok.NonNull;

public final class BindingUtil {

  private BindingUtil() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull BindingConstructor fixedBindingWithBound(@NonNull Object object, @NonNull Class<?>... other) {
    // check if there are other types supplied
    if (other.length == 0) {
      return InjectUtil.createFixedBinding(object, object.getClass());
    }

    // create a binding constructor which is primarily bound to the given object type
    var boundTypes = ObjectArrays.concat(object.getClass(), other);
    return InjectUtil.createFixedBinding(object, boundTypes);
  }
}
