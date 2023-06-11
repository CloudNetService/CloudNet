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

package eu.cloudnetservice.driver.network.rpc.generation.api;

import eu.cloudnetservice.common.concurrent.Task;
import eu.cloudnetservice.driver.database.Database;
import eu.cloudnetservice.driver.document.Document;
import org.jetbrains.annotations.Nullable;

public abstract class BaseDatabase implements Database {

  public static final Document TEST_DOCUMENT = Document.newJsonDocument().append("hello", "world");

  @Override
  public @Nullable Document get(String key) {
    return TEST_DOCUMENT;
  }

  @Override
  public Task<Document> getAsync(String key) {
    return Task.completedTask(TEST_DOCUMENT);
  }
}
