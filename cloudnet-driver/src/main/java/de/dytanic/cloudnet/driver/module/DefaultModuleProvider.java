package de.dytanic.cloudnet.driver.module;

import com.google.common.base.Preconditions;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public final class DefaultModuleProvider implements IModuleProvider {

    protected Collection<DefaultModuleWrapper> moduleWrappers = new CopyOnWriteArrayList<>();

    protected IModuleProviderHandler moduleProviderHandler = new ModuleProviderHandlerAdapter();

    protected IModuleDependencyLoader moduleDependencyLoader = new DefaultMemoryModuleDependencyLoader();

    private File moduleDirectory = new File("modules");

    @Override
    public File getModuleDirectory() {
        return moduleDirectory;
    }

    @Override
    public void setModuleDirectory(File moduleDirectory) {
        this.moduleDirectory = Preconditions.checkNotNull(moduleDirectory);
    }

    @Override
    public Collection<IModuleWrapper> getModules() {
        return Collections.unmodifiableCollection(moduleWrappers);
    }

    @Override
    public Collection<IModuleWrapper> getModules(String group) {
        Preconditions.checkNotNull(group);

        return this.getModules().stream().filter(defaultModuleWrapper -> defaultModuleWrapper.getModuleConfiguration().group.equals(group)).collect(Collectors.toList());
    }

    @Override
    public IModuleWrapper getModule(String name) {
        Preconditions.checkNotNull(name);

        return this.moduleWrappers.stream().filter(defaultModuleWrapper -> defaultModuleWrapper.getModuleConfiguration().getName().equals(name)).findFirst().orElse(null);
    }

    @Override
    public IModuleWrapper loadModule(URL url) {
        Preconditions.checkNotNull(url);

        DefaultModuleWrapper moduleWrapper = null;

        if (this.moduleWrappers.stream().anyMatch(defaultModuleWrapper -> defaultModuleWrapper.getUrl().toString().equalsIgnoreCase(url.toString()))) {
            return null;
        }

        try {

            this.moduleWrappers.add(moduleWrapper = new DefaultModuleWrapper(this, url, this.moduleDirectory));
            moduleWrapper.loadModule();

        } catch (Throwable throwable) {
            throwable.printStackTrace();

            if (moduleWrapper != null) {
                moduleWrapper.unloadModule();
            }
        }

        return moduleWrapper;
    }

    @Override
    public IModuleWrapper loadModule(File file) {
        Preconditions.checkNotNull(file);

        return loadModule(file.toPath());
    }

    @Override
    public IModuleWrapper loadModule(Path path) {
        Preconditions.checkNotNull(path);

        try {
            return loadModule(path.toUri().toURL());
        } catch (MalformedURLException exception) {
            exception.printStackTrace();
        }

        return null;
    }

    @Override
    public IModuleProvider loadModule(URL... urls) {
        Preconditions.checkNotNull(urls);

        for (URL url : urls) {
            loadModule(url);
        }

        return this;
    }

    @Override
    public IModuleProvider loadModule(File... files) {
        Preconditions.checkNotNull(files);

        for (File file : files) {
            loadModule(file);
        }

        return this;
    }

    @Override
    public IModuleProvider loadModule(Path... paths) {
        Preconditions.checkNotNull(paths);

        for (Path path : paths) {
            loadModule(path);
        }

        return this;
    }

    @Override
    public IModuleProvider startAll() {
        for (DefaultModuleWrapper moduleWrapper : this.moduleWrappers) {
            moduleWrapper.startModule();
        }

        return this;
    }

    @Override
    public IModuleProvider stopAll() {
        for (DefaultModuleWrapper moduleWrapper : this.moduleWrappers) {
            moduleWrapper.stopModule();
        }

        return this;
    }

    @Override
    public IModuleProvider unloadAll() {
        for (DefaultModuleWrapper moduleWrapper : this.moduleWrappers) {
            moduleWrapper.unloadModule();
        }

        return this;
    }

    public IModuleProviderHandler getModuleProviderHandler() {
        return this.moduleProviderHandler;
    }

    public void setModuleProviderHandler(IModuleProviderHandler moduleProviderHandler) {
        this.moduleProviderHandler = moduleProviderHandler;
    }

    public IModuleDependencyLoader getModuleDependencyLoader() {
        return this.moduleDependencyLoader;
    }

    public void setModuleDependencyLoader(IModuleDependencyLoader moduleDependencyLoader) {
        this.moduleDependencyLoader = moduleDependencyLoader;
    }
}