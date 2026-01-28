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
import eu.cloudnetservice.node.impl.database.AbstractDatabase;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.JSONB;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Table;
import org.jooq.impl.DSL;

public class JooqDatabase extends AbstractDatabase {

  static final Name KEY_FIELD_NAME = DSL.name("Name");
  static final Field<String> KEY_FIELD = DSL.field(KEY_FIELD_NAME, String.class);
  static final Name DOCUMENT_FIELD_NAME = DSL.name("Document");
  static final Field<Document> DOCUMENT_FIELD = DSL
    .field(DOCUMENT_FIELD_NAME, JSONB.class)
    .convert(DocumentConverter.INSTANCE);

  protected final Name dslName;
  protected final Table<Record> dslTable;
  protected final DSLContext dslContext;

  public JooqDatabase(
    @NonNull String name,
    @NonNull JooqProvider databaseProvider,
    @NonNull DSLContext dslContext
  ) {
    super(name, databaseProvider);

    this.dslName = DSL.name(name);
    this.dslTable = DSL.table(this.dslName);
    this.dslContext = dslContext;
  }

  @Override
  public @Nullable Map<String, Document> readChunk(long beginIndex, int chunkSize) {
    var chunk = this.dslContext.select(KEY_FIELD, DOCUMENT_FIELD)
      .from(this.dslTable)
      .limit(chunkSize)
      .offset(beginIndex)
      .fetchMap(KEY_FIELD, DOCUMENT_FIELD);
    return chunk.isEmpty() ? null : chunk;
  }

  @Override
  public boolean insert(@NonNull String key, @NonNull Document document) {
    return this.dslContext.insertInto(this.dslTable)
      .set(KEY_FIELD, key)
      .set(DOCUMENT_FIELD, document)
      .onConflict(KEY_FIELD)
      .doUpdate()
      .set(DOCUMENT_FIELD, document)
      .execute() > 0;
  }

  @Override
  public boolean contains(@NonNull String key) {
    return this.dslContext.fetchExists(this.dslContext.selectOne().from(this.dslTable).where(KEY_FIELD.eq(key)));
  }

  @Override
  public boolean delete(@NonNull String key) {
    return this.dslContext.delete(this.dslTable).where(KEY_FIELD.eq(key)).execute() > 0;
  }

  @Override
  public @Nullable Document get(@NonNull String key) {
    return this.dslContext
      .select(DOCUMENT_FIELD)
      .from(this.dslTable)
      .where(KEY_FIELD.eq(key))
      .fetchOptional()
      .map(Record1::value1)
      .orElse(null);
  }

  @Override
  public @NonNull Collection<Document> find(@NonNull String fieldName, @Nullable String fieldValue) {
    Map<String, String> filters = HashMap.newHashMap(1);
    filters.put(fieldName, fieldValue);
    return this.find(filters);
  }

  @Override
  public @NonNull Collection<Document> find(@NonNull Map<String, String> filters) {
    List<Condition> conditions = new ArrayList<>();
    for (var entry : filters.entrySet()) {
      var jsonAttribute = DSL.jsonbGetAttributeAsText(DSL.field(DOCUMENT_FIELD_NAME, JSONB.class), entry.getKey());
      conditions.add(jsonAttribute.eq(entry.getValue()));
    }

    return this.dslContext
      .select(DOCUMENT_FIELD)
      .from(this.dslTable)
      .where(conditions)
      .fetch()
      .getValues(DOCUMENT_FIELD);
  }

  @Override
  public @NonNull Collection<String> keys() {
    return this.dslContext
      .select(KEY_FIELD)
      .from(this.dslName)
      .fetch()
      .getValues(KEY_FIELD);
  }

  @Override
  public @NonNull Collection<Document> documents() {
    return this.dslContext.select(DOCUMENT_FIELD).from(this.dslTable).fetch().getValues(DOCUMENT_FIELD);
  }

  @Override
  public @NonNull Map<String, Document> entries() {
    return this.dslContext.select(KEY_FIELD, DOCUMENT_FIELD).from(this.dslTable).fetchMap(KEY_FIELD, DOCUMENT_FIELD);
  }

  @Override
  public void clear() {
    this.dslContext.truncate(this.name).execute();
  }

  @Override
  public long documentCount() {
    return this.dslContext.fetchCount(DSL.table(this.dslName));
  }

  @Override
  public boolean synced() {
    return true;
  }

  @Override
  public void close() {
  }
}
