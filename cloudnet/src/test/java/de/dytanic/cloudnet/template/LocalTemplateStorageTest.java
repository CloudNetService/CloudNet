package de.dytanic.cloudnet.template;

import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import de.dytanic.cloudnet.driver.template.TemplateStorage;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

public final class LocalTemplateStorageTest {

    @Test
    public void testTemplateStorage() throws Exception {
        File directory = new File("build/local_template_storage");
        TemplateStorage storage = new LocalTemplateStorage(directory);

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             InputStream inputStream = LocalTemplateStorageTest.class.getClassLoader().getResourceAsStream("local_template_storage.zip")) {
            FileUtils.copy(inputStream, byteArrayOutputStream);
            storage.deploy(byteArrayOutputStream.toByteArray(), new ServiceTemplate("Test", "default", "local"));
            Assert.assertTrue(new File(directory, "Test/default/plugins/test_file.yml").exists());
        }

        Assert.assertEquals(1, storage.getTemplates().size());
        Assert.assertNotNull(storage.getTemplates().stream().filter(serviceTemplate -> serviceTemplate.getPrefix().equals("Test") &&
                serviceTemplate.getName().equals("default")).findFirst().orElse(null));

        storage.deploy(new File(directory, "Test/default"), new ServiceTemplate("Lobby", "fun", "local"));
        Assert.assertTrue(new File(directory, "Lobby/fun/plugins/test_file.yml").exists());

        storage.copy(new ServiceTemplate("Test", "default", "local"), new File(directory, "Test/copied"));
        Assert.assertTrue(new File(directory, "Test/copied/plugins/test_file.yml").exists());

        FileUtils.delete(directory);
        Assert.assertFalse(directory.exists());
    }
}