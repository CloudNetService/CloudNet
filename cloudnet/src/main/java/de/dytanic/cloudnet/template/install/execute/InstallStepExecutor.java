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

package de.dytanic.cloudnet.template.install.execute;

import de.dytanic.cloudnet.template.install.InstallInformation;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for install steps when downloading and patching server software
 */
@FunctionalInterface
public interface InstallStepExecutor {

  @NotNull Set<Path> execute(
    @NotNull InstallInformation info,
    @NotNull Path workingDirectory,
    @NotNull Set<Path> files) throws IOException;

  default void interrupt() {
  }
}
