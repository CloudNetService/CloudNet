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

package eu.cloudnetservice.node.impl.console;

import java.util.regex.Pattern;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jline.jansi.Ansi;

public enum ConsoleColor {

  BLACK("black", '0', Ansi.ansi().reset().fg(Ansi.Color.BLACK).toString()),
  DARK_BLUE("dark_blue", '1', Ansi.ansi().reset().fg(Ansi.Color.BLUE).toString()),
  GREEN("green", '2', Ansi.ansi().reset().fg(Ansi.Color.GREEN).toString()),
  CYAN("cyan", '3', Ansi.ansi().reset().fg(Ansi.Color.CYAN).toString()),
  DARK_RED("dark_red", '4', Ansi.ansi().reset().fg(Ansi.Color.RED).toString()),
  PURPLE("purple", '5', Ansi.ansi().reset().fg(Ansi.Color.MAGENTA).toString()),
  ORANGE("orange", '6', Ansi.ansi().reset().fg(Ansi.Color.YELLOW).toString()),
  GRAY("gray", '7', Ansi.ansi().reset().fg(Ansi.Color.WHITE).toString()),
  DARK_GRAY("dark_gray", '8', Ansi.ansi().reset().fg(Ansi.Color.BLACK).bold().toString()),
  BLUE("blue", '9', Ansi.ansi().reset().fg(Ansi.Color.BLUE).bold().toString()),
  LIGHT_GREEN("light_green", 'a', Ansi.ansi().reset().fg(Ansi.Color.GREEN).bold().toString()),
  AQUA("aqua", 'b', Ansi.ansi().reset().fg(Ansi.Color.CYAN).bold().toString()),
  RED("red", 'c', Ansi.ansi().reset().fg(Ansi.Color.RED).bold().toString()),
  PINK("pink", 'd', Ansi.ansi().reset().fg(Ansi.Color.MAGENTA).bold().toString()),
  YELLOW("yellow", 'e', Ansi.ansi().reset().fg(Ansi.Color.YELLOW).bold().toString()),
  WHITE("white", 'f', Ansi.ansi().reset().fg(Ansi.Color.WHITE).bold().toString()),
  OBFUSCATED("obfuscated", 'k', Ansi.ansi().a(Ansi.Attribute.BLINK_SLOW).toString()),
  BOLD("bold", 'l', Ansi.ansi().a(Ansi.Attribute.UNDERLINE_DOUBLE).toString()),
  STRIKETHROUGH("strikethrough", 'm', Ansi.ansi().a(Ansi.Attribute.STRIKETHROUGH_ON).toString()),
  UNDERLINE("underline", 'n', Ansi.ansi().a(Ansi.Attribute.UNDERLINE).toString()),
  ITALIC("italic", 'o', Ansi.ansi().a(Ansi.Attribute.ITALIC).toString()),
  DEFAULT("default", 'r', Ansi.ansi().reset().toString());

  private static final ConsoleColor[] VALUES = values();
  private static final String LOOKUP = "0123456789abcdefklmnor";
  private static final String RGB_ANSI = "\u001B[38;2;%d;%d;%dm";

  private final String name;
  private final String ansiCode;
  private final char index;

  ConsoleColor(@NonNull String name, char index, @NonNull String ansiCode) {
    this.name = name;
    this.index = index;
    this.ansiCode = ansiCode;
  }

  public static @NonNull String toColoredString(char triggerChar, @NonNull String input) {
    var contentBuilder = new StringBuilder(convertRGBColors(triggerChar, input));

    var breakIndex = contentBuilder.length() - 1;
    for (var i = 0; i < breakIndex; i++) {
      if (contentBuilder.charAt(i) == triggerChar) {
        var format = LOOKUP.indexOf(contentBuilder.charAt(i + 1));
        if (format != -1) {
          var ansiCode = VALUES[format].ansiCode();

          contentBuilder.delete(i, i + 2).insert(i, ansiCode);
          breakIndex += ansiCode.length() - 2;
        }
      }
    }

    return contentBuilder.toString();
  }

  private static @NonNull String convertRGBColors(char triggerChar, @NonNull String input) {
    var replacePattern = Pattern.compile(triggerChar + "#([\\da-fA-F]){6}");
    return replacePattern.matcher(input).replaceAll(result -> {
      // we could use java.awt.Color but that would load unnecessary native libraries
      int hexInput = Integer.decode(result.group().substring(1));
      return String.format(RGB_ANSI, (hexInput >> 16) & 0xFF, (hexInput >> 8) & 0xFF, hexInput & 0xFF);
    });
  }

  public static @NonNull String stripColor(char triggerChar, @NonNull String input) {
    var contentBuilder = new StringBuilder(stripRGBColors(triggerChar, input));

    var breakIndex = contentBuilder.length() - 1;
    for (var i = 0; i < breakIndex; i++) {
      if (contentBuilder.charAt(i) == triggerChar && LOOKUP.indexOf(contentBuilder.charAt(i + 1)) != -1) {
        contentBuilder.delete(i, i + 2);
        breakIndex -= 2;
      }
    }

    return contentBuilder.toString();
  }

  private static @NonNull String stripRGBColors(char triggerChar, @NonNull String input) {
    var replacePattern = Pattern.compile(triggerChar + "#([\\da-fA-F]){6}");
    return replacePattern.matcher(input).replaceAll("");
  }

  public static @Nullable ConsoleColor byChar(char index) {
    for (var color : VALUES) {
      if (color.index == index) {
        return color;
      }
    }

    return null;
  }

  public static @Nullable ConsoleColor lastColor(char triggerChar, @NonNull String text) {
    text = text.trim();
    if (text.length() > 2 && text.charAt(text.length() - 2) == triggerChar) {
      return byChar(text.charAt(text.length() - 1));
    }

    return null;
  }

  @Override
  public @NonNull String toString() {
    return this.ansiCode;
  }

  public @NonNull String displayName() {
    return this.name;
  }

  public @NonNull String ansiCode() {
    return this.ansiCode;
  }

  public char index() {
    return this.index;
  }
}
