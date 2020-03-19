package eu.cloudnetservice.cloudnet.ext.npcs.node.listener;


import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.util.DefaultModuleHelper;
import de.dytanic.cloudnet.event.service.CloudServicePreStartEvent;
import eu.cloudnetservice.cloudnet.ext.npcs.node.CloudNetNPCModule;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

public class IncludePluginListener {

    private static final String PROTOCOLLIB_DOWNLOAD_URL = "https://github.com/dmulloy2/ProtocolLib/releases/latest/download/ProtocolLib.jar";

    private static final Path PROTOCOLLIB_CACHE_PATH = Paths.get(System.getProperty("cloudnet.tempDir", "temp"), "caches", "ProtocolLib.jar");

    private CloudNetNPCModule npcModule;

    public IncludePluginListener(CloudNetNPCModule npcModule) throws IOException {
        this.npcModule = npcModule;
        this.downloadProtocolLib();
    }

    private void downloadProtocolLib() throws IOException {
        URLConnection urlConnection = new URL(PROTOCOLLIB_DOWNLOAD_URL).openConnection();

        urlConnection.setUseCaches(false);
        urlConnection.setDoOutput(false);

        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
        urlConnection.connect();

        try (InputStream inputStream = urlConnection.getInputStream()) {
            Files.copy(inputStream, PROTOCOLLIB_CACHE_PATH);
        }
    }

    @EventListener
    public void handle(CloudServicePreStartEvent event) {
        if (!event.getCloudService().getServiceConfiguration().getServiceId().getEnvironment().isMinecraftJavaServer()) {
            return;
        }

        boolean installPlugin = this.npcModule.getNPCConfiguration().getConfigurations().stream()
                .anyMatch(npcConfigurationEntry -> Arrays.asList(event.getCloudService().getServiceConfiguration().getGroups()).contains(npcConfigurationEntry.getTargetGroup()));

        File pluginsFolder = new File(event.getCloudService().getDirectory(), "plugins");
        pluginsFolder.mkdirs();

        File file = new File(pluginsFolder, "cloudnet-npcs.jar");
        file.delete();

        try {
            Files.copy(PROTOCOLLIB_CACHE_PATH, pluginsFolder.toPath().resolve("ProtocolLib.jar"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            CloudNetDriver.getInstance().getLogger().error("Unable to copy ProtocolLib!", exception);
            return;
        }

        if (installPlugin && DefaultModuleHelper.copyCurrentModuleInstanceFromClass(IncludePluginListener.class, file)) {
            DefaultModuleHelper.copyPluginConfigurationFileForEnvironment(
                    IncludePluginListener.class,
                    event.getCloudService().getServiceConfiguration().getProcessConfig().getEnvironment(),
                    file
            );
        }
    }

}
