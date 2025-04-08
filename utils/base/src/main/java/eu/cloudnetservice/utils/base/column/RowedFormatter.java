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

import com.google.common.base.Preconditions;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;

/**
 * A formatter for rows based on columns. The columns are generated from functions given to this formatter.
 *
 * @param defaultFormatter the default formatter to use to turn the formatted rows into a table.
 * @param columns          the column level data extractors.
 * @param <T>              the type of element converted to a table by this formatter
 * @since 4.0
 */
public record RowedFormatter<T>(
  @NonNull ColumnFormatter defaultFormatter,
  @NonNull List<Function<T, Object>> columns
) {

  /**
   * Constructs a new builder for a rowed formatter.
   *
   * @param <T> the type of elements that will be handled by the rowed formatter.
   * @return a new builder for a rowed formatter.
   */
  public static <T> @NonNull Builder<T> builder() {
    return new Builder<>();
  }

  /**
   * Converts the given input data into columns to prepare them to be converted into a table.
   *
   * @param input the input data to parse and convert into columns.
   * @return the prepared columns based on the given input data.
   * @throws NullPointerException if the given input data is null.
   */
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

  /**
   * Converts the given input data into columns to prepare them to be converted into a table.
   *
   * @param input the input data to parse and convert into columns.
   * @return the prepared columns based on the given input data.
   * @throws NullPointerException if the given input data is null.
   */
  public @NonNull ColumnEntry[] convertToColumns(@NonNull T... input) {
    return this.convertToColumns(Arrays.asList(input));
  }

  /**
   * Converts the given input data into a formatted table using the default formatter given to this formatter.
   *
   * @param input the input data to parse and convert into a table.
   * @return the formatted table based on the given input data.
   * @throws NullPointerException if the given input data is null.
   */
  public @NonNull Collection<String> format(@NonNull Collection<T> input) {
    return this.defaultFormatter.formatLines(this.convertToColumns(input));
  }

  /**
   * Converts the given input data into a formatted table using the default formatter given to this formatter.
   *
   * @param input the input data to parse and convert into a table.
   * @return the formatted table based on the given input data.
   * @throws NullPointerException if the given input data is null.
   */
  public @NonNull Collection<String> format(@NonNull T... input) {
    return this.defaultFormatter.formatLines(this.convertToColumns(input));
  }

  /**
   * A builder that can be used to construct a new row based formatter.
   *
   * @param <T> the type of elements that will be handled by the new formatter.
   * @since 4.0
   */
  public static final class Builder<T> {

    private ColumnFormatter defaultFormatter;
    private List<Function<T, Object>> columns = new LinkedList<>();

    /**
     * Sets the default formatter for the columns.
     *
     * @param defaultFormatter the default formatter to use.
     * @return the same builder instance as used to call the method, for chaining.
     * @throws NullPointerException if the given column formatter is null.
     */
    @Contract(value = "_ -> this", pure = true)
    public @NonNull Builder<T> defaultFormatter(@NonNull ColumnFormatter defaultFormatter) {
      this.defaultFormatter = defaultFormatter;
      return this;
    }

    /**
     * Sets the extractor functions for all columns. This method copies the list into this builder, meaning that changes
     * made to the given list are not reflected into this builder.
     *
     * @param columns the extractor functions for all columns, in order.
     * @return the same builder instance as used to call the method, for chaining.
     * @throws NullPointerException if the given column extractor function list is null.
     */
    @Contract(value = "_ -> this", pure = true)
    public @NonNull Builder<T> columns(@NonNull List<Function<T, Object>> columns) {
      this.columns = new LinkedList<>(columns);
      return this;
    }

    /**
     * Sets the extractor function for the next column.
     *
     * @param converter the converter for the next column.
     * @return the same builder instance as used to call the method, for chaining.
     * @throws NullPointerException if the given column extractor function is null.
     */
    @Contract(value = "_ -> this", pure = true)
    public @NonNull Builder<T> column(@NonNull Function<T, Object> converter) {
      this.columns.add(converter);
      return this;
    }

    /**
     * Builds a new row based formatter from this builder. Further changes to this builder do not reflect into the
     * returned formatter.
     *
     * @return a new rowed formatter based on this builder.
     * @throws NullPointerException     if the given default formatter is null.
     * @throws IllegalArgumentException if there are no column extractor function are given.
     */
    public @NonNull RowedFormatter<T> build() {
      Preconditions.checkNotNull(this.defaultFormatter, "no default formatter given");
      Preconditions.checkArgument(!this.columns.isEmpty(), "at least one column must be given");

      return new RowedFormatter<>(this.defaultFormatter, new LinkedList<>(this.columns));
    }
  }
}
