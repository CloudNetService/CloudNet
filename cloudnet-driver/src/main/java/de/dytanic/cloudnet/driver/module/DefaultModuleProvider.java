package de.dytanic.cloudnet.driver.module;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

public final class DefaultModuleProvider implements IModuleProvider {

    protected Collection<DefaultModuleWrapper> moduleWrappers = Iterables.newCopyOnWriteArrayList();

    protected IModuleProviderHandler moduleProviderHandler = new ModuleProviderHandlerAdapter();

    protected IModuleDependencyLoader moduleDependencyLoader = new DefaultMemoryModuleDependencyLoader();

    private File moduleDirectory = new File("modules");

    @Override
    public File getModuleDirectory() {
        return moduleDirectory;
    }

    @Override
    public void setModuleDirectory(File moduleDirectory) {
        this.moduleDirectory = Validate.checkNotNull(moduleDirectory);
    }

    @Override
    public Collection<IModuleWrapper> getModules() {
        return Collections.unmodifiableCollection(moduleWrappers);
    }

    @Override
    public Collection<IModuleWrapper> getModules(String group) {
        Validate.checkNotNull(group);

        return Iterables.filter(this.getModules(), defaultModuleWrapper -> defaultModuleWrapper.getModuleConfiguration().group.equals(group));
    }

    @Override
    public IModuleWrapper getModule(String name) {
        Validate.checkNotNull(name);

        return Iterables.first(this.moduleWrappers, defaultModuleWrapper -> defaultModuleWrapper.getModuleConfiguration().getName().equals(name));
    }

    @Override
    public IModuleWrapper loadModule(URL url) {
        Validate.checkNotNull(url);

        DefaultModuleWrapper moduleWrapper = null;

        if (Iterables.first(this.moduleWrappers, defaultModuleWrapper -> defaultModuleWrapper.getUrl().toString().equalsIgnoreCase(url.toString())) != null) {
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
        Validate.checkNotNull(file);

        return loadModule(file.toPath());
    }

    @Override
    public IModuleWrapper loadModule(Path path) {
        Validate.checkNotNull(path);

        try {
            return loadModule(path.toUri().toURL());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public IModuleProvider loadModule(URL... urls) {
        Validate.checkNotNull(urls);

        for (URL url : urls) {
            loadModule(url);
        }

        return this;
    }

    @Override
    public IModuleProvider loadModule(File... files) {
        Validate.checkNotNull(files);

        for (File file : files) {
            loadModule(file);
        }

        return this;
    }

    @Override
    public IModuleProvider loadModule(Path... paths) {
        Validate.checkNotNull(paths);

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