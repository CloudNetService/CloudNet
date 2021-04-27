package de.dytanic.cloudnet.driver.module;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class DefaultPersistableModuleDependencyLoader implements IModuleDependencyLoader {

    protected final Path baseDirectory;

    public DefaultPersistableModuleDependencyLoader(Path baseDirectory) {
        this.baseDirectory = baseDirectory;
        try {
            Files.createDirectories(baseDirectory);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public URL loadModuleDependencyByUrl(ModuleConfiguration moduleConfiguration, ModuleDependency moduleDependency, Map<String, String> moduleRepositoriesUrls) throws Exception {
        return this.loadModuleDependency0(moduleDependency, moduleDependency.getUrl());
    }

    @Override
    public URL loadModuleDependencyByRepository(ModuleConfiguration moduleConfiguration, ModuleDependency moduleDependency, Map<String, String> moduleRepositoriesUrls) throws Exception {
        return this.loadModuleDependency0(moduleDependency, moduleRepositoriesUrls.get(moduleDependency.getRepo()) +
                moduleDependency.getGroup().replace(".", "/") + "/" +
                moduleDependency.getName() + "/" + moduleDependency.getVersion() + "/" +
                moduleDependency.getName() + "-" + moduleDependency.getVersion() + ".jar");
    }

    private URL loadModuleDependency0(ModuleDependency moduleDependency, String url) throws Exception {
        Path destFile = this.baseDirectory.resolve(moduleDependency.getGroup().replace(".", "/") + "/" + moduleDependency.getName() +
                "/" + moduleDependency.getVersion() + "/" + moduleDependency.getName() + "-" + moduleDependency.getVersion() + ".jar");

        if (!Files.exists(destFile)) {
            Files.createDirectories(destFile.getParent());

            URLConnection urlConnection = new URL(url).openConnection();

            urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
            urlConnection.setDoOutput(false);
            urlConnection.setUseCaches(false);
            urlConnection.connect();

            try (InputStream inputStream = urlConnection.getInputStream()) {
                Files.copy(inputStream, destFile);
            }
        }

        return destFile.toUri().toURL();
    }

    public Path getBaseDirectory() {
        return this.baseDirectory;
    }
}