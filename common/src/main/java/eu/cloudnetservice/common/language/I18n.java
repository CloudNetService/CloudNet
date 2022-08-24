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

package eu.cloudnetservice.common.language;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.SetMultimap;
import eu.cloudnetservice.common.io.FileUtil;
import eu.cloudnetservice.common.log.LogManager;
import eu.cloudnetservice.common.log.Logger;
import eu.cloudnetservice.common.unsafe.ResourceResolver;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

/**
 * The main entry point for localization made in the CloudNet system. Language files can be registered in multiple ways
 * to this registry. Multiple language files for the same language will be combined to one entry and can be translated.
 * If multiple language files with the same language key are registered, the first registered language file will be used
 * to translate the requested key.
 * <p>
 * Unregistering language files can be done via the class loader which must be given for message loading.
 *
 * @since 4.0
 */
public final class I18n {

  // https://regex101.com/r/syFEig/1
  private static final Pattern MESSAGE_FORMAT = Pattern.compile("\\{(.+?)\\$.+?\\$}");

  private static final Logger LOGGER = LogManager.logger(I18n.class);
  private static final SetMultimap<String, Entry> REGISTERED_ENTRIES = HashMultimap.create();
  private static final AtomicReference<String> CURRENT_LANGUAGE = new AtomicReference<>("en_US");

  private I18n() {
    throw new UnsupportedOperationException();
  }

  /**
   * Loads all language files which are located in the jar at given class source and registers them to the loader of the
   * given class. All language files in the jar must be located in the {@code lang/} directory and their extension must
   * be properties. Subdirectories are ignored by this method.
   *
   * @param clazzSource the source which tries to register all language files.
   * @throws NullPointerException if the given class source is null.
   */
  public static void loadFromLangPath(@NonNull Class<?> clazzSource) {
    var resourcePath = Path.of(ResourceResolver.resolveURIFromResourceByClass(clazzSource));
    FileUtil.openZipFile(resourcePath, fs -> {
      // get the language directory
      var langDir = fs.getPath("lang/");
      if (Files.notExists(langDir) || !Files.isDirectory(langDir)) {
        throw new IllegalStateException("lang/ must be an existing directory inside the jar to load");
      }
      // visit each file and register it as a language source
      FileUtil.walkFileTree(langDir, ($, sub) -> {
        // try to load and register the language file
        try (var stream = Files.newInputStream(sub)) {
          var lang = sub.getFileName().toString().replace(".properties", "");
          addLanguageFile(lang, stream, clazzSource.getClassLoader());
        } catch (IOException exception) {
          LOGGER.severe("Unable to open language file for reading @ %s", exception, sub);
        }
      }, false, "*.properties");
    });
  }

  /**
   * Registers the language properties file at the given path to the given language and loader source.
   *
   * @param lang   the language to register the file to.
   * @param file   the location of the language file to load.
   * @param source the class loader to which the requester belongs.
   * @throws NullPointerException if either the given language, file or loader is null.
   */
  public static void addLanguageFile(@NonNull String lang, @NonNull Path file, @NonNull ClassLoader source) {
    try (var inputStream = Files.newInputStream(file)) {
      addLanguageFile(lang, inputStream, source);
    } catch (IOException exception) {
      LOGGER.severe("Exception while reading language file", exception);
    }
  }

