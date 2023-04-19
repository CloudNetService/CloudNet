/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

import eu.cloudnetservice.common.io.FileUtil;
import eu.cloudnetservice.common.util.StringUtil;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class I18nTest {

  @Test
  void testMessageRegistering() {
    var loader = I18nTest.class.getClassLoader();

    var properties = new Properties();
    properties.put("test_message", "Test_Message");

    I18n.addLanguageFile("en", properties, loader);
    I18n.language("en");

    Assertions.assertNotNull(I18n.trans("test_message"));
    Assertions.assertEquals("Test_Message", I18n.trans("test_message"));

    properties = new Properties();
    for (var i = 0; i < 100; i++) {
      properties.put(StringUtil.generateRandomString(5), StringUtil.generateRandomString(5));
    }

    I18n.addLanguageFile("en", properties, loader);
    for (var entry : properties.entrySet()) {
      Assertions.assertEquals(entry.getValue(), I18n.trans(entry.getKey().toString()));
    }
  }

  @Test
  void testMessageFormatting() {
    var directory = Path.of("node/src/main/resources/lang");
    Assertions.assertTrue(Files.exists(directory));
    // walk the lang directory and try to parse all translations to check if there are no unclosed formatting characters
    FileUtil.walkFileTree(
      directory,
      (root, sub) -> I18n.addLanguageFile("test_TEST", sub, I18nTest.class.getClassLoader()),
      true);
  }
}
