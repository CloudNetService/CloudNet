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

package eu.cloudnetservice.ext.adventure;

import eu.cloudnetservice.ext.component.ComponentFormats;
import lombok.NonNull;
import net.kyori.adventure.text.Component;

/**
 * @deprecated use Mimimessage instead.
 */
@Deprecated(forRemoval = true)
public final class AdventureSerializerUtil {

  public static final char HEX_CHAR = '#';
  public static final char COLOR_CHAR = '§';
  public static final char LEGACY_CHAR = '&';
  public static final char BUNGEE_HEX_CHAR = 'x';

  private AdventureSerializerUtil() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull String serializeToString(@NonNull String textToSerialize) {
    return ComponentFormats.LEGACY_HEX.fromAdventure(ComponentFormats.LEGACY_HEX_AMPERSAND.toAdventure(textToSerialize));
  }

  public static @NonNull Component serialize(@NonNull String input) {
    return ComponentFormats.LEGACY_HEX.toAdventure(
      ComponentFormats.LEGACY_HEX.fromAdventure(ComponentFormats.LEGACY_HEX_AMPERSAND.toAdventure(input))
    );
  }
}
