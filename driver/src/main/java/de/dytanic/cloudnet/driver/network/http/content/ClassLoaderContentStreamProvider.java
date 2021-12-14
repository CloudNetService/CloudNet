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

final class ClassLoaderContentStreamProvider implements ContentStreamProvider {

  private final String pathPrefix;
  private final ClassLoader contentSource;

  public ClassLoaderContentStreamProvider(String pathPrefix, ClassLoader contentSource) {
    this.pathPrefix = pathPrefix;
    this.contentSource = contentSource;
  }

  @Override
  public @Nullable StreamableContent provideContent(@NotNull String path) {
    var resourceLocation = this.pathPrefix + path;
    Preconditions.checkArgument(!resourceLocation.contains(".."), "File traversal for path " + path);

    var contentLocationUrl = this.contentSource.getResource(resourceLocation);
    return contentLocationUrl == null
      ? null
      : new URLStreamableContent(FileMimeTypeHelper.getFileType(resourceLocation), contentLocationUrl);
  }

  private static final class URLStreamableContent implements StreamableContent {

    private final String contentType;
    private final URL contentLocationUrl;

    public URLStreamableContent(String contentType, URL contentLocationUrl) {
      this.contentType = contentType + "; charset=UTF-8";
      this.contentLocationUrl = contentLocationUrl;
    }

    @Override
    public @NotNull InputStream openStream() throws IOException {
      return this.contentLocationUrl.openStream();
    }

    @Override
    public @NotNull String contentType() {
      return this.contentType;
    }
  }
}
