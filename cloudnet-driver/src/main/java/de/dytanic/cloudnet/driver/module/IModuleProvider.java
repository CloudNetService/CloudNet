package de.dytanic.cloudnet.driver.module;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;

public interface IModuleProvider {

  void setModuleProviderHandler(IModuleProviderHandler moduleProviderHandler);

  IModuleProviderHandler getModuleProviderHandler();

  void setModuleDependencyLoader(
      IModuleDependencyLoader moduleDependencyLoader);

  IModuleDependencyLoader getModuleDependencyLoader();

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