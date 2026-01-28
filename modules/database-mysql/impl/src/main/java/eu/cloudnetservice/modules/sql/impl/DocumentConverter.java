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
import org.jetbrains.annotations.NotNull;
import org.jooq.Converter;
import org.jooq.JSONB;

public class DocumentConverter implements Converter<JSONB, Document> {

  public static final DocumentConverter INSTANCE = new DocumentConverter();

  private DocumentConverter() {
  }

  @Override
  public Document from(JSONB databaseObject) {
    return DocumentFactory.json().parse(databaseObject.data());
  }

  @Override
  public JSONB to(Document userObject) {
    return JSONB.valueOf(userObject.toString());
  }

  @Override
  public @NotNull Class<JSONB> fromType() {
    return JSONB.class;
  }

  @Override
  public @NotNull Class<Document> toType() {
    return Document.class;
  }
}
