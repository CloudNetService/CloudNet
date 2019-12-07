package de.dytanic.cloudnet.driver.module;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;

public interface ModuleProvider {

    File getModuleDirectory();

    void setModuleDirectory(File moduleDirectory);

    ModuleProviderHandler getModuleProviderHandler();

    void setModuleProviderHandler(ModuleProviderHandler moduleProviderHandler);

    ModuleDependencyLoader getModuleDependencyLoader();

    void setModuleDependencyLoader(ModuleDependencyLoader moduleDependencyLoader);

    Collection<ModuleWrapper> getModules();

    Collection<ModuleWrapper> getModules(String group);

    ModuleWrapper getModule(String name);

    ModuleWrapper loadModule(URL url);

    ModuleWrapper loadModule(File file);

    ModuleWrapper loadModule(Path path);

    ModuleProvider loadModule(URL... urls);

    ModuleProvider loadModule(File... files);

    ModuleProvider loadModule(Path... paths);

    ModuleProvider startAll();

    ModuleProvider stopAll();

    ModuleProvider unloadAll();

}