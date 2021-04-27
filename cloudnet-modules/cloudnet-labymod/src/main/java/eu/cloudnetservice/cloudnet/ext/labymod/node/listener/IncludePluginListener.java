package eu.cloudnetservice.cloudnet.ext.labymod.node.listener;

import de.dytanic.cloudnet.common.io.FileUtils;
import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.util.DefaultModuleHelper;
import de.dytanic.cloudnet.event.service.CloudServicePreStartEvent;
import eu.cloudnetservice.cloudnet.ext.labymod.node.CloudNetLabyModModule;

import java.nio.file.Path;

public final class IncludePluginListener {

    private final CloudNetLabyModModule module;

    public IncludePluginListener(CloudNetLabyModModule module) {
        this.module = module;
    }

    @EventListener
    public void handle(CloudServicePreStartEvent event) {
        if (!this.module.getConfiguration().isEnabled() ||
                !this.module.isSupportedEnvironment(event.getCloudService().getServiceConfiguration().getProcessConfig().getEnvironment())) {
            return;
        }

        Path pluginsFolder = event.getCloudService().getDirectoryPath().resolve("plugins");
        FileUtils.createDirectoryReported(pluginsFolder);

        Path targetFile = pluginsFolder.resolve("cloudnet-labymod.jar");
        FileUtils.deleteFileReported(targetFile);

        if (DefaultModuleHelper.copyCurrentModuleInstanceFromClass(IncludePluginListener.class, targetFile)) {
            DefaultModuleHelper.copyPluginConfigurationFileForEnvironment(
                    IncludePluginListener.class,
                    event.getCloudService().getServiceConfiguration().getProcessConfig().getEnvironment(),
                    targetFile
            );
        }
    }
}