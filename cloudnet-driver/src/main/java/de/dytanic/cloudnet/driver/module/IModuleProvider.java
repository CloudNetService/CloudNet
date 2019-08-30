package de.dytanic.cloudnet.driver.module;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;

public interface IModuleProvider {

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