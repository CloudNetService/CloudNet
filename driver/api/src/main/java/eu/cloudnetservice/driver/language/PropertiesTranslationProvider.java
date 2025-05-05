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

package eu.cloudnetservice.driver.language;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * An implementation of a translation provider which loads the translations from a properties source and uses a message
 * format to insert arguments into the translations. This provider supports a special argument placeholder for the
 * message format: {@code {[index]$[name]$}}, for example {@code {0$name$}}. These placeholders are just replaced with
 * the provided index (so in the example the replacement would be {@code {0}}) to simplify the understanding which
 * argument actually is represented by the index.
 *
 * @since 4.0
 */
public final class PropertiesTranslationProvider implements TranslationProvider {

  // https://regex101.com/r/FaX3tj/1
  private static final Pattern TRANSLATION_ARG_FORMAT_PATTERN = Pattern.compile("\\{(\\d+)\\$.+?\\$}");

  // a lock that must be held when using a MessageFormat to format a translation
  // this is due to the fact that MessageFormats are not thread safe, but we don't expect
  // this high of concurrency on translation requests that each message format needs its own lock
  private final Lock translationFormatLock;

  // mapping of translation keys to their translations, where the value is either
  // a string if the translations has no arguments or a MessageFormat if it does contain one or more args
  private final Map<String, Object> translations;

  /**
   * Constructs a new translation provider instance for the given translations. This constructor is sealed as the map
   * value type is very open and should only be set by trusted code.
   *
   * @param translations the translations that are available to this provider.
   * @throws NullPointerException if the given translations map is null.
   */
  private PropertiesTranslationProvider(@NonNull Map<String, Object> translations) {
    this.translations = Map.copyOf(translations);
    this.translationFormatLock = new ReentrantLock();
  }

  /**
   * Loads a properties document in UTF-8 format from the given input stream. The keys of the properties are used as the
   * translation keys while the values are used as the actual translations.
   *
   * @param inputStream the input stream from which the properties should be loaded.
   * @return a translation provider based on the properties provided by the given input stream.
   * @throws NullPointerException     if the given input stream is null.
   * @throws IllegalArgumentException if the translations couldn't be loaded from the stream.
   */
  public static @NonNull TranslationProvider fromProperties(@NonNull InputStream inputStream) {
    try (var reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
      return fromProperties(reader);
    } catch (IOException exception) {
      throw new IllegalArgumentException("Unable to load translations from stream", exception);
    }
  }

  /**
   * Loads a properties document from the given reader. The keys of the properties are used as the translation keys
   * while the values are used as the actual translations.
   *
   * @param reader the reader from which the properties should be loaded.
   * @return a translation provider based on the properties provided by the given reader.
   * @throws NullPointerException     if the given reader is null.
   * @throws IllegalArgumentException if the translations couldn't be loaded from the stream.
   */
  public static @NonNull TranslationProvider fromProperties(@NonNull Reader reader) {
    try {
      var properties = new Properties();
      properties.load(reader);
      return fromProperties(properties);
    } catch (IOException exception) {
      throw new IllegalArgumentException("Unable to load translations from reader", exception);
    }
  }

  /**
   * Constructs a translation provider based on the given properties. The keys of the properties are used as the
   * translation keys while the values are used as the actual translations.
   *
   * @param properties the properties which hold the key-translation mappings for the provider.
   * @return a translation provider based on the given properties.
   * @throws NullPointerException     if the given properties instance is null.
   * @throws IllegalArgumentException if the translations couldn't be loaded from properties.
   */
  public static @NonNull TranslationProvider fromProperties(@NonNull Properties properties) {
    Map<String, Object> translations = new HashMap<>();
    for (var translationKey : properties.stringPropertyNames()) {
      var translation = properties.getProperty(translationKey);
      if (translation.contains("{") && translation.contains("}")) {
        // translation that might contain an argument which gets inserted during
        var translationWithoutArgPlaceholders = TRANSLATION_ARG_FORMAT_PATTERN.matcher(translation).replaceAll("{$1}");
        var translationFormat = new MessageFormat(translationWithoutArgPlaceholders, Locale.ROOT);
        var formatsInTranslation = translationFormat.getFormats();
        if (formatsInTranslation.length > 0) {
          translations.put(translationKey, translationFormat);
          continue;
        }
      }

      // raw translation without any arguments
      translations.put(translationKey, translation);
    }

    return new PropertiesTranslationProvider(translations);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable String translate(@NonNull String key, Object... args) {
    var translationEntry = this.translations.get(key);
    if (translationEntry == null) {
      return null;
    }

    // raw translation that doesn't hold any arguments
    if (translationEntry instanceof String translation) {
      return translation;
    }

    // if the translation is now a raw string it has to be a MessageFormat which
    // we need to format using the provided arguments inside the formatting lock
    this.translationFormatLock.lock();
    try {
      var translationFormat = (MessageFormat) translationEntry;
      return translationFormat.format(args);
    } finally {
      this.translationFormatLock.unlock();
    }
  }
}
