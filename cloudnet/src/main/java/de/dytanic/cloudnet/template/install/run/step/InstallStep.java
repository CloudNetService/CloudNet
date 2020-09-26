package de.dytanic.cloudnet.template.install.run.step;

import de.dytanic.cloudnet.template.install.run.InstallInformation;
import de.dytanic.cloudnet.template.install.run.step.executor.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public enum InstallStep {
    DOWNLOAD(new DownloadStepExecutor()),
    BUILD(new BuildStepExecutor()),
    UNZIP(new UnzipStepExecutor()),
    COPY_FILTER(new CopyFilterStepExecutor()),
    DEPLOY(new DeployStepExecutor());

    private final InstallStepExecutor executor;

    InstallStep(InstallStepExecutor executor) {
        this.executor = executor;
    }

    public @NotNull Set<Path> execute(@NotNull InstallInformation installInformation, @NotNull Path workingDirectory, @NotNull Set<Path> inputPaths) throws IOException {
        return this.executor.execute(installInformation, workingDirectory, inputPaths);
    }

    public void interrupt() {
        this.executor.interrupt();
    }

}
