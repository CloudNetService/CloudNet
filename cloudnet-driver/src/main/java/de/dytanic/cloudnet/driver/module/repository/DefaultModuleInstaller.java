package de.dytanic.cloudnet.driver.module.repository;

import de.dytanic.cloudnet.common.concurrent.ThrowableFunction;
import de.dytanic.cloudnet.driver.module.IModuleProvider;
import de.dytanic.cloudnet.driver.module.IModuleWrapper;
import de.dytanic.cloudnet.driver.module.ModuleId;
import de.dytanic.cloudnet.driver.module.ModuleLifeCycle;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

public class DefaultModuleInstaller implements ModuleInstaller {

    private final IModuleProvider moduleProvider;
    private final ThrowableFunction<URL, InputStream, IOException> inputStreamProvider;
    private final String baseURL;

    public DefaultModuleInstaller(IModuleProvider moduleProvider, ThrowableFunction<URL, InputStream, IOException> inputStreamProvider, String baseURL) {
        this.moduleProvider = moduleProvider;
        this.inputStreamProvider = inputStreamProvider != null ? inputStreamProvider : URL::openStream;
        this.baseURL = baseURL;
    }

    @Override
    public @NotNull IModuleProvider getModuleProvider() {
        return this.moduleProvider;
    }

    @Override
    public boolean installModule(@NotNull RepositoryModuleInfo moduleInfo, OutputStream outputStream) throws IOException {
        URL url = new URL(this.baseURL + String.format("/%s/modules/file/%s/%s", RemoteModuleRepository.VERSION_PARENT, moduleInfo.getModuleId().getGroup(), moduleInfo.getModuleId().getName()));
        InputStream inputStream = this.inputStreamProvider.apply(url);

        byte[] buffer = new byte[8192];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, len);
        }

        inputStream.close();

        return true;
    }

    @Override
    public boolean installModule(@NotNull RepositoryModuleInfo moduleInfo, boolean load) throws IOException {
        Path modulePath = this.moduleProvider.getModuleDirectory().toPath()
                .resolve(moduleInfo.getModuleId().getGroup() + "." + moduleInfo.getModuleId().getName() + ".jar");

        Files.createDirectories(this.moduleProvider.getModuleDirectory().toPath());

        try (OutputStream outputStream = Files.newOutputStream(modulePath)) {
            if (!this.installModule(moduleInfo, outputStream)) {
                return false;
            }
        }

        if (!load) {
            return true;
        }

        IModuleWrapper moduleWrapper = this.moduleProvider.loadModule(modulePath);
        if (moduleWrapper != null) {
            moduleWrapper.startModule();

            return moduleWrapper.getModuleLifeCycle() == ModuleLifeCycle.STARTED;
        } else {
            Files.delete(modulePath);

            return false;
        }
    }

    @Override
    public boolean uninstallModule(@NotNull ModuleId moduleId) throws IOException {
        Optional<IModuleWrapper> optional = this.moduleProvider.getModules().stream()
                .filter(wrapper -> wrapper.getModuleConfiguration().getModuleId().equalsIgnoreVersion(moduleId))
                .findFirst();

        if (optional.isPresent()) {
            optional.get().unloadModule();
            try {
                URI uri = optional.get().getUrl().toURI();

                if (uri.getScheme().equals("file")) {
                    Path path = Paths.get(uri);
                    Files.deleteIfExists(path);
                }

            } catch (URISyntaxException exception) {
                exception.printStackTrace();
            }
        }

        return optional.isPresent();
    }

    @Override
    public boolean isModuleInstalled(@NotNull ModuleId moduleId) {
        return this.moduleProvider.getModules().stream()
                .anyMatch(wrapper -> wrapper.getModuleConfiguration().getModuleId().equalsIgnoreVersion(moduleId));
    }

    @Override
    public Collection<RepositoryModuleInfo> getInstalledModules(@NotNull Collection<RepositoryModuleInfo> sourceModuleInfos) {
        return sourceModuleInfos.stream()
                .filter(moduleInfo -> this.isModuleInstalled(moduleInfo.getModuleId()))
                .collect(Collectors.toList());
    }

}
