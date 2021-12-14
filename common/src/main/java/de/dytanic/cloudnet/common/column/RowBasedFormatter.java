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
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

public class RowBasedFormatter<T> {

  private final ColumnFormatter defaultFormatter;
  private final List<Function<T, Object>> columns;

  protected RowBasedFormatter(
    @NotNull ColumnFormatter defaultFormatter,
    @NotNull List<Function<T, Object>> columns
  ) {
    this.defaultFormatter = defaultFormatter;
    this.columns = columns;
  }

  public static <T> @NotNull Builder<T> builder() {
    return new Builder<>();
  }

  public @NotNull ColumnEntry[] convertToColumns(@NotNull Collection<T> input) {
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
      if (entry.getFormattedEntries().length == 0
        || (i != 0 && result[i - 1].getFormattedEntries().length != entry.getFormattedEntries().length)) {
        return new ColumnEntry[0];
      }
      // all valid - append
      result[i] = entry;
    }
    // success!
    return result;
  }

  public @NotNull ColumnEntry[] convertToColumns(@NotNull T... input) {
    return this.convertToColumns(Arrays.asList(input));
  }

  public @NotNull Collection<String> format(@NotNull Collection<T> input) {
    return this.defaultFormatter.formatLines(this.convertToColumns(input));
  }

  public @NotNull Collection<String> format(@NotNull T... input) {
    return this.defaultFormatter.formatLines(this.convertToColumns(input));
  }

  public static final class Builder<T> {

    private ColumnFormatter defaultFormatter;
    private List<Function<T, Object>> columns = new LinkedList<>();

    public @NotNull Builder<T> defaultFormatter(@NotNull ColumnFormatter defaultFormatter) {
      this.defaultFormatter = defaultFormatter;
      return this;
    }

    public @NotNull Builder<T> columns(@NotNull List<Function<T, Object>> columns) {
      this.columns = new LinkedList<>(columns);
      return this;
    }

    public @NotNull Builder<T> column(@NotNull Function<T, Object> converter) {
      this.columns.add(converter);
      return this;
    }

    public @NotNull RowBasedFormatter<T> build() {
      Verify.verifyNotNull(this.defaultFormatter, "no default formatter given");
      Verify.verify(!this.columns.isEmpty(), "at least one column must be given");

      return new RowBasedFormatter<>(this.defaultFormatter, this.columns);
    }
  }
}
