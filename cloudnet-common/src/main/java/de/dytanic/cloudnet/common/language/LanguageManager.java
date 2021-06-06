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

package de.dytanic.cloudnet.common.language;

import de.dytanic.cloudnet.common.Properties;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * The LanguageManager is a static service, which handles messages from different languages, which are registered or
 * loaded there
 */
public final class LanguageManager {

  private static final Map<String, Properties> LANGUAGE_CACHE = new HashMap<>();
  /**
   * The current language, which the getMessage() method will the message return
   */
  private static volatile String language;

  private LanguageManager() {
    throw new UnsupportedOperationException();
  }

  /**
   * Resolve and returns the following message in the language which is currently set as member "language"
   *
   * @param property the following message property, which should sort out
   * @return the message which is defined in language cache or a fallback message like {@code "<language LANGUAGE not
   * found>"} or {@code "<message property not found in LANGUAGE>"}
   */
  public static String getMessage(String property) {
    if (language == null || !LANGUAGE_CACHE.containsKey(language)) {
      return "<language " + language + " not found>";
    }

    return LANGUAGE_CACHE.get(language)
      .getOrDefault(property, "<message " + property + " not found in language " + language + ">");
  }

  /**
   * Add a new language properties, which can resolve with getMessage() in the configured language.
   *
   * @param language   the language, which should append
   * @param properties the properties which will add in the language as parameter
   */
  public static void addLanguageFile(String language, Properties properties) {
    if (language == null || properties == null) {
      return;
    }

    if (LANGUAGE_CACHE.containsKey(language)) {
      LANGUAGE_CACHE.get(language).putAll(properties);
    } else {
      LANGUAGE_CACHE.put(language, properties);
    }
  }

  /**
   * Add a new language properties, which can resolve with getMessage() in the configured language.
   *
   * @param language the language, which should append
   * @param file     the properties which will add in the language as parameter
   */
  @Deprecated
  public static void addLanguageFile(String language, File file) {
    addLanguageFile(language, file.toPath());
  }

  /**
   * Add a new language properties, which can resolve with getMessage() in the configured language.
   *
   * @param language the language, which should append
   * @param file     the properties which will add in the language as parameter
   */
  public static void addLanguageFile(String language, Path file) {
    try (InputStream inputStream = Files.newInputStream(file)) {
      addLanguageFile(language, inputStream);
    } catch (IOException exception) {
      exception.printStackTrace();
    }
  }

  /**
   * Add a new language properties, which can resolve with getMessage() in the configured language.
   *
   * @param language    the language, which should append
   * @param inputStream the properties which will add in the language as parameter
   */
  public static void addLanguageFile(String language, InputStream inputStream) {
    try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
      addLanguageFile(language, reader);
    } catch (IOException exception) {
      exception.printStackTrace();
    }
  }

  /**
   * Add a new language properties, which can resolve with getMessage() in the configured language.
   *
   * @param language the language, which should append
   * @param reader   the properties which will be added in the language as parameter
   */
  public static void addLanguageFile(String language, Reader reader) {
    Properties properties = new Properties();

    try {
      properties.load(reader);
    } catch (IOException exception) {
      exception.printStackTrace();
    }

    addLanguageFile(language, properties);
  }

  public static String getLanguage() {
    return LanguageManager.language;
  }

  public static void setLanguage(String language) {
    LanguageManager.language = language;
  }
}
