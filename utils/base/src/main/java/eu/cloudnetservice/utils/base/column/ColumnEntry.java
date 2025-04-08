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
import lombok.NonNull;
import org.jetbrains.annotations.Range;

/**
 * Represents a column in a table view.
 *
 * @param columnMinLength  the min length the column needs to have the equal length for all entries.
 * @param formattedEntries the formatted version of all entries for the column.
 * @since 4.0
 */
public record ColumnEntry(
  @Range(from = 0, to = Integer.MAX_VALUE) int columnMinLength,
  @NonNull String[] formattedEntries
) {

  /**
   * Wraps the given entries into a column entry. This method pads the given entries and automatically calculates the
   * required length of the column.
   *
   * @param entries the entries to wrap into a column entry.
   * @return a formatted column entry based on the given entries.
   * @throws NullPointerException if the given entries are null.
   */
  public static @NonNull ColumnEntry wrap(@NonNull String... entries) {
    var entriesCopy = Arrays.copyOf(entries, entries.length);

    // get the longest entry and fill all other entries with spaces
    var longestLength = 0;
    for (var entry : entriesCopy) {
      longestLength = Math.max(longestLength, entry.length());
    }

    // bring all entries to one length by appending spaces
    for (var i = 0; i < entriesCopy.length; i++) {
      var entry = entriesCopy[i];
      var entryLength = entry.length();

      // check if spaces are required
      if (entryLength < longestLength) {
        entriesCopy[i] = entry + " ".repeat(longestLength - entryLength);
      }
    }

    // parsing successful!
    return new ColumnEntry(longestLength, entriesCopy);
  }
}
