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

package eu.cloudnetservice.driver.impl.document.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.cloudnetservice.driver.document.Document;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * A holder for the different gson instances that are used internally.
 *
 * @since 4.0
 */
final class GsonProvider {

  /**
   * The default gson instance. This instance has all the base configuration done in order to be used for other,
   * delegate gson instances.
   */
  static final Gson NORMAL_GSON_INSTANCE = new GsonBuilder()
    .serializeNulls()
    .disableHtmlEscaping()
    .registerTypeAdapterFactory(DelegateTypeAdapterFactory.hierarchyFactory(Path.class, PathTypeAdapter::new))
    .registerTypeAdapterFactory(DelegateTypeAdapterFactory.standardFactory(Pattern.class, PatternTypeAdapter::new))

    // local time, date & date time
    .registerTypeAdapterFactory(DelegateTypeAdapterFactory.standardFactory(LocalTime.class, gson ->
      TimeTypeAdapter.of(LocalTime::from, DateTimeFormatter.ISO_LOCAL_TIME, gson)))
    .registerTypeAdapterFactory(DelegateTypeAdapterFactory.standardFactory(LocalDate.class, gson ->
      TimeTypeAdapter.of(LocalDate::from, DateTimeFormatter.ISO_LOCAL_DATE, gson)))
    .registerTypeAdapterFactory(DelegateTypeAdapterFactory.standardFactory(LocalDateTime.class, gson ->
      TimeTypeAdapter.of(LocalDateTime::from, DateTimeFormatter.ISO_LOCAL_DATE_TIME, gson)))

    // offset time & date time
    .registerTypeAdapterFactory(DelegateTypeAdapterFactory.standardFactory(OffsetTime.class, gson ->
      TimeTypeAdapter.of(OffsetTime::from, DateTimeFormatter.ISO_OFFSET_TIME, gson)))
    .registerTypeAdapterFactory(DelegateTypeAdapterFactory.standardFactory(OffsetDateTime.class, gson ->
      TimeTypeAdapter.of(OffsetDateTime::from, DateTimeFormatter.ISO_OFFSET_DATE_TIME, gson)))

    // zoned date time
    .registerTypeAdapterFactory(DelegateTypeAdapterFactory.standardFactory(ZonedDateTime.class, gson ->
      TimeTypeAdapter.of(ZonedDateTime::from, DateTimeFormatter.ISO_ZONED_DATE_TIME, gson)))

    // instant & duration
    .registerTypeAdapterFactory(DelegateTypeAdapterFactory.standardFactory(Instant.class, gson ->
      TimeTypeAdapter.of(Instant::from, DateTimeFormatter.ISO_INSTANT, gson)))
    .registerTypeAdapterFactory(DelegateTypeAdapterFactory.standardFactory(Duration.class, DurationTypeAdapter::new))

    .registerTypeAdapterFactory(DelegateTypeAdapterFactory.hierarchyFactory(Document.class, DocumentTypeAdapter::new))
    .setFieldNamingStrategy(GsonDocumentFieldNamingStrategy.INSTANCE)
    .addSerializationExclusionStrategy(GsonDocumentExclusionStrategy.SERIALIZE)
    .addDeserializationExclusionStrategy(GsonDocumentExclusionStrategy.DESERIALIZE)
    .create();

  /**
   * Gson instance based on the normal instance but with pretty printing enabled.
   */
  static final Gson PRETTY_PRINTING_GSON_INSTANCE = NORMAL_GSON_INSTANCE.newBuilder()
    .setPrettyPrinting()
    .create();

  private GsonProvider() {
    throw new UnsupportedOperationException();
  }
}
