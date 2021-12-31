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

package de.dytanic.cloudnet.driver.network.http.content;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import lombok.NonNull;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface ContentStreamProvider {

  static @NonNull ContentStreamProvider allOf(@NonNls ContentStreamProvider... providers) {
    return new MultipleContentStreamProvider(providers);
  }

  static @NonNull ContentStreamProvider fileTree(@NonNull Path path) {
    return new FileContentStreamProvider(path);
  }

  static @NonNull ContentStreamProvider classLoader(@NonNull ClassLoader classLoader) {
    return new ClassLoaderContentStreamProvider("", classLoader);
  }

  static @NonNull ContentStreamProvider classLoader(@NonNull ClassLoader classLoader, @NonNull String pathPrefix) {
    return new ClassLoaderContentStreamProvider(pathPrefix.endsWith("/") ? pathPrefix : pathPrefix + "/", classLoader);
  }

  @Nullable StreamableContent provideContent(@NonNull String path);

  interface StreamableContent {

    @NonNull InputStream openStream() throws IOException;

    @NonNull String contentType();
  }
}
