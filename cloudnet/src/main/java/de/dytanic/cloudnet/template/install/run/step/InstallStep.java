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
