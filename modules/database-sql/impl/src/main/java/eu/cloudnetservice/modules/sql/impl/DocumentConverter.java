/*
 * Copyright 2019-present CloudNetService team & contributors
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

package eu.cloudnetservice.modules.sql.impl;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.document.StandardSerialisationStyle;
import org.jooq.Converter;
import org.jooq.JSONB;

final class DocumentConverter {

  public static final Converter<JSONB, Document> INSTANCE = Converter.ofNullable(
    JSONB.class,
    Document.class,
    raw -> DocumentFactory.json().parse(raw.data()),
    document -> JSONB.jsonb(document.serializeToString(StandardSerialisationStyle.COMPACT)));

  private DocumentConverter() {
  }
}
