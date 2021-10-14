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

package de.dytanic.cloudnet.common.document.gson;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class JsonDocProperty<E> {

  protected final BiConsumer<E, JsonDocument> appender;

  protected final Function<JsonDocument, E> resolver;

  protected final Consumer<JsonDocument> remover;

  protected final Predicate<JsonDocument> tester;

  public JsonDocProperty(BiConsumer<E, JsonDocument> appender, Function<JsonDocument, E> resolver,
    Consumer<JsonDocument> remover, Predicate<JsonDocument> tester) {
    this.appender = appender;
    this.resolver = resolver;
    this.remover = remover;
    this.tester = tester;
  }

  public BiConsumer<E, JsonDocument> getAppender() {
    return this.appender;
  }

  public Function<JsonDocument, E> getResolver() {
    return this.resolver;
  }

  public Consumer<JsonDocument> getRemover() {
    return this.remover;
  }

  public Predicate<JsonDocument> getTester() {
    return this.tester;
  }
}
