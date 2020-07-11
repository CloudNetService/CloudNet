package eu.cloudnetservice.cloudnet.ext.labymod.node.listener;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.util.DefaultModuleHelper;
import de.dytanic.cloudnet.event.service.CloudServicePreStartEvent;
import eu.cloudnetservice.cloudnet.ext.labymod.node.CloudNetLabyModModule;

import java.io.File;

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

        new File(event.getCloudService().getDirectory(), "plugins").mkdirs();
        File file = new File(event.getCloudService().getDirectory(), "plugins/cloudnet-labymod.jar");
        file.delete();

        if (DefaultModuleHelper.copyCurrentModuleInstanceFromClass(IncludePluginListener.class, file)) {
            DefaultModuleHelper.copyPluginConfigurationFileForEnvironment(IncludePluginListener.class,
                    event.getCloudService().getServiceConfiguration().getProcessConfig().getEnvironment(), file);
        }
    }
}