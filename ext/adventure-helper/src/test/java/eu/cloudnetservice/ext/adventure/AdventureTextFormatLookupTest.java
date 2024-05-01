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

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.TextFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AdventureTextFormatLookupTest {

  @Test
  void testFindColorByChar() {
    Assertions.assertEquals(NamedTextColor.RED, AdventureTextFormatLookup.findColor("red"));
    Assertions.assertEquals(NamedTextColor.GREEN, AdventureTextFormatLookup.findColor("GREEN"));
    Assertions.assertEquals(NamedTextColor.DARK_BLUE, AdventureTextFormatLookup.findColor("dark_blue"));

    Assertions.assertEquals(NamedTextColor.GOLD, AdventureTextFormatLookup.findFormat("gold"));
    Assertions.assertEquals(NamedTextColor.DARK_RED, AdventureTextFormatLookup.findFormat("dark_red"));

    Assertions.assertNull(AdventureTextFormatLookup.findColor("italic"));
    Assertions.assertNull(AdventureTextFormatLookup.findColor("strikethrough"));

    Assertions.assertNull(AdventureTextFormatLookup.findColor(""));
    Assertions.assertNull(AdventureTextFormatLookup.findColor("test"));
  }

  @Test
  void testFindDecorationByChar() {
    Assertions.assertEquals(TextDecoration.BOLD, AdventureTextFormatLookup.findDecoration("bold"));
    Assertions.assertEquals(TextDecoration.ITALIC, AdventureTextFormatLookup.findDecoration("italic"));
    Assertions.assertEquals(TextDecoration.STRIKETHROUGH, AdventureTextFormatLookup.findDecoration("strikethrough"));

    Assertions.assertEquals(TextDecoration.UNDERLINED, AdventureTextFormatLookup.findFormat("underlined"));
    Assertions.assertEquals(TextDecoration.OBFUSCATED, AdventureTextFormatLookup.findFormat("obfuscated"));

    Assertions.assertNull(AdventureTextFormatLookup.findDecoration("dark_blue"));
    Assertions.assertNull(AdventureTextFormatLookup.findDecoration("red"));

    Assertions.assertNull(AdventureTextFormatLookup.findDecoration(""));
    Assertions.assertNull(AdventureTextFormatLookup.findDecoration("test"));
  }

  @Test
  void testFindCharByFormat() {
    Assertions.assertEquals('c', AdventureTextFormatLookup.findFormatChar(NamedTextColor.RED));
    Assertions.assertEquals('6', AdventureTextFormatLookup.findFormatChar(NamedTextColor.GOLD));
    Assertions.assertEquals('1', AdventureTextFormatLookup.findFormatChar(NamedTextColor.DARK_BLUE));

    Assertions.assertEquals('n', AdventureTextFormatLookup.findFormatChar(TextDecoration.UNDERLINED));
    Assertions.assertEquals('k', AdventureTextFormatLookup.findFormatChar(TextDecoration.OBFUSCATED));
    Assertions.assertEquals('m', AdventureTextFormatLookup.findFormatChar(TextDecoration.STRIKETHROUGH));

    Assertions.assertNull(AdventureTextFormatLookup.findFormatChar(new TextFormat() {
    }));
  }
}
