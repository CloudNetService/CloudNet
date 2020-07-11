package de.dytanic.cloudnet.driver.module.repository;

import de.dytanic.cloudnet.driver.module.IModuleProvider;
import de.dytanic.cloudnet.driver.module.ModuleId;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

public interface ModuleInstaller {

    @NotNull
    IModuleProvider getModuleProvider();

    boolean installModule(@NotNull RepositoryModuleInfo moduleInfo, OutputStream outputStream) throws IOException;

    boolean installModule(@NotNull RepositoryModuleInfo moduleInfo, boolean load) throws IOException;

    boolean uninstallModule(@NotNull ModuleId moduleId) throws IOException;

    boolean isModuleInstalled(@NotNull ModuleId moduleId);

    Collection<RepositoryModuleInfo> getInstalledModules(@NotNull Collection<RepositoryModuleInfo> sourceModuleInfos);

}
