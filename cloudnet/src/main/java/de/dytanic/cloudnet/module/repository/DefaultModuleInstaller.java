package de.dytanic.cloudnet.module.repository;

import de.dytanic.cloudnet.console.IConsole;
import de.dytanic.cloudnet.driver.module.IModuleProvider;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import de.dytanic.cloudnet.driver.module.ModuleId;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;

public class DefaultModuleInstaller implements ModuleInstaller {

    private IModuleProvider moduleProvider;

    public DefaultModuleInstaller(IModuleProvider moduleProvider) {
        this.moduleProvider = moduleProvider;
    }

    @Override
    public IModuleProvider getModuleProvider() {
        return this.moduleProvider;
    }

    @Override
    public void installModule(IConsole console, String baseURL, RepositoryModuleInfo moduleInfo) throws IOException {
        Path modulePath = this.getModulePath(moduleInfo.getModuleId());

        Files.createDirectories(this.moduleProvider.getModuleDirectory().toPath());

        URL url = new URL(baseURL + String.format("%s/modules/file/%s/%s", RemoteModuleRepository.VERSION_PARENT, moduleInfo.getModuleId().getGroup(), moduleInfo.getModuleId().getName()));
        URLConnection connection = url.openConnection();

        try (InputStream inputStream = connection.getInputStream()) {
            Files.copy(inputStream, modulePath);
        }

        IModuleWrapper moduleWrapper = null;
        try {
            moduleWrapper = this.moduleProvider.loadModule(modulePath);
            if (moduleWrapper != null) {
                moduleWrapper.startModule();
            } else {
                Files.delete(modulePath);
            }
        } catch (Throwable throwable) {
            if (moduleWrapper != null) {
                moduleWrapper.stopModule();
            }
            Files.delete(modulePath);
            throw throwable;
        }
    }

    @Override
    public void uninstallModule(ModuleId moduleId) throws IOException {
        Path modulePath = this.getModulePath(moduleId);

        URL moduleURL = modulePath.toUri().toURL();

        this.moduleProvider.getModules().stream()
                .filter(moduleWrapper -> moduleWrapper.getUrl().toString().equals(moduleURL.toString()))
                .findFirst()
                .ifPresent(IModuleWrapper::unloadModule);

        Files.deleteIfExists(modulePath);
    }

    @Override
    public boolean isModuleInstalled(ModuleId moduleId) {
        return Files.exists(this.getModulePath(moduleId));
    }

    @Override
    public Collection<RepositoryModuleInfo> getInstalledModules(Collection<RepositoryModuleInfo> sourceModuleInfos) {
        return sourceModuleInfos.stream()
                .filter(moduleInfo -> this.isModuleInstalled(moduleInfo.getModuleId()))
                .collect(Collectors.toList());
    }

    private Path getModulePath(ModuleId moduleId) {
        return this.moduleProvider.getModuleDirectory().toPath()
                .resolve(moduleId.getGroup() + "." + moduleId.getName() + ".jar");
    }

}
