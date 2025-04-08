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

package eu.cloudnetservice.utils.base.column;

import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ColumnTextFormatterTest {

  @Test
  void testDataFormatting() {
    var formatter = ColumnFormatter.builder()
      .leftSpacer(" ")
      .rightSpacer("")
      .columnLeftBracket('|')
      .columnRightBracket(' ')
      .headerValuesSpacerChar('-')
      .columnTitles("Name", "Rank", "World", "HP")
      .build();

    var entries = new ColumnEntry[]{
      ColumnEntry.wrap("derpeepo", "derklaro", "0utplayyyy"),
      ColumnEntry.wrap("Muted", "Profi", "Player"),
      ColumnEntry.wrap("world", "world_nether", "world_the_end"),
      ColumnEntry.wrap("3", "0", "15")
    };

    var formattedLines = formatter.formatLines(entries);
    var expectedLines = Arrays.asList(
      "| Name       | Rank   | World         | HP ",
      "-------------------------------------------",
      "| derpeepo   | Muted  | world         | 3  ",
      "| derklaro   | Profi  | world_nether  | 0  ",
      "| 0utplayyyy | Player | world_the_end | 15 ");

    Assertions.assertIterableEquals(expectedLines, formattedLines);
  }
}
