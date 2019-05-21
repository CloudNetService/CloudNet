package de.dytanic.cloudnet.driver.module;

import java.net.URL;
import java.util.Map;

public interface IModuleDependencyLoader {

    URL loadModuleDependencyByUrl(ModuleConfiguration moduleConfiguration, ModuleDependency moduleDependency, Map<String, String> moduleRepositoriesUrls) throws Exception;

    URL loadModuleDependencyByRepository(ModuleConfiguration moduleConfiguration, ModuleDependency moduleDependency, Map<String, String> moduleRepositoriesUrls) throws Exception;

}