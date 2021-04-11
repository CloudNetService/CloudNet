package de.dytanic.cloudnet.template;

import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.service.ServiceTemplate;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.zip.ZipInputStream;

public final class LocalTemplateStorageTest {

    @Test
    public void testTemplateStorage() throws Exception {
        File directory = new File("build/local_template_storage");
        ITemplateStorage storage = new LocalTemplateStorage(directory);

        try (InputStream fileStream = LocalTemplateStorageTest.class.getClassLoader().getResourceAsStream("local_template_storage.zip");
             ZipInputStream stream = new ZipInputStream(Objects.requireNonNull(fileStream), StandardCharsets.UTF_8)
        ) {
            storage.deploy(stream, new ServiceTemplate("Test", "default", "local"));
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