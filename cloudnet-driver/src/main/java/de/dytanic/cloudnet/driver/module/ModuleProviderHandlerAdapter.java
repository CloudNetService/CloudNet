package de.dytanic.cloudnet.driver.module;

import de.dytanic.cloudnet.driver.module.dependency.ModuleDependency;
import de.dytanic.cloudnet.driver.module.repository.RepositoryModuleInfo;

public class ModuleProviderHandlerAdapter implements IModuleProviderHandler {

    @Override
    public boolean handlePreModuleLoad(IModuleWrapper moduleWrapper) {
        return true;
    }

    @Override
    public void handlePostModuleLoad(IModuleWrapper moduleWrapper) {

    }

    @Override
    public boolean handlePreModuleStart(IModuleWrapper moduleWrapper) {
        return true;
    }

    @Override
    public void handlePostModuleStart(IModuleWrapper moduleWrapper) {

    }

    @Override
    public boolean handlePreModuleStop(IModuleWrapper moduleWrapper) {
        return true;
    }

    @Override
    public void handlePostModuleStop(IModuleWrapper moduleWrapper) {

    }

    @Override
    public void handlePreModuleUnload(IModuleWrapper moduleWrapper) {

    }

    @Override
    public void handlePostModuleUnload(IModuleWrapper moduleWrapper) {

    }

    @Override
    public void handlePreInstallDependency(IModuleWrapper moduleWrapper, ModuleDependency dependency) {

    }

    @Override
    public void handlePostInstallDependency(IModuleWrapper moduleWrapper, ModuleDependency dependency) {

    }

    @Override
    public void handleCheckForUpdates(IModuleWrapper moduleWrapper) {

    }

    @Override
    public void handlePreInstallUpdate(IModuleWrapper moduleWrapper, RepositoryModuleInfo moduleInfo) {

    }

    @Override
    public void handleInstallUpdateFailed(IModuleWrapper moduleWrapper, RepositoryModuleInfo moduleInfo) {

    }

    @Override
    public void handlePostInstallUpdate(IModuleWrapper moduleWrapper, RepositoryModuleInfo moduleInfo) {

    }

}