package de.dytanic.cloudnet.module.repository;

import de.dytanic.cloudnet.console.IConsole;
import de.dytanic.cloudnet.driver.module.IModuleProvider;
import de.dytanic.cloudnet.driver.module.ModuleId;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;

public interface ModuleInstaller {

    IModuleProvider getModuleProvider();

    void installModule(IConsole console, RepositoryModuleInfo moduleInfo) throws IOException;

    void uninstallModule(ModuleId moduleId) throws IOException;

    boolean isModuleInstalled(ModuleId moduleId);

    Collection<RepositoryModuleInfo> getInstalledModules(Collection<RepositoryModuleInfo> sourceModuleInfos);

}
