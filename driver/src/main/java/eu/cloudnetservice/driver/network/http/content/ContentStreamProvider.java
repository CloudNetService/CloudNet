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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import lombok.NonNull;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a provider for content streams from any source.
 *
 * @since 4.0
 */
@FunctionalInterface
public interface ContentStreamProvider {

  /**
   * A provider which has multiple providers as its source, always using the first one from the array which can provide
   * the content at the requested path.
   *
   * @param providers the providers the provider is based on.
   * @return a content provider trying to load the requested resource from one of the provided providers.
   * @throws NullPointerException if the given provider array is null.
   */
  static @NonNull ContentStreamProvider allOf(@NonNls ContentStreamProvider... providers) {
    return new MultipleContentStreamProvider(providers);
  }

  /**
   * A provider which tries to load the requested path from one file in a file tree. This provider ensures that the
   * request will not leave the provided directory, throwing an exception in that case. However, the requested path is
   * allowed to be in a subdirectory of the given path.
   *
   * @param path the base path to load the files from.
   * @return a provider which uses a file tree as its source.
   * @throws NullPointerException if the given path is null.
   */
  static @NonNull ContentStreamProvider fileTree(@NonNull Path path) {
    return new FileContentStreamProvider(path);
  }

  /**
   * A provider which tries to load the requested data from the class loader lookup api. This method call is equivalent
   * to {@code ContentStreamProvider.classLoader("", classLoader)}.
   *
   * @param classLoader the loader to load the resources from.
   * @return a provider loading the resources from the given class loader.
   * @throws NullPointerException if the given class loader is null.
   */
  static @NonNull ContentStreamProvider classLoader(@NonNull ClassLoader classLoader) {
    return new ClassLoaderContentStreamProvider("", classLoader);
  }

  /**
   * A provider which tries to load the requested data from the class loader lookup api. The provider will ensure that
   * there is no path traversal happening, by checking if the given path contains {@code ..}.
   *
   * @param classLoader the loader to load the resources from.
   * @param pathPrefix  the path prefix to prepend when trying to load the resources.
   * @return a provider loading the resources from the given class loader.
   * @throws NullPointerException if either the given call loader or path prefix is null.
   */
  static @NonNull ContentStreamProvider classLoader(@NonNull ClassLoader classLoader, @NonNull String pathPrefix) {
    return new ClassLoaderContentStreamProvider(pathPrefix.endsWith("/") ? pathPrefix : pathPrefix + "/", classLoader);
  }

  /**
   * Called to provide the content at the given path. If the locator is unable to locate the content at the path it just
   * returns null to indicate that.
   *
   * @param path the path to the requested content.
   * @return null if the path does not exist, a content representation otherwise.
   * @throws NullPointerException if the given path is null.
   */
  @Nullable StreamableContent provideContent(@NonNull String path);

  /**
   * Represents a wrapper around some kind of content which can be streamed.
   *
   * @since 4.0
   */
  interface StreamableContent {

    /**
     * Opens the stream to the content this provider is wrapping.
     *
     * @return an input stream for the wrapped content.
     * @throws IOException if an i/o error occurs while opening the stream.
     */
    @NonNull InputStream openStream() throws IOException;

    /**
     * Get the content type of the underlying source of the stream.
     *
     * @return the content type.
     */
    @NonNull String contentType();
  }
}
