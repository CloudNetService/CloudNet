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

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.TextFormat;
import org.jetbrains.annotations.Nullable;

public final class AdventureTextFormatLookup {

  private static final String ALL_CHARS;
  private static final List<String> ALL_NAMES;
  private static final List<TextFormat> FORMATS;

  static {
    Map<TextFormat, String> formats = new LinkedHashMap<>(16 + 5); // colors + decorations
    // colors
    formats.put(NamedTextColor.BLACK, "0");
    formats.put(NamedTextColor.DARK_BLUE, "1");
    formats.put(NamedTextColor.DARK_GREEN, "2");
    formats.put(NamedTextColor.DARK_AQUA, "3");
    formats.put(NamedTextColor.DARK_RED, "4");
    formats.put(NamedTextColor.DARK_PURPLE, "5");
    formats.put(NamedTextColor.GOLD, "6");
    formats.put(NamedTextColor.GRAY, "7");
    formats.put(NamedTextColor.DARK_GRAY, "8");
    formats.put(NamedTextColor.BLUE, "9");
    formats.put(NamedTextColor.GREEN, "a");
    formats.put(NamedTextColor.AQUA, "b");
    formats.put(NamedTextColor.RED, "c");
    formats.put(NamedTextColor.LIGHT_PURPLE, "d");
    formats.put(NamedTextColor.YELLOW, "e");
    formats.put(NamedTextColor.WHITE, "f");
    // decorations
    formats.put(TextDecoration.OBFUSCATED, "k");
    formats.put(TextDecoration.BOLD, "l");
    formats.put(TextDecoration.STRIKETHROUGH, "m");
    formats.put(TextDecoration.UNDERLINED, "n");
    formats.put(TextDecoration.ITALIC, "o");

    // copy into immutable, this method prevents us from (un-) boxing in runtime
    FORMATS = new LinkedList<>(formats.keySet());
    ALL_CHARS = String.join("", formats.values());
    ALL_NAMES = formats.keySet().stream().map(TextFormat::toString).map(String::toLowerCase).toList();
  }

  private AdventureTextFormatLookup() {
    throw new UnsupportedOperationException();
  }

  public static @Nullable TextFormat findFormat(@NonNull String name) {
    // find the index of the char
    var formatIndex = ALL_NAMES.indexOf(name.toLowerCase());
    return formatIndex == -1 ? null : FORMATS.get(formatIndex);
  }

  public static @Nullable NamedTextColor findColor(@NonNull String name) {
    // check if the format at the position is a color
    var format = findFormat(name);
    return format instanceof NamedTextColor color ? color : null;
  }

  public static @Nullable TextDecoration findDecoration(@NonNull String name) {
    // check if the format at the position is a decoration
    var format = findFormat(name);
    return format instanceof TextDecoration decoration ? decoration : null;
  }

  public static @Nullable Character findFormatChar(@NonNull TextFormat format) {
    var formatIndex = FORMATS.indexOf(format);
    return formatIndex == -1 ? null : ALL_CHARS.charAt(formatIndex);
  }
}
