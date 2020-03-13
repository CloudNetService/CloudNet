package de.dytanic.cloudnet.driver.module;

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
}