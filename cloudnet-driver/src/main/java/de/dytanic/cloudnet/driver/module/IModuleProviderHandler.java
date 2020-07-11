package de.dytanic.cloudnet.driver.module;

import de.dytanic.cloudnet.driver.module.dependency.ModuleDependency;
import de.dytanic.cloudnet.driver.module.repository.RepositoryModuleInfo;

public interface IModuleProviderHandler {

    boolean handlePreModuleLoad(IModuleWrapper moduleWrapper);

    void handlePostModuleLoad(IModuleWrapper moduleWrapper);

    boolean handlePreModuleStart(IModuleWrapper moduleWrapper);

    void handlePostModuleStart(IModuleWrapper moduleWrapper);

    boolean handlePreModuleStop(IModuleWrapper moduleWrapper);

    void handlePostModuleStop(IModuleWrapper moduleWrapper);

    void handlePreModuleUnload(IModuleWrapper moduleWrapper);

    void handlePostModuleUnload(IModuleWrapper moduleWrapper);

    void handlePreInstallDependency(IModuleWrapper moduleWrapper, ModuleDependency dependency);

    void handlePostInstallDependency(IModuleWrapper moduleWrapper, ModuleDependency dependency);

    void handleCheckForUpdates(IModuleWrapper moduleWrapper);

    void handlePreInstallUpdate(IModuleWrapper moduleWrapper, RepositoryModuleInfo moduleInfo);

    void handleInstallUpdateFailed(IModuleWrapper moduleWrapper, RepositoryModuleInfo moduleInfo);

    void handlePostInstallUpdate(IModuleWrapper moduleWrapper, RepositoryModuleInfo moduleInfo);

}