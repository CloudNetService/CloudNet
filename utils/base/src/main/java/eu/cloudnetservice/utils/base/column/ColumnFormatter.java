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
import eu.cloudnetservice.utils.base.StringUtil;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import lombok.NonNull;
import org.jetbrains.annotations.Contract;

/**
 * A formatter that is highly customizable to build tables from columns.
 *
 * @since 4.0
 */
public final class ColumnFormatter {

  private static final String ENTRY_FORMAT = "%s%s%s%s%s";

  private final String leftSpacer;
  private final String rightSpacer;

  private final char columnLeftBracket;
  private final char columnRightBracket;
  private final char headerValuesSpacerChar;

  private final String[] columnTitles;

  private volatile Collection<String> formattedColumnTitles;

  /**
   * Constructs a new column formatter instance.
   *
   * @param leftSpacer             the spacer to put after the left bracket.
   * @param rightSpacer            the right spacer to put after the column text.
   * @param columnLeftBracket      the bracket to put as the first char of a column.
   * @param columnRightBracket     the bracket to put as the last char of a column.
   * @param headerValuesSpacerChar the char to use in the spacer line between the header and rows.
   * @param columnTitles           the display name of all column titles, in order.
   * @throws NullPointerException if one of the given arguments is null.
   */
  private ColumnFormatter(
    @NonNull String leftSpacer,
    @NonNull String rightSpacer,
    char columnLeftBracket,
    char columnRightBracket,
    char headerValuesSpacerChar,
    @NonNull String[] columnTitles
  ) {
    this.leftSpacer = leftSpacer;
    this.rightSpacer = rightSpacer;
    this.columnLeftBracket = columnLeftBracket;
    this.columnRightBracket = columnRightBracket;
    this.headerValuesSpacerChar = headerValuesSpacerChar;
    this.columnTitles = columnTitles;
  }

  /**
   * Constructs a new builder for a colum formatter.
   *
   * @return a new column formatter builder.
   */
  public static @NonNull Builder builder() {
    return new Builder();
  }

  /**
   * Formats the given column entries into a table based on the formatter configuration. The returned collection is
   * ordered and can contain duplicates (this depends on the given input and how the input is converted).
   *
   * @param entries the entries to format.
   * @return the given entries, formatted as a table, line by line.
   * @throws NullPointerException if the given entries are null.
   */
  public @NonNull Collection<String> formatLines(@NonNull ColumnEntry... entries) {
    // no entries - no pain, just format the header
    if (entries.length == 0) {
      if (this.formattedColumnTitles == null) {
        // initial formatting of the titles
        var builder = new StringBuilder();
        for (var columnTitle : this.columnTitles) {
          builder.append(String.format(
            ENTRY_FORMAT,
            this.columnLeftBracket,
            this.leftSpacer,
            columnTitle,
            this.rightSpacer,
            this.columnRightBracket));
        }
        // cache the result
        this.formattedColumnTitles = Collections.singleton(builder.toString());
      }

      // use the cached formatted title
      return this.formattedColumnTitles;
    } else {
      // for each header find the longest entry (or use the header length if the header is longer)
      var spaceCache = new String[entries.length];
      // format the header
      var builder = new StringBuilder();
      for (var i = 0; i < this.columnTitles.length; i++) {
        var title = this.columnTitles[i];
        var titleLength = title.length();

        // compute the cache - if the title is too short append to the title, otherwise append to the column value
        String ourSpaces;
        if (titleLength > entries[i].columnMinLength()) {
          ourSpaces = "";
          spaceCache[i] = " ".repeat(titleLength - entries[i].columnMinLength());
        } else {
          ourSpaces = " ".repeat(entries[i].columnMinLength() - titleLength);
          spaceCache[i] = "";
        }

        // print the header entry
        builder
          .append(this.columnLeftBracket)
          .append(this.leftSpacer)
          .append(this.columnTitles[i])
          .append(ourSpaces)
          .append(this.rightSpacer)
          .append(this.columnRightBracket);
      }

      // the result cache - it contains each row
      List<String> result = new LinkedList<>();
      result.add(builder.toString());
      // the second line is the spacer between the header and the entries
      result.add(StringUtil.repeat(this.headerValuesSpacerChar, builder.length()));
      // reset the string builder
      builder.setLength(0);
      // get the amount of times we need to loop to fill everything
      var repeatCount = this.columnTitles.length * entries[0].formattedEntries().length;
      // format each row
      var currentDepth = 0;
      for (var i = 0; i <= repeatCount; i++) {
        // step the depth if required
        if (i != 0 && i % this.columnTitles.length == 0) {
          // append the build line to the result
          result.add(builder.toString());
          // reset the string builder
          builder.setLength(0);
          // step the depth
          currentDepth++;
          // stop here if it was the last run - we just need to flush the buffer
          if (i == repeatCount) {
            break;
          }
        }
        // get the index of the entry we want to print
        var index = i - (this.columnTitles.length * currentDepth);
        if (index < 0) {
          // we are still in the first row
          index = i;
        }
        // get the amount of spaces needed to print so that the column looks nice
        var spaces = spaceCache[index];
        // append the current entry
        builder
          .append(this.columnLeftBracket)
          .append(this.leftSpacer)
          .append(entries[index].formattedEntries()[currentDepth])
          .append(spaces)
          .append(this.rightSpacer)
          .append(this.columnRightBracket);
      }

      // formatting completed
      return result;
    }
  }

