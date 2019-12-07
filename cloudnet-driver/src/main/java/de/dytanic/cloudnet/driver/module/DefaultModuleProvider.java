package de.dytanic.cloudnet.driver.module;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.collection.Iterables;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

public final class DefaultModuleProvider implements ModuleProvider {

    protected Collection<DefaultModuleWrapper> moduleWrappers = Iterables.newCopyOnWriteArrayList();

    protected ModuleProviderHandler moduleProviderHandler = new ModuleProviderHandlerAdapter();

    protected ModuleDependencyLoader moduleDependencyLoader = new DefaultMemoryModuleDependencyLoader();

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
    public Collection<ModuleWrapper> getModules() {
        return Collections.unmodifiableCollection(moduleWrappers);
    }

    @Override
    public Collection<ModuleWrapper> getModules(String group) {
        Validate.checkNotNull(group);

        return Iterables.filter(this.getModules(), defaultModuleWrapper -> defaultModuleWrapper.getModuleConfiguration().group.equals(group));
    }

    @Override
    public ModuleWrapper getModule(String name) {
        Validate.checkNotNull(name);

        return Iterables.first(this.moduleWrappers, defaultModuleWrapper -> defaultModuleWrapper.getModuleConfiguration().getName().equals(name));
    }

    @Override
    public ModuleWrapper loadModule(URL url) {
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
    public ModuleWrapper loadModule(File file) {
        Validate.checkNotNull(file);

        return loadModule(file.toPath());
    }

    @Override
    public ModuleWrapper loadModule(Path path) {
        Validate.checkNotNull(path);

        try {
            return loadModule(path.toUri().toURL());
        } catch (MalformedURLException exception) {
            exception.printStackTrace();
        }

        return null;
    }

    @Override
    public ModuleProvider loadModule(URL... urls) {
        Validate.checkNotNull(urls);

        for (URL url : urls) {
            loadModule(url);
        }

        return this;
    }

    @Override
    public ModuleProvider loadModule(File... files) {
        Validate.checkNotNull(files);

        for (File file : files) {
            loadModule(file);
        }

        return this;
    }

    @Override
    public ModuleProvider loadModule(Path... paths) {
        Validate.checkNotNull(paths);

        for (Path path : paths) {
            loadModule(path);
        }

        return this;
    }

    @Override
    public ModuleProvider startAll() {
        for (DefaultModuleWrapper moduleWrapper : this.moduleWrappers) {
            moduleWrapper.startModule();
        }

        return this;
    }

    @Override
    public ModuleProvider stopAll() {
        for (DefaultModuleWrapper moduleWrapper : this.moduleWrappers) {
            moduleWrapper.stopModule();
        }

        return this;
    }

    @Override
    public ModuleProvider unloadAll() {
        for (DefaultModuleWrapper moduleWrapper : this.moduleWrappers) {
            moduleWrapper.unloadModule();
        }

        return this;
    }

    public ModuleProviderHandler getModuleProviderHandler() {
        return this.moduleProviderHandler;
    }

    public void setModuleProviderHandler(ModuleProviderHandler moduleProviderHandler) {
        this.moduleProviderHandler = moduleProviderHandler;
    }

    public ModuleDependencyLoader getModuleDependencyLoader() {
        return this.moduleDependencyLoader;
    }

    public void setModuleDependencyLoader(ModuleDependencyLoader moduleDependencyLoader) {
        this.moduleDependencyLoader = moduleDependencyLoader;
    }
}