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

package eu.cloudnetservice.driver.network.http.content;

import com.google.common.base.Preconditions;
import eu.cloudnetservice.driver.util.FileMimeTypeHelper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a content stream source for resources located and streamable from a class loader.
 *
 * @param pathPrefix    the prefix to append to each resource location.
 * @param contentSource the source to try load the content from.
 * @since 4.0
 */
record ClassLoaderContentStreamProvider(
  @NonNull String pathPrefix,
  @NonNull ClassLoader contentSource
) implements ContentStreamProvider {

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable StreamableContent provideContent(@NonNull String path) {
    var resourceLocation = this.pathPrefix + path;
    Preconditions.checkArgument(!resourceLocation.contains(".."), "File traversal for path " + path);

    var contentLocationUrl = this.contentSource.getResource(resourceLocation);
    return contentLocationUrl == null
      ? null
      : URLStreamableContent.of(FileMimeTypeHelper.fileType(resourceLocation), contentLocationUrl);
  }

  /**
   * A resource located at a specific url.
   *
   * @param contentType        the content type of the stream.
   * @param contentLocationUrl the content location in form of an url.
   * @since 4.0
   */
  private record URLStreamableContent(
    @NonNull String contentType,
    @NonNull URL contentLocationUrl
  ) implements StreamableContent {

    public static @NonNull URLStreamableContent of(@NonNull String contentType, @NonNull URL contentLocationUrl) {
      return new URLStreamableContent(contentType + "; charset=UTF-8", contentLocationUrl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull InputStream openStream() throws IOException {
      return this.contentLocationUrl.openStream();
    }
  }
}
