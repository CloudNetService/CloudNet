package de.dytanic.cloudnet.driver.module;

public interface IModuleProviderHandler {

  void handlePreModuleLoad(IModuleWrapper moduleWrapper);

  void handlePostModuleLoad(IModuleWrapper moduleWrapper);

  void handlePreModuleStart(IModuleWrapper moduleWrapper);

  void handlePostModuleStart(IModuleWrapper moduleWrapper);

  void handlePreModuleStop(IModuleWrapper moduleWrapper);

  void handlePostModuleStop(IModuleWrapper moduleWrapper);

  void handlePreModuleUnload(IModuleWrapper moduleWrapper);

  void handlePostModuleUnload(IModuleWrapper moduleWrapper);

  void handlePreInstallDependency(IModuleWrapper moduleWrapper,
    ModuleDependency dependency);

  void handlePostInstallDependency(IModuleWrapper moduleWrapper,
    ModuleDependency dependency);
}