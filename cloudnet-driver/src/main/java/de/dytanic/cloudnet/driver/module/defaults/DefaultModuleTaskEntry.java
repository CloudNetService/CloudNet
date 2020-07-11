package de.dytanic.cloudnet.driver.module.defaults;

import de.dytanic.cloudnet.driver.module.IModule;
import de.dytanic.cloudnet.driver.module.IModuleTaskEntry;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import de.dytanic.cloudnet.driver.module.ModuleTask;

import java.lang.reflect.Method;

public final class DefaultModuleTaskEntry implements IModuleTaskEntry {

    private final IModuleWrapper moduleWrapper;

    private final ModuleTask taskInfo;

    private final Method handler;

    public DefaultModuleTaskEntry(IModuleWrapper moduleWrapper, ModuleTask taskInfo, Method handler) {
        this.moduleWrapper = moduleWrapper;
        this.taskInfo = taskInfo;
        this.handler = handler;
    }

    @Override
    public IModule getModule() {
        return this.moduleWrapper.getModule();
    }

    public IModuleWrapper getModuleWrapper() {
        return this.moduleWrapper;
    }

    public ModuleTask getTaskInfo() {
        return this.taskInfo;
    }

    public Method getHandler() {
        return this.handler;
    }
}