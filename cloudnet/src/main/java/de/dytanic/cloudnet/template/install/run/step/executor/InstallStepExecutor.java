package de.dytanic.cloudnet.template.install.run.step.executor;

import de.dytanic.cloudnet.template.install.run.InstallInformation;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public interface InstallStepExecutor {

  @NotNull Set<Path> execute(@NotNull InstallInformation installInformation, @NotNull Path workingDirectory,
    @NotNull Set<Path> inputPaths) throws IOException;

  default void interrupt() {
  }

}
