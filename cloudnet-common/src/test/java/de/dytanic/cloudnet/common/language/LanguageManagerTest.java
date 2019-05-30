package de.dytanic.cloudnet.common.language;

import de.dytanic.cloudnet.common.Properties;
import org.junit.Assert;
import org.junit.Test;

public class LanguageManagerTest {

  @Test
  public void test() {
    Properties properties = new Properties();
    properties.put("test_message", "Test_Message");

    LanguageManager.setLanguage("en");
    LanguageManager.addLanguageFile("en", properties);

    Assert.assertNotNull(LanguageManager.getMessage("test_message"));
    Assert.assertEquals("Test_Message",
      LanguageManager.getMessage("test_message"));
  }
}