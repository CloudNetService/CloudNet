package de.dytanic.cloudnet.driver.module;

import de.dytanic.cloudnet.driver.module.dependency.IModuleDependencyLoader;
import de.dytanic.cloudnet.driver.module.repository.ModuleInstaller;
import de.dytanic.cloudnet.driver.module.repository.ModuleRepository;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;

public interface IModuleProvider {

    ModuleRepository getModuleRepository();

    ModuleInstaller getModuleInstaller();

    boolean isAutoUpdateEnabled();

    void setAutoUpdateEnabled(boolean autoUpdateEnabled);

    File getModuleDirectory();

    void setModuleDirectory(File moduleDirectory);

    IModuleProviderHandler getModuleProviderHandler();

    void setModuleProviderHandler(IModuleProviderHandler moduleProviderHandler);

    IModuleDependencyLoader getModuleDependencyLoader();

    void setModuleDependencyLoader(IModuleDependencyLoader moduleDependencyLoader);

    Collection<IModuleWrapper> getModules();

    Collection<IModuleWrapper> getModules(String group);

    IModuleWrapper getModule(String name);

    IModuleWrapper loadModule(URL url);

    IModuleWrapper loadModule(File file);

    IModuleWrapper loadModule(Path path);

    IModuleProvider loadModule(URL... urls);

    IModuleProvider loadModule(File... files);

    IModuleProvider loadModule(Path... paths);

    IModuleProvider startAll();

    IModuleProvider stopAll();

    IModuleProvider unloadAll();

}