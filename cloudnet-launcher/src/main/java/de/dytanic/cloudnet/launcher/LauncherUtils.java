/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.launcher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.dytanic.cloudnet.launcher.module.CloudNetModule;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class LauncherUtils {

  public static final Gson GSON = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").create();

  public static final List<CloudNetModule> DEFAULT_MODULES = Arrays.asList(
    new CloudNetModule("cloudnet-bridge", "cloudnet-bridge.jar"),
    new CloudNetModule("cloudnet-signs", "cloudnet-signs.jar"),
    new CloudNetModule("cloudnet-npcs", "cloudnet-npcs.jar"),
    new CloudNetModule("cloudnet-syncproxy", "cloudnet-syncproxy.jar"),
    new CloudNetModule("cloudnet-cloudflare", "cloudnet-cloudflare.jar"),
    new CloudNetModule("cloudnet-report", "cloudnet-report.jar"),
    new CloudNetModule("cloudnet-rest", "cloudnet-rest.jar"),
    new CloudNetModule("cloudnet-smart", "cloudnet-smart.jar"),
    new CloudNetModule("cloudnet-cloudperms", "cloudnet-cloudperms.jar"),
    new CloudNetModule("cloudnet-storage-ftp", "cloudnet-storage-ftp.jar"),
    new CloudNetModule("cloudnet-database-mysql", "cloudnet-database-mysql.jar"),
    new CloudNetModule("cloudnet-labymod", "cloudnet-labymod.jar")
  );

  public static final String[] VERSION_FILE_NAMES = new String[]{
    "cloudnet.jar",
    "cloudnet.cnl",
    "driver.jar",
    "driver.cnl"
  };

  public static final String CLOUDNET_REPOSITORY = "cloudnet.repository";
  public static final String LAUNCHER_DEV_MODE = "cloudnet.launcher.dev-mode";
  public static final String CLOUDNET_SNAPSHOTS = "cloudnet.snapshots.use-snapshots";
  public static final String CLOUDNET_REPOSITORY_AUTO_UPDATE = "cloudnet.auto-update";
  public static final String CLOUDNET_SNAPSHOTS_JOB_URL = "cloudnet.snapshots.job.url";
  public static final String CLOUDNET_REPOSITORY_GITHUB = "cloudnet.repository.github";
  public static final String CLOUDNET_SELECTED_VERSION = "cloudnet.launcher.select.version";
  public static final String FALLBACK_VERSION = LauncherUtils.class.getPackage().getSpecificationVersion();

  private LauncherUtils() {
    throw new UnsupportedOperationException();
  }

  public static InputStream readFromURL(String url) throws IOException {
    URLConnection urlConnection = new URL(url).openConnection();

    urlConnection.setUseCaches(false);
    urlConnection.setDoOutput(false);

    urlConnection.setConnectTimeout(5000);
    urlConnection.setReadTimeout(5000);

    urlConnection.setRequestProperty("User-Agent",
      "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
    urlConnection.connect();

    return urlConnection.getInputStream();
  }

  public static void deleteFile(Path path) throws IOException {
    Files.walk(path)
      .sorted(Comparator.reverseOrder())
      .forEach(file -> {
        try {
          Files.delete(file);
        } catch (IOException exception) {
          exception.printStackTrace();
        }
      });
  }

}
