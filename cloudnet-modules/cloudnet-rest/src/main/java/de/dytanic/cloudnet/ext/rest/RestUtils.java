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

package de.dytanic.cloudnet.ext.rest;

import com.google.common.collect.Iterables;
import de.dytanic.cloudnet.driver.service.ServiceConfigurationBase;
import java.util.ArrayList;

public final class RestUtils {

  private RestUtils() {
    throw new UnsupportedOperationException();
  }

  public static void replaceNulls(ServiceConfigurationBase configuration) {
    if (configuration.getTemplates() == null) {
      configuration.setTemplates(new ArrayList<>());
    }
    if (configuration.getIncludes() == null) {
      configuration.setIncludes(new ArrayList<>());
    }
    if (configuration.getDeployments() == null) {
      configuration.setDeployments(new ArrayList<>());
    }
  }

  public static <T> T getFirst(Iterable<T> iterable) {
    return getFirst(iterable, null);
  }

  public static <T> T getFirst(Iterable<T> iterable, T def) {
    if (iterable == null) {
      return def;
    } else {
      return Iterables.getFirst(iterable, def);
    }
  }
}
