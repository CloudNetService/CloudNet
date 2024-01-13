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

import lombok.NonNull;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

final class BungeeComponentFormat extends JavaEditionComponentFormat<BaseComponent[]> {

  private static final char HEX_CHAR = 'x';
  private static final int HEX_SEG_LENGTH = 14;

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
    return true;
  }

  @Override
  public boolean nextSegmentIsHexadecimalFormatting(char[] fullData, int pos, char current, char next) {
    return (current == COLOR_CHAR || current == LEGACY_CHAR)
      && next == HEX_CHAR
      && (pos + HEX_SEG_LENGTH) < fullData.length;
  }

  @Override
  public @NonNull BaseComponent[] encodeStringToComponent(@NonNull String text) {
    return TextComponent.fromLegacyText(text);
  }
}
