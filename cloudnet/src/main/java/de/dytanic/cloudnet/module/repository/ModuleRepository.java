package de.dytanic.cloudnet.module.repository;

import de.dytanic.cloudnet.driver.module.ModuleId;

import java.util.Collection;

public interface ModuleRepository {

    /**
     * Checks whether the module repository is online or not
     *
     * @return {@code true} if the modules can be loaded or {@code false} otherwise
     */
    boolean isReachable();

    String getBaseURL();

    Collection<RepositoryModuleInfo> loadAvailableModules();

    RepositoryModuleInfo loadRepositoryModuleInfo(ModuleId moduleId);

}
