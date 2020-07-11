package de.dytanic.cloudnet.module;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.module.IModuleProviderHandler;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import de.dytanic.cloudnet.driver.module.defaults.DefaultModuleProviderHandler;

public final class NodeModuleProviderHandler extends DefaultModuleProviderHandler implements IModuleProviderHandler {

    @Override
    public void handlePostModuleStop(IModuleWrapper moduleWrapper) {
        super.handlePostModuleStop(moduleWrapper);

        CloudNet.getInstance().unregisterPacketListenersByClassLoader(moduleWrapper.getClassLoader());
        CloudNet.getInstance().getHttpServer().removeHandler(moduleWrapper.getClassLoader());
        CloudNet.getInstance().getCommandMap().unregisterCommands(moduleWrapper.getClassLoader());
    }

}