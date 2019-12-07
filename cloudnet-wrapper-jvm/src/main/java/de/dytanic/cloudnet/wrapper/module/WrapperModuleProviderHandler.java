package de.dytanic.cloudnet.wrapper.module;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.logging.ILogger;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.event.events.module.*;
import de.dytanic.cloudnet.driver.module.ModuleProvider;
import de.dytanic.cloudnet.driver.module.ModuleProviderHandler;
import de.dytanic.cloudnet.driver.module.ModuleWrapper;
import de.dytanic.cloudnet.driver.module.ModuleDependency;

public final class WrapperModuleProviderHandler implements ModuleProviderHandler {

    @Override
    public void handlePreModuleLoad(ModuleWrapper moduleWrapper) {
        this.callEvent(new ModulePreLoadEvent(this.getModuleProvider(), moduleWrapper));
        this.getLogger().info(replaceAll(LanguageManager.getMessage("cloudnet-pre-load-module"), this.getModuleProvider(), moduleWrapper));
    }

    @Override
    public void handlePostModuleLoad(ModuleWrapper moduleWrapper) {
        this.callEvent(new ModulePostLoadEvent(this.getModuleProvider(), moduleWrapper));
        this.getLogger().info(replaceAll(LanguageManager.getMessage("cloudnet-post-load-module"), this.getModuleProvider(), moduleWrapper));
    }

    @Override
    public void handlePreModuleStart(ModuleWrapper moduleWrapper) {
        this.callEvent(new ModulePreStartEvent(this.getModuleProvider(), moduleWrapper));
        this.getLogger().info(replaceAll(LanguageManager.getMessage("cloudnet-pre-start-module"), this.getModuleProvider(), moduleWrapper));
    }

    @Override
    public void handlePostModuleStart(ModuleWrapper moduleWrapper) {
        this.callEvent(new ModulePostStartEvent(this.getModuleProvider(), moduleWrapper));
        this.getLogger().info(replaceAll(LanguageManager.getMessage("cloudnet-post-start-module"), this.getModuleProvider(), moduleWrapper));
    }

    @Override
    public void handlePreModuleStop(ModuleWrapper moduleWrapper) {
        this.callEvent(new ModulePreStopEvent(this.getModuleProvider(), moduleWrapper));
        this.getLogger().info(replaceAll(LanguageManager.getMessage("cloudnet-pre-stop-module"), this.getModuleProvider(), moduleWrapper));
    }

    @Override
    public void handlePostModuleStop(ModuleWrapper moduleWrapper) {
        this.callEvent(new ModulePostStopEvent(this.getModuleProvider(), moduleWrapper));
        this.getLogger().info(replaceAll(LanguageManager.getMessage("cloudnet-post-stop-module"), this.getModuleProvider(), moduleWrapper));
        CloudNetDriver.getInstance().getEventManager().unregisterListeners(moduleWrapper.getClassLoader());
    }

    @Override
    public void handlePreModuleUnload(ModuleWrapper moduleWrapper) {
        this.callEvent(new ModulePreUnloadEvent(this.getModuleProvider(), moduleWrapper));
        this.getLogger().info(replaceAll(LanguageManager.getMessage("cloudnet-pre-unload-module"), this.getModuleProvider(), moduleWrapper));
    }

    @Override
    public void handlePostModuleUnload(ModuleWrapper moduleWrapper) {
        this.callEvent(new ModulePostUnloadEvent(this.getModuleProvider(), moduleWrapper));
        this.getLogger().info(replaceAll(LanguageManager.getMessage("cloudnet-post-unload-module"), this.getModuleProvider(), moduleWrapper));
    }

    @Override
    public void handlePreInstallDependency(ModuleWrapper moduleWrapper, ModuleDependency dependency) {
        this.callEvent(new ModulePreInstallDependencyEvent(this.getModuleProvider(), moduleWrapper, dependency));
        this.getLogger().info(replaceAll(LanguageManager.getMessage("cloudnet-pre-install-dependency-module")
                        .replace("%group%", dependency.getGroup())
                        .replace("%name%", dependency.getName())
                        .replace("%version%", dependency.getVersion())
                , this.getModuleProvider(), moduleWrapper));
    }

    @Override
    public void handlePostInstallDependency(ModuleWrapper moduleWrapper, ModuleDependency dependency) {
        this.callEvent(new ModulePostInstallDependencyEvent(this.getModuleProvider(), moduleWrapper, dependency));
        this.getLogger().info(replaceAll(LanguageManager.getMessage("cloudnet-post-install-dependency-module")
                        .replace("%group%", dependency.getGroup())
                        .replace("%name%", dependency.getName())
                        .replace("%version%", dependency.getVersion())
                , this.getModuleProvider(), moduleWrapper));
    }

    private ILogger getLogger() {
        return CloudNetDriver.getInstance().getLogger();
    }

    private ModuleProvider getModuleProvider() {
        return CloudNetDriver.getInstance().getModuleProvider();
    }

    private void callEvent(Event event) {
        CloudNetDriver.getInstance().getEventManager().callEvent(event);
    }

    private String replaceAll(String text, ModuleProvider moduleProvider, ModuleWrapper moduleWrapper) {
        Validate.checkNotNull(text);
        Validate.checkNotNull(moduleProvider);
        Validate.checkNotNull(moduleWrapper);

        return text.replace("%module_group%", moduleWrapper.getModuleConfiguration().getGroup())
                .replace("%module_name%", moduleWrapper.getModuleConfiguration().getName())
                .replace("%module_version%", moduleWrapper.getModuleConfiguration().getVersion())
                .replace("%module_author%", moduleWrapper.getModuleConfiguration().getAuthor());
    }
}