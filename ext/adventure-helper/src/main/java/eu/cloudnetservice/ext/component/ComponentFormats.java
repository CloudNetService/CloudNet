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

package eu.cloudnetservice.ext.component;

import dev.derklaro.aerogel.Element;
import dev.derklaro.aerogel.util.Qualifiers;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class ComponentFormats {

  public static final ComponentFormat<Component> ADVENTURE = new AdventureComponentFormat();
  public static final BungeeComponentFormat BUNGEE = new BungeeComponentFormat();
  public static final PlaceholderComponentFormat<String> BUKKIT = new LegacyComponentFormat(() -> BukkitComponentSerializer.legacy());

  public static final PlaceholderComponentFormat<String> LEGACY_HEX = new LegacyComponentFormat(
    LegacyComponentSerializer.builder()
      .character('§')
      .hexColors()
      .useUnusualXRepeatedCharacterHexFormat()
      .flattener(ComponentFlattener.basic()).build()
  );

  public static final PlaceholderComponentFormat<String> LEGACY_HEX_AMPERSAND = new LegacyComponentFormat(
    LegacyComponentSerializer.builder()
      .character('&')
      .hexColors()
      .useUnusualXRepeatedCharacterHexFormat()
      .flattener(ComponentFlattener.basic()).build()
  );
  public static final PlaceholderComponentFormat<String> LEGACY_HEX_AMPERSAND_PRECISE
    = new PreciseLegacyComponentFormat(LEGACY_HEX_AMPERSAND);

  public static final PlaceholderComponentFormat<String> LEGACY = new LegacyComponentFormat(
    LegacyComponentSerializer.builder()
    .character('§')
    .flattener(ComponentFlattener.basic()).build()
  );

  public static final PlaceholderComponentFormat<String> BEDROCK = LEGACY;
  public static final JsonComponentFormat JSON = new JsonComponentFormat();
  public static final PlaceholderComponentFormat<String> PLAIN = new StripColorComponentFormat(LEGACY_HEX);
  static final PlaceholderComponentFormat<String> MINIMESSAGE = new MinimessageComponentFormat();
  public static final PlaceholderComponentFormat<String> USER_INPUT = new ConditionalComponentFormat(
    () -> InjectionLayer.ext().instance(
      Element.forType(boolean.class)
        .requireAnnotation(Qualifiers.named("minimessage"))
    ),
    LEGACY_HEX_AMPERSAND_PRECISE,
    MINIMESSAGE
  );

  private ComponentFormats() {
    throw new UnsupportedOperationException();
  }

  // the first minecraft version to support hex colors
  // https://minecraft.fandom.com/wiki/Java_Edition_1.16
  public static boolean supportsHex(int version) {
    return version >= 735;
  }
}
