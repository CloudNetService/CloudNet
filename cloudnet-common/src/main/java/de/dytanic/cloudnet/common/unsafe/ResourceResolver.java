/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.common.unsafe;

import com.google.common.base.Preconditions;
import java.net.URI;
import java.net.URISyntaxException;
import org.jetbrains.annotations.ApiStatus;

/**
 * Allows you to provide the URI of a class whose classpath item
 */
@ApiStatus.Internal
public final class ResourceResolver {

  private ResourceResolver() {
    throw new UnsupportedOperationException();
  }

  /**
   * Allows you to provide the URI of a class whose classpath item
   *
   * @param clazz the class, which should resolve the classpath item URI from
   * @return the uri of the classpath element or null if an exception was caught
   * @see Class
   * @see java.security.ProtectionDomain
   * @see java.security.CodeSource
   */
  public static URI resolveURIFromResourceByClass(Class<?> clazz) {
    Preconditions.checkNotNull(clazz);

    try {
      return clazz.getProtectionDomain().getCodeSource().getLocation().toURI();
    } catch (URISyntaxException exception) {
      exception.printStackTrace();
    }

    return null;
  }

}
