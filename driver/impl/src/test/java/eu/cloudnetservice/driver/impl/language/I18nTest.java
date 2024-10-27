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

import eu.cloudnetservice.driver.impl.junit.EnableServicesInject;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.language.PropertiesTranslationProvider;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@EnableServicesInject
public class I18nTest {

  @Test
  void testPropertiesTranslationProviderLoadFromInputStream() throws IOException {
    var properties = new Properties();
    properties.put("hello-world", "Hello, World!");
    properties.put("hello-rob", "Hello, Rob, how was your day?");
    properties.put("hello-generic", "Hello, {0$name$}, how was your day?");

    var out = new ByteArrayOutputStream();
    properties.store(out, "testing translation file");
    var provider = PropertiesTranslationProvider.fromProperties(new ByteArrayInputStream(out.toByteArray()));

    Assertions.assertEquals("Hello, World!", provider.translate("hello-world"));
    Assertions.assertEquals("Hello, Rob, how was your day?", provider.translate("hello-rob"));
    Assertions.assertEquals("Hello, Tester, how was your day?", provider.translate("hello-generic", "Tester"));
    Assertions.assertNull(provider.translate("hello-tester"));
  }

  @Test
  void testPropertiesTranslationProviderFromProperties() {
    var properties = new Properties();
    properties.put("hello-world", "Hello, World!");
    properties.put("hello-rob", "Hello, Rob, how was your day?");
    properties.put("hello-generic", "Hello, {0$name$}, how was your day?");
    var provider = PropertiesTranslationProvider.fromProperties(properties);

    Assertions.assertEquals("Hello, World!", provider.translate("hello-world"));
    Assertions.assertEquals("Hello, Rob, how was your day?", provider.translate("hello-rob"));
    Assertions.assertEquals("Hello, Tester, how was your day?", provider.translate("hello-generic", "Tester"));
    Assertions.assertNull(provider.translate("hello-tester"));
  }

  @Test
  void testMessageFormatCorrectlyUsedWhenNeeded() {
    var properties = new Properties();
    properties.put("1", "Hello, World {0}+{1}");
    properties.put("2", "Hello, World {0$name$}+{1}");
    properties.put("3", "Hello, World {0$name$}+{1,number,integer}");
    var provider = PropertiesTranslationProvider.fromProperties(properties);

    Assertions.assertEquals("Hello, World Rob+1,234,567.89", provider.translate("1", "Rob", 1234567.89));
    Assertions.assertEquals("Hello, World Rob+1,234,567.89", provider.translate("2", "Rob", 1234567.89));
    Assertions.assertEquals("Hello, World Rob+1,234,568", provider.translate("3", "Rob", 1234567.89));

    Assertions.assertEquals("Hello, World null+1,234,567.89", provider.translate("1", null, 1234567.89));
    Assertions.assertEquals("Hello, World Rob+{1}", provider.translate("3", "Rob"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testPropertiesProviderOnlyUsesMessageFormatWhenNeeded() throws ReflectiveOperationException {
    var properties = new Properties();
    properties.put("0", "Hello, World");
    properties.put("1", "Hello, World Tester");
    properties.put("2", "Hello, World {0$name$}");
    properties.put("3", "Hello, World Tester+{1}");
    properties.put("4", "Hello, World Tester '{Testing :)}'");
    var provider = PropertiesTranslationProvider.fromProperties(properties);

    var translationsMapField = PropertiesTranslationProvider.class.getDeclaredField("translations");
    translationsMapField.setAccessible(true);
    var translations = (Map<String, Object>) translationsMapField.get(provider);

    Assertions.assertInstanceOf(String.class, translations.get("0"));
    Assertions.assertInstanceOf(String.class, translations.get("1"));
    Assertions.assertInstanceOf(MessageFormat.class, translations.get("2"));
    Assertions.assertInstanceOf(MessageFormat.class, translations.get("3"));
    Assertions.assertInstanceOf(String.class, translations.get("4"));
  }

  @Test
  void testSelectedLanguageUpdate() {
    var i18n = I18n.i18n();
    Assertions.assertEquals(Locale.US, i18n.selectedLanguage());
    i18n.selectLanguage(Locale.GERMANY);
    Assertions.assertEquals(Locale.GERMANY, i18n.selectedLanguage());
  }

  @Test
  void testTranslationWithMultipleProviders() {
    var properties1 = new Properties();
    properties1.put("1", "Hello World");
    properties1.put("2", "Hello Rob");
    var provider1 = PropertiesTranslationProvider.fromProperties(properties1);

    var properties2 = new Properties();
    properties2.put("1", "Hello Rob");
    properties2.put("3", "Hello Tester");
    var provider2 = PropertiesTranslationProvider.fromProperties(properties2);

    var i18n = I18n.i18n();
    i18n.registerProvider(i18n.selectedLanguage(), provider1);
    i18n.registerProvider(i18n.selectedLanguage(), provider2);

    Assertions.assertEquals("Hello World", i18n.translate("1"));
    Assertions.assertEquals("Hello Rob", i18n.translate("2"));
    Assertions.assertEquals("Hello Tester", i18n.translate("3"));
  }

  @Test
  void testTranslationProviderUnregister() {
    var properties1 = new Properties();
    properties1.put("1", "Hello World");
    var provider1 = PropertiesTranslationProvider.fromProperties(properties1);

    var properties2 = new Properties();
    properties2.put("2", "Hello Rob");
    var provider2 = PropertiesTranslationProvider.fromProperties(properties2);

    var i18n = I18n.i18n();
    i18n.registerProvider(i18n.selectedLanguage(), provider1);
    i18n.registerProvider(i18n.selectedLanguage(), provider2);

    Assertions.assertEquals("Hello World", i18n.translate("1"));
    Assertions.assertEquals("Hello Rob", i18n.translate("2"));

    i18n.unregisterProvider(i18n.selectedLanguage(), provider1);
    Assertions.assertEquals("<missing translation for 1 in en-US>", i18n.translate("1"));
    Assertions.assertEquals("Hello Rob", i18n.translate("2"));
  }

  @Test
  void testTranslationProvidersUnregisterByClassLoader() {
    var properties1 = new Properties();
    properties1.put("1", "Hello World");
    var provider1 = PropertiesTranslationProvider.fromProperties(properties1);

    var properties2 = new Properties();
    properties2.put("2", "Hello Rob");
    var provider2 = PropertiesTranslationProvider.fromProperties(properties2);

    var i18n = I18n.i18n();
    i18n.registerProvider(i18n.selectedLanguage(), provider1);
    i18n.registerProvider(i18n.selectedLanguage(), provider2);

    Assertions.assertEquals("Hello World", i18n.translate("1"));
    Assertions.assertEquals("Hello Rob", i18n.translate("2"));

    i18n.unregisterProviders(I18nTest.class.getClassLoader());
    Assertions.assertEquals("<missing translation for 1 in en-US>", i18n.translate("1"));
    Assertions.assertEquals("<missing translation for 2 in en-US>", i18n.translate("2"));
  }
}
