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

package eu.cloudnetservice.ext.component;

import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

final class AdventureComponentFormat extends JavaEditionComponentFormat<Component> {

  private static final char HEX_CHAR = '#';
  private static final int HEX_SEG_LENGTH = 8;

  private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.builder()
    .character(COLOR_CHAR)
    .extractUrls()
    .hexColors()
    .build();

  @Override
  public int hexSegmentLength() {
    return HEX_SEG_LENGTH;
  }

  @Override
  public char hexIndicationChar() {
    return HEX_CHAR;
  }

  @Override
  public boolean usesColorCharAsHexDelimiter() {
    return false;
  }

  @Override
  public boolean nextSegmentIsHexadecimalFormatting(char[] fullData, int pos, char current, char next) {
    return (current == COLOR_CHAR || current == LEGACY_CHAR)
      && next == HEX_CHAR
      && (pos + HEX_SEG_LENGTH) < fullData.length;
  }

  @Override
  public @NonNull Component encodeStringToComponent(@NonNull String text) {
    return SERIALIZER.deserialize(text);
  }
}
