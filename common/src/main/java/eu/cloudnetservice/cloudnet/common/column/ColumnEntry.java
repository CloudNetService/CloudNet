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

import lombok.NonNull;
import org.jetbrains.annotations.Range;

public record ColumnEntry(
  @Range(from = 0, to = Integer.MAX_VALUE) int columnMinLength,
  @NonNull String[] formattedEntries
) {

  public static @NonNull ColumnEntry wrap(@NonNull String @NonNull ... entries) {
    // get the longest entry and fill all other entries with spaces
    var longestLength = 0;
    for (var entry : entries) {
      longestLength = Math.max(longestLength, entry.length());
    }
    // bring all entries to one length by appending spaces
    for (var i = 0; i < entries.length; i++) {
      var entry = entries[i];
      var entryLength = entry.length();
      // check if spaces are required
      if (entryLength < longestLength) {
        entries[i] = entry + " ".repeat(longestLength - entryLength);
      }
    }
    // parsing successful!
    return new ColumnEntry(longestLength, entries);
  }
}
