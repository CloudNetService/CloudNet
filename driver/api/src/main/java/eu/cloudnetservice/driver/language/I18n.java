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

import eu.cloudnetservice.driver.registry.ServiceRegistry;
import java.util.Collection;
import java.util.Locale;
import lombok.NonNull;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * A registry for translations in different languages. One language is selected as the default language and used to
 * translate all translation requests. Translations can take a variable number of arguments, but can also be fixed which
 * means that they don't take any argument at all. How translations are loaded and messages are formatted is the
 * responsibility of a {@link TranslationProvider}.
 *
 * @since 4.0
 */
public interface I18n {

  /**
   * Get the current default implementation of this translator interface from the service registry. Where possible
   * injection should be preferred over using this method.
   *
   * @return the current default implementation of this translator interface from the service registry.
   */
  static @NonNull I18n i18n() {
    return ServiceRegistry.registry().defaultInstance(I18n.class);
  }

  /**
   * Get the current selected language. If no language was selected specifically, the {@code en-US} locale is returned.
   *
   * @return the current selected language.
   */
  @NonNull
  Locale selectedLanguage();

  /**
   * Sets the given locale as the current selected language. Future translations calls will use translations from the
   * given language. This method does no checks if translations for the given language are present.
   *
   * @param language the language to set as the current selected language.
   * @throws NullPointerException if the given language is null.
   */
  void selectLanguage(@NonNull Locale language);

  /**
   * Get a view of the locales for which a translation provider was registered.
   *
   * @return a view of the locales for which a translation provider was registered.
   */
  @NonNull
  @UnmodifiableView
  Collection<Locale> availableLanguages();

  /**
   * Returns the translated message for the given translation key based on the current selected language, optionally
   * inserting the given arguments into the message. If no translation can be provided for the key in the selected
   * language, a fallback message is returned instead.
   *
   * @param key  the key of the translation to get.
   * @param args the arguments which can optionally be embedded into the translation.
   * @return the translated message for the given key in the current language.
   * @throws NullPointerException     if the given translation key or arguments array is null.
   * @throws IllegalArgumentException if the translation cannot be formatted with the given arguments.
   */
  @NonNull
  String translate(@NonNull String key, Object... args);

  /**
   * Registers a translation provider into this registry. Translation providers are called in registration order when a
   * translation is requested, returning the translated message from the first provider that can provide it. The caller
   * of this method is marked as the owner of the translation provider. This information will be used when a request is
   * made to unregister all translation providers by their class loader.
   *
   * @param language the language for which the provider should be registered.
   * @param provider the provider to register into this registry for the given language.
   * @throws NullPointerException   if the given language or provider is null.
   * @throws IllegalCallerException if the caller of the method cannot be resolved.
   */
  void registerProvider(@NonNull Locale language, @NonNull TranslationProvider provider);

  /**
   * Unregisters the given provider for the given language from this registry.
   *
   * @param language the language for which the provider was registered.
   * @param provider the provider to unregister from this registry.
   * @throws NullPointerException if the given language or provider is null.
   */
  void unregisterProvider(@NonNull Locale language, @NonNull TranslationProvider provider);

  /**
   * Unregisters all providers from this registry whose owner class was loaded by the given class loader.
   *
   * @param classLoader the class loader of the owner of the providers to be unregistered.
   * @throws NullPointerException if the given class loader is null.
   */
  void unregisterProviders(@NonNull ClassLoader classLoader);
}
