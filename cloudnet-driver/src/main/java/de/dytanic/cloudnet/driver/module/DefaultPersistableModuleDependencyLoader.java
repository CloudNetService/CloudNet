package de.dytanic.cloudnet.driver.module;

import de.dytanic.cloudnet.common.io.FileUtils;
import lombok.Getter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

@Getter
public class DefaultPersistableModuleDependencyLoader implements IModuleDependencyLoader {

    protected File baseDirectory;

    public DefaultPersistableModuleDependencyLoader(File baseDirectory)
    {
        this.baseDirectory = baseDirectory;
        this.baseDirectory.mkdirs();
    }

    @Override
    public URL loadModuleDependencyByUrl(ModuleConfiguration moduleConfiguration, ModuleDependency moduleDependency, Map<String, String> moduleRepositoriesUrls) throws Exception
    {
        return loadModuleDependency0(moduleDependency, moduleDependency.getUrl());
    }

    @Override
    public URL loadModuleDependencyByRepository(ModuleConfiguration moduleConfiguration, ModuleDependency moduleDependency, Map<String, String> moduleRepositoriesUrls) throws Exception
    {
        return loadModuleDependency0(moduleDependency, moduleRepositoriesUrls.get(moduleDependency.getRepo()) +
            moduleDependency.getGroup().replace(".", "/") + "/" +
            moduleDependency.getName() + "/" + moduleDependency.getVersion() + "/" +
            moduleDependency.getName() + "-" + moduleDependency.getVersion() + ".jar");
    }

    private URL loadModuleDependency0(ModuleDependency moduleDependency, String url) throws Exception
    {
        File destFile = new File(this.baseDirectory, moduleDependency.getGroup().replace(".", "/") + "/" + moduleDependency.getName() +
            "/" + moduleDependency.getVersion() + "/" + moduleDependency.getName() + "-" + moduleDependency.getVersion() + ".jar");

        if (!destFile.exists())
        {
            destFile.getParentFile().mkdirs();

            URLConnection urlConnection = new URL(url).openConnection();

            urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
            urlConnection.setDoOutput(false);
            urlConnection.setUseCaches(false);
            urlConnection.connect();

            destFile.createNewFile();
            try (InputStream inputStream = urlConnection.getInputStream();
                 FileOutputStream fileOutputStream = new FileOutputStream(destFile))
            {
                FileUtils.copy(inputStream, fileOutputStream);
            }
        }

        return destFile.toURI().toURL();
    }
}