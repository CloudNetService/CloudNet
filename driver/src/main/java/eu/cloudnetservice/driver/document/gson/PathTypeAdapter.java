/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.document.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

final class PathTypeAdapter extends TypeAdapter<Path> {

  @Override
  public void write(@NonNull JsonWriter out, @Nullable Path value) throws IOException {
    TypeAdapters.STRING.write(out, value == null ? null : value.toString().replace(File.separatorChar, '/'));
  }

  @Override
  public @Nullable Path read(@NonNull JsonReader in) throws IOException {
    var path = TypeAdapters.STRING.read(in);
    return path == null ? null : Path.of(path);
  }
}
