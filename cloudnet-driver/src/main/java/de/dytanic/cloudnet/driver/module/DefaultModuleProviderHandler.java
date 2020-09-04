package de.dytanic.cloudnet.driver.module;

import com.google.common.base.Preconditions;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.logging.ILogger;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.event.Event;
import de.dytanic.cloudnet.driver.event.events.module.*;

public class DefaultModuleProviderHandler implements IModuleProviderHandler {

    @Override
    public boolean handlePreModuleLoad(IModuleWrapper moduleWrapper) {
        boolean cancelled = this.callEvent(new ModulePreLoadEvent(this.getModuleProvider(), moduleWrapper)).isCancelled();
        if (!cancelled) {
            this.getLogger().info(this.replaceAll(LanguageManager.getMessage("cloudnet-pre-load-module"), this.getModuleProvider(), moduleWrapper));
        }

        return !cancelled;
    }

    @Override
    public void handlePostModuleLoad(IModuleWrapper moduleWrapper) {
        this.callEvent(new ModulePostLoadEvent(this.getModuleProvider(), moduleWrapper));
        this.getLogger().extended(this.replaceAll(LanguageManager.getMessage("cloudnet-post-load-module"), this.getModuleProvider(), moduleWrapper));
    }

    @Override
    public boolean handlePreModuleStart(IModuleWrapper moduleWrapper) {
        boolean cancelled = this.callEvent(new ModulePreStartEvent(this.getModuleProvider(), moduleWrapper)).isCancelled();
        if (!cancelled) {
            this.getLogger().info(this.replaceAll(LanguageManager.getMessage("cloudnet-pre-start-module"), this.getModuleProvider(), moduleWrapper));
            CloudNetDriver.getInstance().getEventManager().registerListener(moduleWrapper.getModule());
        }

        return !cancelled;
    }

    @Override
    public void handlePostModuleStart(IModuleWrapper moduleWrapper) {
        this.callEvent(new ModulePostStartEvent(this.getModuleProvider(), moduleWrapper));
        this.getLogger().extended(this.replaceAll(LanguageManager.getMessage("cloudnet-post-start-module"), this.getModuleProvider(), moduleWrapper));
    }

    @Override
    public boolean handlePreModuleStop(IModuleWrapper moduleWrapper) {
        boolean cancelled = this.callEvent(new ModulePreStopEvent(this.getModuleProvider(), moduleWrapper)).isCancelled();
        if (!cancelled) {
            this.getLogger().info(this.replaceAll(LanguageManager.getMessage("cloudnet-pre-stop-module"), this.getModuleProvider(), moduleWrapper));
        }

        return !cancelled;
    }

    @Override
    public void handlePostModuleStop(IModuleWrapper moduleWrapper) {
        CloudNetDriver.getInstance().getServicesRegistry().unregisterAll(moduleWrapper.getClassLoader());
        CloudNetDriver.getInstance().getEventManager().unregisterListeners(moduleWrapper.getClassLoader());

        this.callEvent(new ModulePostStopEvent(this.getModuleProvider(), moduleWrapper));
        this.getLogger().extended(this.replaceAll(LanguageManager.getMessage("cloudnet-post-stop-module"), this.getModuleProvider(), moduleWrapper));
    }

    @Override
    public void handlePreModuleUnload(IModuleWrapper moduleWrapper) {
        this.callEvent(new ModulePreUnloadEvent(this.getModuleProvider(), moduleWrapper));
        this.getLogger().info(this.replaceAll(LanguageManager.getMessage("cloudnet-pre-unload-module"), this.getModuleProvider(), moduleWrapper));
    }

    @Override
    public void handlePostModuleUnload(IModuleWrapper moduleWrapper) {
        this.callEvent(new ModulePostUnloadEvent(this.getModuleProvider(), moduleWrapper));
        this.getLogger().extended(this.replaceAll(LanguageManager.getMessage("cloudnet-post-unload-module"), this.getModuleProvider(), moduleWrapper));
    }

    @Override
    public void handlePreInstallDependency(IModuleWrapper moduleWrapper, ModuleDependency dependency) {
        this.callEvent(new ModulePreInstallDependencyEvent(this.getModuleProvider(), moduleWrapper, dependency));
        this.getLogger().extended(this.replaceAll(LanguageManager.getMessage("cloudnet-pre-install-dependency-module")
                        .replace("%group%", dependency.getGroup())
                        .replace("%name%", dependency.getName())
                        .replace("%version%", dependency.getVersion())
                , this.getModuleProvider(), moduleWrapper));
    }

    @Override
    public void handlePostInstallDependency(IModuleWrapper moduleWrapper, ModuleDependency dependency) {
        this.callEvent(new ModulePostInstallDependencyEvent(this.getModuleProvider(), moduleWrapper, dependency));
        this.getLogger().extended(this.replaceAll(LanguageManager.getMessage("cloudnet-post-install-dependency-module")
                        .replace("%group%", dependency.getGroup())
                        .replace("%name%", dependency.getName())
                        .replace("%version%", dependency.getVersion())
                , this.getModuleProvider(), moduleWrapper));
    }

    protected ILogger getLogger() {
        return CloudNetDriver.getInstance().getLogger();
    }

    protected IModuleProvider getModuleProvider() {
        return CloudNetDriver.getInstance().getModuleProvider();
    }

    protected <T extends Event> T callEvent(T event) {
        return CloudNetDriver.getInstance().getEventManager().callEvent(event);
    }


    protected String replaceAll(String text, IModuleProvider moduleProvider, IModuleWrapper moduleWrapper) {
        Preconditions.checkNotNull(text);
        Preconditions.checkNotNull(moduleProvider);
        Preconditions.checkNotNull(moduleWrapper);

        return text.replace("%module_group%", moduleWrapper.getModuleConfiguration().getGroup())
                .replace("%module_name%", moduleWrapper.getModuleConfiguration().getName())
                .replace("%module_version%", moduleWrapper.getModuleConfiguration().getVersion())
                .replace("%module_author%", moduleWrapper.getModuleConfiguration().getAuthor());
    }

}
