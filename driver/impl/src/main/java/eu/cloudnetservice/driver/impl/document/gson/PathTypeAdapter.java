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
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A type adapter that can serialize and deserialize paths of any type into/from json.
 *
 * @since 4.0
 */
final class PathTypeAdapter extends TypeAdapter<Path> {

  private final TypeAdapter<String> stringTypeAdapter;

  /**
   * Constructs a new instance of the path type adapter.
   *
   * @param gsonInstance the gson instance this type adapter instance got bound to.
   * @throws NullPointerException if the given gson instance is null.
   */
  public PathTypeAdapter(@NonNull Gson gsonInstance) {
    this.stringTypeAdapter = gsonInstance.getAdapter(String.class);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void write(@NonNull JsonWriter out, @Nullable Path value) throws IOException {
    this.stringTypeAdapter.write(out, value == null ? null : value.toString().replace(File.separatorChar, '/'));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable Path read(@NonNull JsonReader in) throws IOException {
    var path = this.stringTypeAdapter.read(in);
    return path == null ? null : Path.of(path);
  }
}
