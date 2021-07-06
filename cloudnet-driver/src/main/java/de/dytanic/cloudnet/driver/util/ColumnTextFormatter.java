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

package de.dytanic.cloudnet.driver.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public class ColumnTextFormatter {

  private ColumnTextFormatter() {
    throw new UnsupportedOperationException();
  }

  public static String formatInColumns(List<String> entries, int columnCount) {
    return formatInColumns(entries, "", columnCount);
  }

  public static String formatInColumns(List<String> entries, String rowSeparator, int columnCount) {
    StringBuilder builder = new StringBuilder();

    List<Row> rows = toRows(entries, columnCount);

    int highestLineLength = rows.stream()
      .flatMap(row -> row.getColumns().stream())
      .flatMap(column -> Arrays.stream(column.getLines()))
      .mapToInt(String::length)
      .max()
      .orElse(0) + 1;

    for (Row row : rows) {
      int lineCount = row.getHighestLineCount();
      for (int i = 0; i < lineCount; i++) {
        for (Column column : row.getColumns()) {
          String line = column.getLines().length > i ? column.getLines()[i] : "";
          builder.append(line);
          for (int j = 0; j < highestLineLength - line.length(); j++) {
            builder.append(' ');
          }
        }
        builder.append('\n');
      }
      builder.append(rowSeparator);
    }

    return builder.toString();
  }

  private static List<Row> toRows(List<String> entries, int columnCount) {
    List<Row> rows = new ArrayList<>();
    Row currentRow = null;
    for (int i = 0; i < entries.size(); i++) {
      if (currentRow == null || i % columnCount == 0) {
        if (currentRow != null) {
          rows.add(currentRow);
        }
        currentRow = new Row(new ArrayList<>());
      }
      currentRow.columns.add(new Column(entries.get(i).split("\n")));
    }
    if (currentRow != null) {
      rows.add(currentRow);
    }

    return rows;
  }

  public static String[] appendEqual(String[] messages, char fillChar) {
    int longest = Arrays.stream(messages).filter(Objects::nonNull).mapToInt(String::length).max().orElse(0);
    if (longest == 0) {
      return messages;
    }

    String[] result = new String[messages.length];
    for (int i = 0; i < result.length; i++) {
      String message = messages[i];
      if (message == null) {
        message = "";
      }

      StringBuilder builder = new StringBuilder();
      for (int j = 0; j < longest - message.length(); j++) {
        builder.append(fillChar);
      }

      result[i] = message + builder.toString();
    }

    return result;
  }

  public static void appendEqual(@NotNull StringBuilder[] output, String prefix, String suffix, String[] messages,
    char fillChar) {
    String[] result = appendEqual(messages, fillChar);
    for (int i = 0; i < output.length; i++) {
      StringBuilder builder = output[i];
      if (messages[i] != null && prefix != null) {
        builder.append(prefix);
      }
      builder.append(result[i]);
      if (messages[i] != null && suffix != null) {
        builder.append(suffix);
      }
    }
  }

  @SafeVarargs
  public static <T> String[] mapToEqual(Collection<T> values, char fillChar,
    @NotNull PrefixedMessageMapper<T>... mappers) {
    StringBuilder[] output = new StringBuilder[values.size()];
    for (int i = 0; i < output.length; i++) {
      output[i] = new StringBuilder();
    }

    for (PrefixedMessageMapper<T> mapper : mappers) {
      String[] messages = values.stream().map(mapper.getMessageMapper()).toArray(String[]::new);
      appendEqual(output, mapper.getPrefix(), mapper.getSuffix(), messages, fillChar);
    }

    return Arrays.stream(output).map(StringBuilder::toString).toArray(String[]::new);
  }

  private static class Row {

    private final List<Column> columns;

    public Row(List<Column> columns) {
      this.columns = columns;
    }

    public List<Column> getColumns() {
      return this.columns;
    }

    public int getHighestLineCount() {
      return this.columns.stream().mapToInt(column -> column.lines.length).max().orElse(0);
    }

  }

  private static class Column {

    private final String[] lines;

    public Column(String[] lines) {
      this.lines = lines;
    }

    public String[] getLines() {
      return this.lines;
    }
  }

}
