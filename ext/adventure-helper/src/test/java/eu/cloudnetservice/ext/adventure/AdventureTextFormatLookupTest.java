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
    Assertions.assertEquals(NamedTextColor.RED, AdventureTextFormatLookup.findColor('c'));
    Assertions.assertEquals(NamedTextColor.GREEN, AdventureTextFormatLookup.findColor('a'));
    Assertions.assertEquals(NamedTextColor.DARK_BLUE, AdventureTextFormatLookup.findColor('1'));

    Assertions.assertEquals(NamedTextColor.GOLD, AdventureTextFormatLookup.findFormat('6'));
    Assertions.assertEquals(NamedTextColor.DARK_RED, AdventureTextFormatLookup.findFormat('4'));

    Assertions.assertNull(AdventureTextFormatLookup.findColor('k'));
    Assertions.assertNull(AdventureTextFormatLookup.findColor('n'));

    Assertions.assertNull(AdventureTextFormatLookup.findColor('g'));
    Assertions.assertNull(AdventureTextFormatLookup.findColor('h'));
  }

  @Test
  void testFindDecorationByChar() {
    Assertions.assertEquals(TextDecoration.BOLD, AdventureTextFormatLookup.findDecoration('l'));
    Assertions.assertEquals(TextDecoration.ITALIC, AdventureTextFormatLookup.findDecoration('o'));
    Assertions.assertEquals(TextDecoration.STRIKETHROUGH, AdventureTextFormatLookup.findDecoration('m'));

    Assertions.assertEquals(TextDecoration.UNDERLINED, AdventureTextFormatLookup.findFormat('n'));
    Assertions.assertEquals(TextDecoration.OBFUSCATED, AdventureTextFormatLookup.findFormat('k'));

    Assertions.assertNull(AdventureTextFormatLookup.findDecoration('1'));
    Assertions.assertNull(AdventureTextFormatLookup.findDecoration('c'));

    Assertions.assertNull(AdventureTextFormatLookup.findDecoration('g'));
    Assertions.assertNull(AdventureTextFormatLookup.findDecoration('h'));
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
