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

package eu.cloudnetservice.common.document.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.regex.Pattern;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
final class PatternTypeAdapter extends TypeAdapter<Pattern> {

  @Override
  public void write(JsonWriter out, Pattern value) throws IOException {
    TypeAdapters.STRING.write(out, value == null ? null : value.pattern());
  }

  @Override
  public Pattern read(JsonReader in) throws IOException {
    var pattern = TypeAdapters.STRING.read(in);
    return pattern == null ? null : Pattern.compile(pattern);
  }
}
