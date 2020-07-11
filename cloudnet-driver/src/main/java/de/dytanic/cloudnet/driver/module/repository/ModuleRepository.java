package de.dytanic.cloudnet.driver.module.repository;

import de.dytanic.cloudnet.driver.module.ModuleId;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface ModuleRepository {

    /**
     * Checks whether the module repository is online or not
     *
     * @return {@code true} if the modules can be loaded or {@code false} otherwise
     */
    boolean isReachable();

    @NotNull
    String getBaseURL();

    @NotNull
    String getModuleURL(@NotNull ModuleId moduleId);

    @NotNull
    Collection<RepositoryModuleInfo> loadAvailableModules();

    @NotNull
    Collection<RepositoryModuleInfo> getAvailableModules();

    RepositoryModuleInfo loadRepositoryModuleInfo(@NotNull ModuleId moduleId);

    RepositoryModuleInfo getRepositoryModuleInfo(@NotNull ModuleId moduleId);

}
