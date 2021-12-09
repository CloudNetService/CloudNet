/*
 * Copyright 2019-2021 CloudNetService team & contributors
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

package de.dytanic.cloudnet.common.column;

import com.google.common.base.Verify;
import de.dytanic.cloudnet.common.StringUtil;
import java.util.Arrays;
import java.util.Comparator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

public final class ColumnEntry {

  private final int columnMinLength;
  private final String[] formattedEntries;

  private ColumnEntry(int columnMinLength, String[] formattedEntries) {
    this.columnMinLength = columnMinLength;
    this.formattedEntries = formattedEntries;
  }

  public static @NotNull ColumnEntry wrap(@NotNull String @NotNull ... entries) {
    Verify.verify(entries.length > 0, "At least one entry must be given");
    // sort the entries - the shortest entry will be the first one, the longest the last one
    Arrays.sort(entries, Comparator.comparingInt(String::length));
    // get the longest entry and fill all other entries with spaces
    int longestLength = entries[entries.length - 1].length();
    for (int i = 0; i < entries.length; i++) {
      String entry = entries[i];
      int entryLength = entry.length();
      // check if spaces are required
      if (entryLength < longestLength) {
        entries[i] = entry + StringUtil.repeat(' ', longestLength - entryLength);
        continue;
      }
      // no spaces required - we can safely stop now as all following entries will have the same size as the longest one
      break;
    }
    // parsing successful!
    return new ColumnEntry(longestLength, entries);
  }

  public @Range(from = 0, to = Integer.MAX_VALUE) int getColumnMinLength() {
    return this.columnMinLength;
  }

  public @NotNull String @NotNull [] getFormattedEntries() {
    return this.formattedEntries;
  }
}
