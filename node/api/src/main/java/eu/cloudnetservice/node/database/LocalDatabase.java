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

package eu.cloudnetservice.node.database;

import eu.cloudnetservice.driver.database.Database;
import eu.cloudnetservice.driver.document.Document;
import java.util.Map;
import java.util.function.BiConsumer;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public interface LocalDatabase extends Database {

  /**
   * Iterates over all entries in the database This option should not be used with big databases Use
   * {@link #iterate(BiConsumer, int)}} instead
   *
   * @param consumer the consumer to pass the entries into
   */
  void iterate(@NonNull BiConsumer<String, Document> consumer);

  /**
   * Iterates over all entries in the database, but in chunks in the given size
   *
   * @param consumer  the consumer to pass the entries into
   * @param chunkSize the chunkSize of the entries
   */
  void iterate(@NonNull BiConsumer<String, Document> consumer, int chunkSize);

  @Nullable Map<String, Document> readChunk(long beginIndex, int chunkSize);
}
