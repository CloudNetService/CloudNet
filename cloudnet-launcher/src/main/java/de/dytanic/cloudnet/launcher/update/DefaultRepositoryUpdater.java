package de.dytanic.cloudnet.launcher.update;

import de.dytanic.cloudnet.launcher.Constants;
import de.dytanic.cloudnet.launcher.util.CloudNetModule;
import de.dytanic.cloudnet.launcher.util.IOUtils;
import lombok.Getter;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;

@Getter
public final class DefaultRepositoryUpdater implements IUpdater {

    private String url;

    private Properties properties;

    @Override
    public boolean init(String url)
    {
        this.url = url = url.endsWith("/") ? url : url + "/";
        properties = new Properties();

        try
        {
            URLConnection urlConnection = new URL(url + "repository").openConnection();
            initHttpUrlConnection(urlConnection);

            urlConnection.connect();

            try (InputStream inputStream = urlConnection.getInputStream())
            {
                properties.load(inputStream);
            }
            return true;

        } catch (Exception ex)
        {
            ex.printStackTrace();
        }

        return false;
    }

    @Override
    public String getRepositoryVersion()
    {
        return properties.getProperty("repository-version");
    }

    @Override
    public String getCurrentVersion()
    {
        return properties.getProperty("app-version");
    }

    @Override
    public boolean installUpdate(String destinationBaseDirectory, String moduleDestinationBaseDirectory)
    {
        String version = getCurrentVersion();
        byte[] buffer = new byte[16384];
        boolean successful = true;

        if (version != null)
        {

            if (!installFile(version, "cloudnet.jar", new File(destinationBaseDirectory + "/" + version, "cloudnet.jar"), buffer))
                successful = false;

            if (!installFile(version, "cloudnet.cnl", new File(destinationBaseDirectory + "/" + version, "cloudnet.cnl"), buffer))
                successful = false;

            if (!installFile(version, "driver.jar", new File(destinationBaseDirectory + "/" + version, "driver.jar"), buffer))
                successful = false;

            if (!installFile(version, "driver.cnl", new File(destinationBaseDirectory + "/" + version, "driver.cnl"), buffer))
                successful = false;

            //return successful;
        }

        if (version != null && moduleDestinationBaseDirectory != null)
        {
            for (CloudNetModule module : Constants.DEFAULT_MODULES)
                if (!installModuleFile(version, module.getFileName(), new File(moduleDestinationBaseDirectory, module.getFileName()), buffer))
                    successful = false;
        }

        return successful;
    }

    /*= ------------------------------------------------ =*/

    private boolean installModuleFile(String version, String name, File file, byte[] buffer)
    {
        System.out.println("Installing remote version module " + name + " in version " + version);

        try
        {
            URLConnection urlConnection = new URL(url + "versions/" + version + "/" + name).openConnection();
            initHttpUrlConnection(urlConnection);

            urlConnection.connect();

            file.getParentFile().mkdirs();
            file.delete();
            file.createNewFile();

            try (InputStream inputStream = urlConnection.getInputStream())
            {
                IOUtils.copy(buffer, inputStream, file.toPath());
            }

            return true;

        } catch (Exception ignored)
        {
        }

        return false;
    }

    private boolean installFile(String version, String name, File file, byte[] buffer)
    {
        if (file.exists()) return true;

        try
        {
            URLConnection urlConnection = new URL(url + "versions/" + version + "/" + name).openConnection();
            initHttpUrlConnection(urlConnection);

            urlConnection.connect();

            file.getParentFile().mkdirs();

            try (InputStream inputStream = urlConnection.getInputStream())
            {
                IOUtils.copy(buffer, inputStream, file.toPath());
            }

            return true;

        } catch (Exception ex)
        {
            ex.printStackTrace();
        }

        return false;
    }

    private void initHttpUrlConnection(URLConnection urlConnection)
    {
        urlConnection.setUseCaches(false);
        urlConnection.setDoOutput(false);

        urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
    }
}