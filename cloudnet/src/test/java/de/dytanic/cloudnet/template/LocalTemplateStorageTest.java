package de.dytanic.cloudnet.template;

import de.dytanic.cloudnet.common.collection.Iterables;
import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.function.Predicate;
import org.junit.Assert;
import org.junit.Test;

public final class LocalTemplateStorageTest {

  @Test
  public void testTemplateStorage() throws Exception {
    File directory = new File("build/local_template_storage");
    ITemplateStorage storage = new LocalTemplateStorage(directory);

    try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      InputStream inputStream = LocalTemplateStorageTest.class
        .getClassLoader()
        .getResourceAsStream("local_template_storage.zip")) {
      FileUtils.copy(inputStream, byteArrayOutputStream);
      storage.deploy(byteArrayOutputStream.toByteArray(),
        new ServiceTemplate("Test", "default", "local"));
      Assert.assertTrue(
        new File(directory, "Test/default/plugins/test_file.yml").exists());
    }

    Assert.assertEquals(1, storage.getTemplates().size());
    Assert.assertNotNull(Iterables
      .first(storage.getTemplates(), new Predicate<ServiceTemplate>() {
        @Override
        public boolean test(ServiceTemplate serviceTemplate) {
          return serviceTemplate.getPrefix().equals("Test") &&
            serviceTemplate.getName().equals("default");
        }
      }));

    storage.deploy(new File(directory, "Test/default"),
      new ServiceTemplate("Lobby", "fun", "local"));
    Assert.assertTrue(
      new File(directory, "Lobby/fun/plugins/test_file.yml").exists());

    storage.copy(new ServiceTemplate("Test", "default", "local"),
      new File(directory, "Test/copied"));
    Assert.assertTrue(
      new File(directory, "Test/copied/plugins/test_file.yml").exists());

    FileUtils.delete(directory);
    Assert.assertFalse(directory.exists());
  }
}