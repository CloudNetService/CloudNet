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
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

record ClassLoaderContentStreamProvider(
  @NonNull String pathPrefix,
  @NonNull ClassLoader contentSource
) implements ContentStreamProvider {

  @Override
  public @Nullable StreamableContent provideContent(@NonNull String path) {
    var resourceLocation = this.pathPrefix + path;
    Preconditions.checkArgument(!resourceLocation.contains(".."), "File traversal for path " + path);

    var contentLocationUrl = this.contentSource.getResource(resourceLocation);
    return contentLocationUrl == null
      ? null
      : URLStreamableContent.of(FileMimeTypeHelper.fileType(resourceLocation), contentLocationUrl);
  }

  private record URLStreamableContent(
    @NonNull String contentType,
    @NonNull URL contentLocationUrl
  ) implements StreamableContent {

    public static @NonNull URLStreamableContent of(@NonNull String contentType, @NonNull URL contentLocationUrl) {
      return new URLStreamableContent(contentType + "; charset=UTF-8", contentLocationUrl);
    }

    @Override
    public @NonNull InputStream openStream() throws IOException {
      return this.contentLocationUrl.openStream();
    }
  }
}
