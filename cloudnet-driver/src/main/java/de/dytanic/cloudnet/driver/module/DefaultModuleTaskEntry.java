package de.dytanic.cloudnet.driver.module;

import java.lang.reflect.Method;

public final class DefaultModuleTaskEntry implements ModuleTaskEntry {

    private ModuleWrapper moduleWrapper;

    private ModuleTask taskInfo;

    private Method handler;

    public DefaultModuleTaskEntry(ModuleWrapper moduleWrapper, ModuleTask taskInfo, Method handler) {
        this.moduleWrapper = moduleWrapper;
        this.taskInfo = taskInfo;
        this.handler = handler;
    }

    @Override
    public Module getModule() {
        return this.moduleWrapper.getModule();
    }

    public ModuleWrapper getModuleWrapper() {
        return this.moduleWrapper;
    }

    public ModuleTask getTaskInfo() {
        return this.taskInfo;
    }

    public Method getHandler() {
        return this.handler;
    }
}