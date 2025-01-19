/*
 * Copyright 2019-2024 CloudNetService team & contributors
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

package eu.cloudnetservice.node.impl.version.execute;

import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.node.impl.version.execute.defaults.BuildStepExecutor;
import eu.cloudnetservice.node.impl.version.execute.defaults.CopyFilterStepExecutor;
import eu.cloudnetservice.node.impl.version.execute.defaults.DeployStepExecutor;
import eu.cloudnetservice.node.impl.version.execute.defaults.DownloadStepExecutor;
import eu.cloudnetservice.node.impl.version.execute.defaults.FabricApiVersionFetch;
import eu.cloudnetservice.node.impl.version.execute.defaults.PaperApiVersionFetchStepExecutor;
import eu.cloudnetservice.node.impl.version.execute.defaults.SpongeApiVersionFetchStepExecutor;
import eu.cloudnetservice.node.impl.version.execute.defaults.UnzipStepExecutor;
import eu.cloudnetservice.node.impl.version.information.VersionInstaller;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import lombok.NonNull;

public enum InstallStep {

  DOWNLOAD(DownloadStepExecutor.class),
  BUILD(BuildStepExecutor.class),
  UNZIP(new UnzipStepExecutor()),
  COPY_FILTER(new CopyFilterStepExecutor()),
  DEPLOY(new DeployStepExecutor()),
  PAPER_API(new PaperApiVersionFetchStepExecutor()),
  FABRIC_API(new FabricApiVersionFetch()),
  SPONGE_API(new SpongeApiVersionFetchStepExecutor());

  private final InstallStepExecutor executor;

  InstallStep(@NonNull InstallStepExecutor executor) {
    this.executor = executor;
  }

  InstallStep(@NonNull Class<? extends InstallStepExecutor> executorClass) {
    this(InjectionLayer.findLayerOf(executorClass).instance(executorClass));
  }

  public @NonNull Set<Path> execute(
    @NonNull VersionInstaller versionInstaller,
    @NonNull Path workingDirectory,
    @NonNull Set<Path> inputPaths
  ) throws IOException {
    return this.executor.execute(versionInstaller, workingDirectory, inputPaths);
  }

  public void interrupt() {
    this.executor.interrupt();
  }
}
