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

package eu.cloudnetservice.cloudnet.common.unsafe;

import java.net.URI;
import java.net.URISyntaxException;
import lombok.NonNull;
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * A resolver for elements in the class path.
 */
@Internal
public final class ResourceResolver {

  private ResourceResolver() {
    throw new UnsupportedOperationException();
  }

  /**
   * Resolves the location of the given {@code clazz} in the class path.
   *
   * @param clazz the clazz to get the location of.
   * @return the uri of the clazz location in the class path.
   * @throws NullPointerException if {@code clazz} is null.
   */
  public static @NonNull URI resolveURIFromResourceByClass(@NonNull Class<?> clazz) {
    try {
      return clazz.getProtectionDomain().getCodeSource().getLocation().toURI();
    } catch (URISyntaxException exception) {
      // this can not happen - a file has always a valid location
      throw new IllegalStateException("Unable to resolve uri of " + clazz, exception);
    }
  }
}