/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.launcher.java17.updater;

import eu.cloudnetservice.ext.updater.defaults.DefaultUpdaterRegistry;
import eu.cloudnetservice.ext.updater.util.GitHubUtil;
import eu.cloudnetservice.launcher.java17.CloudNetLauncher;
import eu.cloudnetservice.launcher.java17.util.BootstrapUtil;
import eu.cloudnetservice.launcher.java17.util.HttpUtil;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpResponse;
import java.util.Properties;
import lombok.NonNull;

public final class LauncherUpdaterRegistry extends DefaultUpdaterRegistry<LauncherUpdaterContext, Object> {

  private final String repo;
  private final String branch;
  private final CloudNetLauncher launcher;

  public LauncherUpdaterRegistry(@NonNull String repo, @NonNull String branch, @NonNull CloudNetLauncher launcher) {
    this.repo = repo;
    this.branch = branch;
    this.launcher = launcher;
  }

  @Override
  protected @NonNull LauncherUpdaterContext provideContext(@NonNull Object provisionContext) throws Exception {
    // CHECKSTYLE.OFF: Launcher has no proper logger
    System.out.printf("Loading checksums (Update repo: %s, Update branch: %s)... %n", this.repo, this.branch);
    // CHECKSTYLE.ON
    // load the properties file which contains the checksum information
    return HttpUtil.get(
      GitHubUtil.buildUri(this.repo, this.branch, "checksums.properties"),
      info -> {
        if (info.statusCode() == 200) {
          return HttpResponse.BodySubscribers.mapping(
            HttpResponse.BodySubscribers.ofInputStream(),
            stream -> {
              try {
                // parse the checksum properties from the stream
                var checksums = new Properties();
                checksums.load(stream);
                // create an updater context from the information
                return new LauncherUpdaterContext(this.launcher, this.repo, this.branch, checksums);
              } catch (IOException exception) {
                // let the handler do the honors
                throw new UncheckedIOException(exception);
              }
            });
        } else if (info.statusCode() == 404) {
          // repo or branch not found
          System.out.printf(
            "Unable to load updater context because update repo \"%s\" or branch \"%s\" doesn't exist! Stopping in 5 seconds...%n",
            this.repo, this.branch);
        } else {
          // generic error
          System.out.printf(
            "Unable to load updater context (repo: \"%s\", branch: \"%s\") - got unexpected http status %d! Stopping in 5 seconds...%n",
            this.repo, this.branch, info.statusCode());
        }

        // wait and stop
        BootstrapUtil.waitAndExit();
        return null;
      }
    ).body();
  }
}
