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

package eu.cloudnetservice.modules.report.emitter;

import eu.cloudnetservice.common.StringUtil;
import eu.cloudnetservice.common.document.gson.JsonDocument;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.function.Consumer;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public record ReportDataWriter(@NonNull StringBuilder stringBuffer, @NonNull String indentSpace, int sectionDepth) {

  public ReportDataWriter(@NonNull StringBuilder stringBuffer, int sectionDepth) {
    this(stringBuffer, StringUtil.repeat(' ', sectionDepth), sectionDepth);
  }

  public static @NonNull ReportDataWriter newEmptyWriter() {
    return new ReportDataWriter(new StringBuilder(), 0);
  }

  private static @NonNull ReportDataWriter appendNewLineIfNeeded(@NonNull ReportDataWriter writer, int sectionDepth) {
    var buffer = writer.stringBuffer;
    var lastWrittenChar = buffer.length() - sectionDepth - 1;

    // check if the last written char is not a newline and write a newline char in that case
    if (lastWrittenChar <= 0 || buffer.charAt(lastWrittenChar) != '\n') {
      return forceNewLine(writer);
    }

    // for chaining
    return writer;
  }

  private static @NonNull ReportDataWriter forceNewLine(@NonNull ReportDataWriter writer) {
    writer.stringBuffer.append('\n').append(writer.indentSpace);
    return writer;
  }

  public @NonNull ReportDataWriter beginSection(@NonNull String title) {
    return this.beginSection(writer -> writer.appendString(title));
  }

  public @NonNull ReportDataWriter beginSection(@NonNull Consumer<ReportDataWriter> titleDecorator) {
    // force a newline
    forceNewLine(this);

    // write the title and prepare the next writer
    titleDecorator.accept(this);
    return forceNewLine(new ReportDataWriter(this.stringBuffer, this.sectionDepth + 2));
  }

  public @NonNull ReportDataWriter endSection() {
    ReportDataWriter writer;
    if (this.sectionDepth >= 2) {
      writer = new ReportDataWriter(this.stringBuffer, this.sectionDepth - 2);
    } else {
      // we can't go deeper
      writer = this;
    }

    // append a newline if the previous element wasn't a newline too
    return appendNewLineIfNeeded(writer, this.sectionDepth);
  }

  public @NonNull ReportDataWriter indent() {
    this.stringBuffer.append(this.indentSpace);
    return this;
  }

  public @NonNull ReportDataWriter appendNewline() {
    return appendNewLineIfNeeded(this, this.sectionDepth);
  }

  public @NonNull ReportDataWriter appendInt(int i) {
    this.stringBuffer.append(i);
    return this;
  }

  public @NonNull ReportDataWriter appendLong(long l) {
    this.stringBuffer.append(l);
    return this;
  }

  public @NonNull ReportDataWriter appendFloat(float f) {
    this.stringBuffer.append(f);
    return this;
  }

  public @NonNull ReportDataWriter appendDouble(double d) {
    this.stringBuffer.append(d);
    return this;
  }

  public @NonNull ReportDataWriter appendBoolean(boolean b) {
    this.stringBuffer.append(b);
    return this;
  }

  public @NonNull ReportDataWriter appendString(@NonNull String s) {
    var parts = s.split("\n");
    for (int i = 0; i < parts.length; i++) {
      this.stringBuffer.append(parts[i]);
      if (i != (parts.length - 1)) {
        this.appendNewline();
      }
    }

    return this;
  }

  public @NonNull ReportDataWriter appendAsJson(@Nullable Object obj) {
    // could technically append {} to indicate an empty object, just to make it more clear we actually emit "null"
    return this.appendString(obj == null ? "null" : JsonDocument.newDocument(obj).toPrettyJson());
  }

  public @NonNull ReportDataWriter appendTimestamp(@NonNull DateTimeFormatter formatter, long timestamp) {
    return this.appendTimestamp(formatter, Instant.ofEpochMilli(timestamp));
  }

  public @NonNull ReportDataWriter appendTimestamp(@NonNull DateTimeFormatter formatter, @NonNull Instant timestamp) {
    return this.appendTimestamp(formatter, timestamp.atZone(ZoneId.systemDefault()));
  }

  public @NonNull ReportDataWriter appendTimestamp(
    @NonNull DateTimeFormatter formatter,
    @NonNull TemporalAccessor temporalAccessor
  ) {
    formatter.formatTo(temporalAccessor, this.stringBuffer);
    return this;
  }

  @Override
  public @NonNull String toString() {
    return this.stringBuffer.toString();
  }
}
