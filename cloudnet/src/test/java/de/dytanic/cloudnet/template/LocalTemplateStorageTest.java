package de.dytanic.cloudnet.template;

import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Assert;
import org.junit.Test;

public final class LocalTemplateStorageTest {

  @Test
  public void testTemplateStorage() throws Exception {
    Path directory = Paths.get("build/local_template_storage");
    ClusterSynchronizedTemplateStorage storage = new LocalTemplateStorage(directory);
    storage.toggleSynchronization(false);

    try (InputStream fileStream = LocalTemplateStorageTest.class.getClassLoader()
      .getResourceAsStream("local_template_storage.zip")) {
      Assert.assertNotNull(fileStream);

      storage.deploy(fileStream, new ServiceTemplate("Test", "default", "local"));
      Assert.assertTrue(Files.exists(directory.resolve("Test/default/plugins/test_file.yml")));
    }

    Assert.assertEquals(1, storage.getTemplates().size());
    Assert.assertNotNull(
      storage.getTemplates().stream().filter(serviceTemplate -> serviceTemplate.getPrefix().equals("Test") &&
        serviceTemplate.getName().equals("default")).findFirst().orElse(null));

    storage.deploy(directory.resolve("Test/default"), new ServiceTemplate("Lobby", "fun", "local"));
    Assert.assertTrue(Files.exists(directory.resolve("Lobby/fun/plugins/test_file.yml")));
    Assert.assertTrue(Files.exists(directory.resolve("Test/default/plugins/test_file.yml")));

    storage.copy(new ServiceTemplate("Test", "default", "local"), directory.resolve("Test/copied"));
    Assert.assertTrue(Files.exists(directory.resolve("Test/copied/plugins/test_file.yml")));

    FileUtils.delete(directory);
    Assert.assertFalse(Files.exists(directory));
  }
}
