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

package eu.cloudnetservice.driver.impl.language;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimaps;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.language.TranslationProvider;
import eu.cloudnetservice.driver.registry.AutoService;
import io.vavr.Tuple2;
import jakarta.inject.Singleton;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.NonNull;
import org.jetbrains.annotations.UnmodifiableView;

/**
 * The default implementation of the internationalization provider.
 *
 * @since 4.0
 */
@Singleton
@AutoService(services = I18n.class, name = "default")
public final class DefaultI18n implements I18n {

  // stack walker to get the caller class of a method, this should only traverse one level in usual cases
  private static final StackWalker CALLER_RETRIEVER_STACK_WALKER =
    StackWalker.getInstance(Set.of(StackWalker.Option.RETAIN_CLASS_REFERENCE, StackWalker.Option.DROP_METHOD_INFO), 1);

  // holds the translation providers for each language in combination with the caller (owner) that registered it
  // this multimap is concurrent and uses a copy on write list as we expect a lot of reads but only a count of writes
  private final ListMultimap<Locale, Tuple2<TranslationProvider, Class<?>>> providersByLanguage =
    Multimaps.newListMultimap(new ConcurrentHashMap<>(), CopyOnWriteArrayList::new);

  // US locale is en-US
  private volatile Locale activeLanguage = Locale.US;

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Locale selectedLanguage() {
    return this.activeLanguage;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void selectLanguage(@NonNull Locale language) {
    this.activeLanguage = language;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull @UnmodifiableView Collection<Locale> availableLanguages() {
    return Collections.unmodifiableCollection(this.providersByLanguage.keySet());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull String translate(@NonNull String key, Object... args) {
    var activeLanguage = this.activeLanguage;
    var providers = this.providersByLanguage.get(activeLanguage);
    for (var providerEntry : providers) {
      var provider = providerEntry._1();
      var translated = provider.translate(key, args);
      if (translated != null) {
        return translated;
      }
    }

    return String.format("<missing translation for %s in %s>", key, activeLanguage.toLanguageTag());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void registerProvider(@NonNull Locale language, @NonNull TranslationProvider provider) {
    var caller = CALLER_RETRIEVER_STACK_WALKER.getCallerClass();
    Tuple2<TranslationProvider, Class<?>> providerEntry = new Tuple2<>(provider, caller);
    this.providersByLanguage.put(language, providerEntry);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("Java8CollectionRemoveIf") // COW ArrayList doesn't support Iterator.remove
  public void unregisterProvider(@NonNull Locale language, @NonNull TranslationProvider provider) {
    var registeredProviders = this.providersByLanguage.get(language);
    for (Tuple2<TranslationProvider, Class<?>> providerEntry : registeredProviders) {
      if (providerEntry._1().equals(provider)) {
        registeredProviders.remove(providerEntry);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void unregisterProviders(@NonNull ClassLoader classLoader) {
    var registeredProviders = this.providersByLanguage.entries();
    for (var entry : registeredProviders) {
      var providerOwner = entry.getValue()._2();
      if (providerOwner.getClassLoader() == classLoader) {
        registeredProviders.remove(entry);
      }
    }
  }
}
