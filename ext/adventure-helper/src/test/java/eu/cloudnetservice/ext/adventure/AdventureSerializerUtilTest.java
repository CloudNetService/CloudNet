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
import net.kyori.adventure.text.format.TextColor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AdventureSerializerUtilTest {

  @Test
  void testOnlyLegacyColors() {
    var serialized = AdventureSerializerUtil.serialize("&7Hello &1W&5orld").children();

    Assertions.assertEquals(serialized.get(0).color(), NamedTextColor.GRAY);
    Assertions.assertEquals(serialized.get(1).color(), NamedTextColor.DARK_BLUE);
    Assertions.assertEquals(serialized.get(2).color(), NamedTextColor.DARK_PURPLE);
  }

  @Test
  void testLegacyAndHexMix() {
    var serialized = AdventureSerializerUtil.serialize("&#000fffHello &1W&5orld").children();

    Assertions.assertEquals(serialized.get(0).color(), TextColor.color(0xfff));
    Assertions.assertEquals(serialized.get(1).color(), NamedTextColor.DARK_BLUE);
    Assertions.assertEquals(serialized.get(2).color(), NamedTextColor.DARK_PURPLE);
  }

  @Test
  void testLegacyAndHexWithBungeeCordMix() {
    var serialized = AdventureSerializerUtil
      .serialize("&x&0&0&0&f&f&fHello &1W§x§0§0§0§b§b§bo&6rld§rBye")
      .children();

    Assertions.assertEquals(5, serialized.size());
    Assertions.assertEquals(TextColor.color(0xfff), serialized.get(0).color());
    Assertions.assertEquals(NamedTextColor.DARK_BLUE, serialized.get(1).color());
    Assertions.assertEquals(TextColor.color(0xbbb), serialized.get(2).color());
    Assertions.assertEquals(NamedTextColor.GOLD, serialized.get(3).color());
    Assertions.assertNull(serialized.get(4).color());
  }
}
