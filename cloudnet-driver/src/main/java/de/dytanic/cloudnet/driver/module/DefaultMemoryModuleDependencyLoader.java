package de.dytanic.cloudnet.driver.module;

import java.net.URL;
import java.util.Map;

public class DefaultMemoryModuleDependencyLoader implements
  IModuleDependencyLoader {

  @Override
  public URL loadModuleDependencyByUrl(ModuleConfiguration moduleConfiguration,
    ModuleDependency moduleDependency,
    Map<String, String> moduleRepositoriesUrls) throws Exception {
    return new URL(moduleDependency.getUrl());
  }

  @Override
  public URL loadModuleDependencyByRepository(
    ModuleConfiguration moduleConfiguration,
    ModuleDependency moduleDependency,
    Map<String, String> moduleRepositoriesUrls) throws Exception {
    return new URL(
      moduleRepositoriesUrls.get(moduleDependency.getRepo()) +
        moduleDependency.getGroup().replace(".", "/") + "/" +
        moduleDependency.getName() + "/" + moduleDependency.getVersion()
        + "/" +
        moduleDependency.getName() + "-" + moduleDependency.getVersion()
        + ".jar"
    );
  }
}