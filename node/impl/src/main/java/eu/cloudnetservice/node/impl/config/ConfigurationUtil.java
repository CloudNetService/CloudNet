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

package eu.cloudnetservice.node.impl.config;

import com.google.common.primitives.Ints;
import eu.cloudnetservice.driver.network.HostAndPort;
import eu.cloudnetservice.utils.base.StringUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

final class ConfigurationUtil {

  static final Function<String, List<HostAndPort>> HOST_AND_PORT_PARSER = value -> {
    // the result
    List<HostAndPort> listeners = new ArrayList<>();
    // read from the value
    var hostAndPorts = value.split(",");
    for (var hostAndPort : hostAndPorts) {
      var splitHostAndPort = hostAndPort.split("%");
      if (splitHostAndPort.length == 2) {
        // try to parse the host and port info
        var port = Ints.tryParse(splitHostAndPort[1]);
        if (port != null) {
          listeners.add(new HostAndPort(splitHostAndPort[0], port));
        }
      }
    }
    return listeners;
  };

  private ConfigurationUtil() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull String get(@NonNull String propertyName, @NonNull String def) {
    // try to read the value from the system properties
    var val = property(propertyName);
    return val == null ? def : val;
  }

  public static @NonNull <T> T get(
    @NonNull String propertyName,
    @NonNull T def,
    @NonNull Function<String, T> mapper
  ) {
    // try to read the value from the system properties
    var val = property(propertyName);
    if (val == null) {
      return def;
    }
    // try to map the input
    try {
      var mapped = mapper.apply(val);
      return mapped == null ? def : mapped;
    } catch (Exception exception) {
      return def;
    }
  }

  private static @Nullable String property(@NonNull String propertyName) {
    var val = System.getProperty(propertyName);
    return val == null ? System.getenv(StringUtil.toUpper(propertyName.replace('.', '_'))) : val;
  }
}
