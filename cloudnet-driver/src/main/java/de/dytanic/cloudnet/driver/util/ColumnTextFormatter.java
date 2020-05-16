package de.dytanic.cloudnet.driver.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
