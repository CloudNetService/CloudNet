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

package eu.cloudnetservice.ext.platforminject.processor.util;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.ConfigFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public final class ConfigUtil {

  private ConfigUtil() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull Config tap(@NonNull ConfigFormat<Config> format, @NonNull Consumer<Config> builder) {
    var config = format.createConfig();
    builder.accept(config);
    return config;
  }

  public static void putIfValuesPresent(@NonNull Config config, @NonNull String key, @Nullable Object[] values) {
    if (values != null && values.length > 0) {
      config.add(key, List.of(values));
    }
  }

  public static void putIfValuesPresent(@NonNull Config config, @NonNull String key, @Nullable Collection<?> values) {
    if (values != null && !values.isEmpty()) {
      config.add(key, values);
    }
  }

  public static void putIfValuesPresent(@NonNull Config config, @NonNull String key, @Nullable Map<?, ?> values) {
    if (values != null && !values.isEmpty()) {
      config.add(key, values);
    }
  }

  public static void putIfNotBlank(@NonNull Config config, @NonNull String key, @Nullable String value) {
    if (value != null && !value.isBlank()) {
      config.add(key, value);
    }
  }

  public static void putIfPresent(@NonNull Config config, @NonNull String key, @Nullable Object value) {
    if (value != null) {
      config.add(key, value);
    }
  }

  public static void putFirstOrDefault(
    @NonNull Config config,
    @NonNull String key,
    @NonNull Iterable<?> values,
    @Nullable String def
  ) {
    // get the first value from the iterator (if any) or fall back to the default value
    var iterator = values.iterator();
    var value = iterator.hasNext() ? iterator.next() : def;

    // put the value into the config, if present
    if (value != null) {
      config.add(key, value);
    }
  }

  public static void putOrDefault(
    @NonNull Config config,
    @NonNull String key,
    @Nullable Object value,
    @NonNull Object def
  ) {
    var configValue = Objects.requireNonNullElse(value, def);
    config.add(key, configValue);
  }
}
