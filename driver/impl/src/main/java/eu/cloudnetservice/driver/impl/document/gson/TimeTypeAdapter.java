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

package eu.cloudnetservice.driver.impl.document.gson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

final class TimeTypeAdapter<T extends TemporalAccessor> extends TypeAdapter<T> {

  private final TemporalQuery<T> query;
  private final DateTimeFormatter formatter;
  private final TypeAdapter<String> stringTypeAdapter;

  private TimeTypeAdapter(
    @NonNull TemporalQuery<T> query,
    @NonNull DateTimeFormatter formatter,
    @NonNull TypeAdapter<String> stringTypeAdapter
  ) {
    this.query = query;
    this.formatter = formatter;
    this.stringTypeAdapter = stringTypeAdapter;
  }

  public static <T extends TemporalAccessor> @NonNull TimeTypeAdapter<T> of(
    @NonNull TemporalQuery<T> query,
    @NonNull DateTimeFormatter formatter,
    @NonNull Gson gsonInstance
  ) {
    return new TimeTypeAdapter<>(query, formatter, gsonInstance.getAdapter(String.class));
  }

  @Override
  public void write(@NonNull JsonWriter out, @Nullable T value) throws IOException {
    this.stringTypeAdapter.write(out, value == null ? null : this.formatter.format(value));
  }

  @Override
  public @Nullable T read(@NonNull JsonReader in) throws IOException {
    var time = this.stringTypeAdapter.read(in);
    return time == null ? null : this.formatter.parse(time, this.query);
  }
}