  /**
   * A builder for a column formatter. By default the builder uses
   * <ol>
   *   <li>{@code |} as the left bracket
   *   <li>a space as the right bracket
   *   <li>a space as the left spacer
   *   <li>an empty string as the right spacer
   *   <li>{@code —} for the header spacing.
   * </ol>
   *
   * @since 4.0
   */
  public static final class Builder {

    private String leftSpacer = " ";
    private String rightSpacer = "";

    private char columnLeftBracket = '|';
    private char columnRightBracket = ' ';
    private char headerValuesSpacerChar = '—';

    private String[] columnTitles = new String[0];

    /**
     * Sets the left spacer to apply to the column. The left spacer is printed after the left bracket. This value
     * defaults to a space.
     *
     * @param leftSpacer the left spacer to use.
     * @return the same builder instance as used to call the method, for chaining.
     * @throws NullPointerException if the given left spacer is null.
     */
    @Contract(value = "_ -> this", pure = true)
    public @NonNull Builder leftSpacer(@NonNull String leftSpacer) {
      this.leftSpacer = leftSpacer;
      return this;
    }

    /**
     * Sets the right spacer to apply to the column. The right spacer is printed after the value of entry and before the
     * right bracket. This value defaults to an empty string.
     *
     * @param rightSpacer the right spacer to use.
     * @return the same builder instance as used to call the method, for chaining.
     * @throws NullPointerException if the given right spacer is null.
     */
    @Contract(value = "_ -> this", pure = true)
    public @NonNull Builder rightSpacer(@NonNull String rightSpacer) {
      this.rightSpacer = rightSpacer;
      return this;
    }

    /**
     * Sets the left bracket to apply to the column. The left bracket is the first char printed in each line. This value
     * defaults to {@code |}.
     *
     * @param columnLeftBracket the left bracket to use.
     * @return the same builder instance as used to call the method, for chaining.
     */
    @Contract(value = "_ -> this", pure = true)
    public @NonNull Builder columnLeftBracket(char columnLeftBracket) {
      this.columnLeftBracket = columnLeftBracket;
      return this;
    }

    /**
     * Sets the right bracket to apply to the column. The right bracket is the last char printed in each line. This
     * value defaults to a space.
     *
     * @param columnRightBracket the right bracket to use.
     * @return the same builder instance as used to call the method, for chaining.
     */
    @Contract(value = "_ -> this", pure = true)
    public @NonNull Builder columnRightBracket(char columnRightBracket) {
      this.columnRightBracket = columnRightBracket;
      return this;
    }

    /**
     * Sets the spacer character to apply between the header of a table and the table values. This value defaults to
     * {@code —}.
     *
     * @param headerValuesSpacerChar the spacer char to use.
     * @return the same builder instance as used to call the method, for chaining.
     */
    @Contract(value = "_ -> this", pure = true)
    public @NonNull Builder headerValuesSpacerChar(char headerValuesSpacerChar) {
      this.headerValuesSpacerChar = headerValuesSpacerChar;
      return this;
    }

    /**
     * Sets the column titles to use for the table. This method copies the given list of titles into this builder,
     * meaning that changes to the given list are not reflected into this builder.
     *
     * @param columnTitles the colum titles.
     * @return the same builder instance as used to call the method, for chaining.
     * @throws NullPointerException if the given column titles list is null.
     */
    @Contract(value = "_ -> this", pure = true)
    public @NonNull Builder columnTitles(@NonNull List<String> columnTitles) {
      this.columnTitles = columnTitles.toArray(new String[0]);
      return this;
    }

    /**
     * Sets the column titles to use for the table. This method copies the given array of titles into this builder,
     * meaning that changes to the given array are not reflected into this builder.
     *
     * @param columnTitles the colum titles.
     * @return the same builder instance as used to call the method, for chaining.
     * @throws NullPointerException if the given column titles array is null.
     */
    public @NonNull Builder columnTitles(@NonNull String... columnTitles) {
      this.columnTitles = Arrays.copyOf(columnTitles, columnTitles.length);
      return this;
    }

    /**
     * Constructs a new column formatter instance from the values supplied to this builder.
     *
     * @return a new column formatter instance.
     * @throws IllegalArgumentException if no column titles were given to this builder.
     */
    @Contract(value = "-> new", pure = true)
    public @NonNull ColumnFormatter build() {
      Preconditions.checkArgument(this.columnTitles.length > 0, "At least one title must be given");
      return new ColumnFormatter(
        this.leftSpacer,
        this.rightSpacer,
        this.columnLeftBracket,
        this.columnRightBracket,
        this.headerValuesSpacerChar,
        this.columnTitles);
    }
  }
}
