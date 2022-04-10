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

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * A content provider which tries to load the requested content from one of the given providers.
 *
 * @param providers the providers to use as the backing lookup providers.
 * @since 4.0
 */
record MultipleContentStreamProvider(@NonNull ContentStreamProvider... providers) implements ContentStreamProvider {

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable StreamableContent provideContent(@NonNull String path) {
    for (var provider : this.providers) {
      var content = provider.provideContent(path);
      if (content != null) {
        return content;
      }
    }
    return null;
  }
}
