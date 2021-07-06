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

package de.dytanic.cloudnet.template.install.run.step;

import de.dytanic.cloudnet.template.install.run.InstallInformation;
import de.dytanic.cloudnet.template.install.run.step.executor.BuildStepExecutor;
import de.dytanic.cloudnet.template.install.run.step.executor.CopyFilterStepExecutor;
import de.dytanic.cloudnet.template.install.run.step.executor.DeployStepExecutor;
import de.dytanic.cloudnet.template.install.run.step.executor.DownloadStepExecutor;
import de.dytanic.cloudnet.template.install.run.step.executor.InstallStepExecutor;
import de.dytanic.cloudnet.template.install.run.step.executor.PaperApiVersionFetchStepExecutor;
import de.dytanic.cloudnet.template.install.run.step.executor.UnzipStepExecutor;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public enum InstallStep {
  DOWNLOAD(new DownloadStepExecutor()),
  BUILD(new BuildStepExecutor()),
  UNZIP(new UnzipStepExecutor()),
  COPY_FILTER(new CopyFilterStepExecutor()),
  DEPLOY(new DeployStepExecutor()),
  PAPER_API(new PaperApiVersionFetchStepExecutor());

  private final InstallStepExecutor executor;

  InstallStep(InstallStepExecutor executor) {
    this.executor = executor;
  }

  public @NotNull Set<Path> execute(@NotNull InstallInformation installInformation, @NotNull Path workingDirectory,
    @NotNull Set<Path> inputPaths) throws IOException {
    return this.executor.execute(installInformation, workingDirectory, inputPaths);
  }

  public void interrupt() {
    this.executor.interrupt();
  }

}
