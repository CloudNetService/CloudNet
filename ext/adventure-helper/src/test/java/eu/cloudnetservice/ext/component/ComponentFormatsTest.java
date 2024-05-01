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

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ComponentFormatsTest {

  @Test
  void testBasicConversion() {
    // Hello World
    var input = MiniMessage.miniMessage().deserialize("<red>Hello </red><aqua>W</aqua><gold>orld</gold> ");

    var outputColored = ComponentFormats.LEGACY.fromAdventure(input);
    var outputPlain = ComponentFormats.PLAIN.fromAdventure(input);

    Assertions.assertEquals("§cHello §bW§6orld§r ", outputColored);
    Assertions.assertEquals("Hello World ", outputPlain);
  }

  @Test
  void testHexFormatFromAdventureToLegacyHex() {
    // Hello World
    var input = "<color:#084cfb>H</color>"
      + "<color:#1a5ffb>e</color>"
      + "<color:#2d71fb>l</color>"
      + "<color:#3f84fc>l</color>"
      + "<color:#5196fc>o </color>"
      + "<color:#64a9fc>W</color>"
      + "<color:#76bbfc>o</color>"
      + "<color:#88cefd>r</color>"
      + "<color:#9be0fd>l</color>"
      + "<color:#adf3fd>d</color>";
    var bukkit = "§x§0§8§4§c§f§bH§x§1§a§5§f§f§be§x§2§d§7§1§f§bl§x§3§f§8§4§f§cl§x§5§1§9§6§f§co "
      + "§x§6§4§a§9§f§cW§x§7§6§b§b§f§co§x§8§8§c§e§f§dr§x§9§b§e§0§f§dl§x§a§d§f§3§f§dd";

    var convertedToAdventure = MiniMessage.miniMessage().deserialize(input);
    var convertedToBukkit = ComponentFormats.LEGACY_HEX.fromAdventure(convertedToAdventure);

    Assertions.assertEquals(bukkit, convertedToBukkit);
  }
}
