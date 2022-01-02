/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.cloudnet.common.column;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Objects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RowBasedFormatterTest {

  @Test
  void testRowBasedFormatting() {
    var formatter = ColumnFormatter.builder()
      .leftSpacer(" ")
      .rightSpacer("")
      .columnLeftBracket('|')
      .columnRightBracket(' ')
      .headerValuesSpacerChar('-')
      .columnTitles("Name", "Rank", "World", "HP")
      .build();

    var rowBasedFormatter = RowBasedFormatter.<Player>builder()
      .defaultFormatter(formatter)
      .column(player -> player.name)
      .column(player -> player.rank)
      .column(player -> player.world)
      .column(player -> player.hp)
      .build();

    var entries = new Player[]{
      new Player("derpeepo", "Muted", "world", 3),
      new Player("derklaro", "Profi", "world_nether", 0),
      new Player("0utplayyyy", "Player", "world_the_end", 15)
    };

    try (var reader = new BufferedReader(new InputStreamReader(
      Objects.requireNonNull(this.getClass().getClassLoader().getResourceAsStream("column_test_data.txt")),
      StandardCharsets.UTF_8
    ))) {
      var output = rowBasedFormatter.format(entries);
      Collection<String> expected = reader.lines().toList();

      Assertions.assertLinesMatch(expected.stream(), output.stream());
    } catch (IOException exception) {
      Assertions.fail(exception);
    }
  }

  private record Player(String name, String rank, String world, int hp) {

  }
}
