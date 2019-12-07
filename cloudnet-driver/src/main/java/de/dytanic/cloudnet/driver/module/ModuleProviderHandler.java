package de.dytanic.cloudnet.driver.module;

public interface ModuleProviderHandler {

    void handlePreModuleLoad(ModuleWrapper moduleWrapper);

    void handlePostModuleLoad(ModuleWrapper moduleWrapper);

    void handlePreModuleStart(ModuleWrapper moduleWrapper);

    void handlePostModuleStart(ModuleWrapper moduleWrapper);

    void handlePreModuleStop(ModuleWrapper moduleWrapper);

    void handlePostModuleStop(ModuleWrapper moduleWrapper);

    void handlePreModuleUnload(ModuleWrapper moduleWrapper);

    void handlePostModuleUnload(ModuleWrapper moduleWrapper);

    void handlePreInstallDependency(ModuleWrapper moduleWrapper, ModuleDependency dependency);

    void handlePostInstallDependency(ModuleWrapper moduleWrapper, ModuleDependency dependency);
}