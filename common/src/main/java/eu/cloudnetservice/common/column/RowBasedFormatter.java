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

package eu.cloudnetservice.common.column;

import com.google.common.base.Preconditions;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import lombok.NonNull;

public record RowBasedFormatter<T>(
  @NonNull ColumnFormatter defaultFormatter,
  @NonNull List<Function<T, Object>> columns
) {

  public static <T> @NonNull Builder<T> builder() {
    return new Builder<>();
  }

  public @NonNull ColumnEntry[] convertToColumns(@NonNull Collection<T> input) {
    var result = new ColumnEntry[this.columns.size()];
    // convert each
    var contents = input instanceof List ? (List<T>) input : new LinkedList<>(input);
    for (var i = 0; i < this.columns.size(); i++) {
      var formatted = new String[input.size()];
      // format each input
      for (var y = 0; y < input.size(); y++) {
        formatted[y] = Objects.toString(this.columns.get(i).apply(contents.get(y)));
      }
      // wrap the formatted string to a column entry
      var entry = ColumnEntry.wrap(formatted);
      // validate that the entry is valid
      if (entry.formattedEntries().length == 0
        || (i != 0 && result[i - 1].formattedEntries().length != entry.formattedEntries().length)) {
        return new ColumnEntry[0];
      }
      // all valid - append
      result[i] = entry;
    }
    // success!
    return result;
  }

  public @NonNull ColumnEntry[] convertToColumns(@NonNull T... input) {
    return this.convertToColumns(Arrays.asList(input));
  }

  public @NonNull Collection<String> format(@NonNull Collection<T> input) {
    return this.defaultFormatter.formatLines(this.convertToColumns(input));
  }

  public @NonNull Collection<String> format(@NonNull T... input) {
    return this.defaultFormatter.formatLines(this.convertToColumns(input));
  }

  public static final class Builder<T> {

    private ColumnFormatter defaultFormatter;
    private List<Function<T, Object>> columns = new LinkedList<>();

    public @NonNull Builder<T> defaultFormatter(@NonNull ColumnFormatter defaultFormatter) {
      this.defaultFormatter = defaultFormatter;
      return this;
    }

    public @NonNull Builder<T> columns(@NonNull List<Function<T, Object>> columns) {
      this.columns = new LinkedList<>(columns);
      return this;
    }

    public @NonNull Builder<T> column(@NonNull Function<T, Object> converter) {
      this.columns.add(converter);
      return this;
    }

    public @NonNull RowBasedFormatter<T> build() {
      Preconditions.checkNotNull(this.defaultFormatter, "no default formatter given");
      Preconditions.checkArgument(!this.columns.isEmpty(), "at least one column must be given");

      return new RowBasedFormatter<>(this.defaultFormatter, this.columns);
    }
  }
}
