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

package de.dytanic.cloudnet.driver.network.http.content;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.driver.util.FileMimeTypeHelper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

record ClassLoaderContentStreamProvider(
  @NotNull String pathPrefix,
  @NotNull ClassLoader contentSource
) implements ContentStreamProvider {

  @Override
  public @Nullable StreamableContent provideContent(@NotNull String path) {
    var resourceLocation = this.pathPrefix + path;
    Preconditions.checkArgument(!resourceLocation.contains(".."), "File traversal for path " + path);

    var contentLocationUrl = this.contentSource.getResource(resourceLocation);
    return contentLocationUrl == null
      ? null
      : URLStreamableContent.of(FileMimeTypeHelper.getFileType(resourceLocation), contentLocationUrl);
  }

  private record URLStreamableContent(
    @NotNull String contentType,
    @NotNull URL contentLocationUrl
  ) implements StreamableContent {

    public static @NotNull URLStreamableContent of(@NotNull String contentType, @NotNull URL contentLocationUrl) {
      return new URLStreamableContent(contentType + "; charset=UTF-8", contentLocationUrl);
    }

    @Override
    public @NotNull InputStream openStream() throws IOException {
      return this.contentLocationUrl.openStream();
    }
  }
}
