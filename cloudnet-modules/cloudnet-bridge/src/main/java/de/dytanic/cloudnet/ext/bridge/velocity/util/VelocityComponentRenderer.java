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

package de.dytanic.cloudnet.ext.bridge.velocity.util;

import de.dytanic.cloudnet.ext.bridge.BridgeConfigurationProvider;
import java.util.function.UnaryOperator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class VelocityComponentRenderer {

  private VelocityComponentRenderer() {
    throw new UnsupportedOperationException();
  }

  public static Component prefixed(@NotNull String text) {
    return raw(prefix() + text);
  }

  public static Component prefixedTranslation(@NotNull String translationKey) {
    return prefixedTranslation(translationKey, null);
  }

  public static Component prefixedTranslation(@NotNull String translationKey,
    @Nullable UnaryOperator<String> configurator) {
    String message = BridgeConfigurationProvider.load().getMessages().getOrDefault(translationKey, "null");
    if (configurator != null) {
      message = configurator.apply(message);
    }

    return raw(prefix() + message.replace('&', '§'));
  }

  public static Component rawTranslation(@NotNull String translationKey) {
    return prefixedTranslation(translationKey, null);
  }

  public static Component rawTranslation(@NotNull String translationKey, @Nullable UnaryOperator<String> configurator) {
    String message = BridgeConfigurationProvider.load().getMessages().getOrDefault(translationKey, "null");
    if (configurator != null) {
      message = configurator.apply(message);
    }

    return raw(message.replace('&', '§'));
  }

  public static Component raw(@NotNull String text) {
    return LegacyComponentSerializer.legacySection().deserialize(text);
  }

  private static String prefix() {
    return BridgeConfigurationProvider.load().getPrefix().replace('&', '§');
  }
}