  /**
   * Registers the language properties file which must be loadable from the given stream to the given language and
   * loader source. This method uses utf-8 to decode the stream.
   *
   * @param lang   the language to register the file to.
   * @param stream the stream from which the properties should get loaded.
   * @param source the class loader to which the requester belongs.
   * @throws NullPointerException if either the given language, stream or loader is null.
   */
  public static void addLanguageFile(@NonNull String lang, @NonNull InputStream stream, @NonNull ClassLoader source) {
    try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
      // load the properties
      var properties = new Properties();
      properties.load(reader);
      // register all language keys
      addLanguageFile(lang, properties, source);
    } catch (IOException exception) {
      LOGGER.severe("Exception while reading language file", exception);
    }
  }

  /**
   * Registers the language properties file to the given language and loader source.
   *
   * @param lang    the language to register the file to.
   * @param entries the entries of the language file to register.
   * @param source  the class loader to which the requester belongs.
   * @throws NullPointerException if either the given language, entries or loader is null.
   */
  public static void addLanguageFile(@NonNull String lang, @NonNull Properties entries, @NonNull ClassLoader source) {
    var messageFormats = ImmutableMap.<String, ThreadLocal<MessageFormat>>builder();
    // register for each property key the associated value wrapped by a MessageFormat for later formatting
    // this also unwraps message formatting keys for easier translation like {0$service$} to {0}
    for (var key : entries.stringPropertyNames()) {
      var format = MESSAGE_FORMAT.matcher(entries.getProperty(key)).replaceAll("{$1}");
      messageFormats.put(key, ThreadLocal.withInitial(() -> new MessageFormat(format, Locale.ROOT)));
    }

    // register all translations for the language
    REGISTERED_ENTRIES.put(lang, new Entry(source, messageFormats.build()));
    LOGGER.fine("Registering language file %s with %d translations", null, lang, entries.size());
  }

  /**
   * Unregisters all language files which were registered by providing the given class loader.
   *
   * @param loader the loader to unregister the language files of.
   * @throws NullPointerException if the given loader is null.
   */
  public static void unregisterLanguageFiles(@NonNull ClassLoader loader) {
    for (var entry : REGISTERED_ENTRIES.entries()) {
      if (entry.getValue().source().equals(loader)) {
        REGISTERED_ENTRIES.remove(entry.getKey(), entry.getValue());
      }
    }
  }

  /**
   * Tries to translate the given message key using all currently registered language files for the language this
   * registry currently uses. This method uses the first language entry which can translate the given key, ignoring all
   * duplicate keys.
   * <p>
   * This method will never return null. However, it does return a string which either indicates that no language files
   * are registered for the current language, or that no registered entry is able to translate the given key.
   *
   * @param messageKey the key of the message to translate.
   * @param args       the arguments for the translation.
   * @throws NullPointerException if either the given key or argument array is null.
   */
  public static String trans(@NonNull String messageKey, @NonNull Object... args) {
    // check if there is at least one entry for the current language
    var entries = REGISTERED_ENTRIES.get(I18n.language());
    if (entries.isEmpty()) {
      return String.format("<no language entry for %s>", I18n.language());
    }

    // use the first entry which is able to translate the given entry
    for (var entry : entries) {
      var result = entry.tryTranslate(messageKey, args);
      if (result != null) {
        // successful translate
        return result;
      }
    }

    // fallthrough if no registered entry can translate the message
    return String.format("<no entry to translate \"%s\" in language \"%s\">", messageKey, I18n.language());
  }

  /**
   * Get the current language to which each message will be translated.
   *
   * @return the current message of this manager.
   */
  public static @NonNull String language() {
    return I18n.CURRENT_LANGUAGE.get();
  }

  /**
   * Gets all the names of the known languages to this translation manager.
   *
   * @return the names of all known languages.
   */
  public static @NonNull Collection<String> knownLanguages() {
    return I18n.REGISTERED_ENTRIES.keys();
  }

  /**
   * Sets the current message to which all messages should get translated. There is no check made if any message is
   * registered for the given language.
   * <p>
   * This method doesn't change to the given language silently if the language is not associated with a translation
   * file.
   *
   * @param language the language this manager should use.
   * @throws NullPointerException if the given language is null.
   */
  public static void language(@NonNull String language) {
    // validate that the language is known before changing it
    if (REGISTERED_ENTRIES.containsKey(language)) {
      I18n.CURRENT_LANGUAGE.set(language);
    }
  }

  /**
   * A registered entry in this registry mapping the loader source and all messages of it.
   *
   * @since 4.0
   */
  private record Entry(@NonNull ClassLoader source, @NonNull Map<String, ThreadLocal<MessageFormat>> languageEntries) {

    /**
     * Tries to translate the given key and formats it with the given arguments. If no mapping for the given key is
     * registered this method simply returns null.
     *
     * @param key  the key to translate.
     * @param args the arguments to use during translation.
     * @return the translated message for the given key or null if the given key is unknown.
     * @throws NullPointerException if the given key of argument array is null.
     */
    public @Nullable String tryTranslate(@NonNull String key, @NonNull Object... args) {
      // try to get the associated format with the key
      var format = this.languageEntries.get(key);
      return format == null ? null : format.get().format(args);
    }
  }
}
