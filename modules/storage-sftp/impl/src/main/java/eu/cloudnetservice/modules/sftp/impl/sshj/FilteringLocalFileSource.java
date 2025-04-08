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

package eu.cloudnetservice.modules.sftp.impl.sshj;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.NonNull;
import net.schmizz.sshj.xfer.FileSystemFile;
import net.schmizz.sshj.xfer.LocalFileFilter;
import org.jetbrains.annotations.Nullable;

public final class FilteringLocalFileSource extends FileSystemFile {

  private final Path path;
  private final Predicate<Path> filter;

  public FilteringLocalFileSource(@NonNull Path path, @Nullable Predicate<Path> filter) {
    super(path.toFile());
    this.path = path;
    this.filter = filter;
  }

  @Override
  public Iterable<FileSystemFile> getChildren(LocalFileFilter filter) throws IOException {
    // check if there is a filter
    if (this.filter == null) {
      // no filter - we can use the present method
      return super.getChildren(filter);
    } else {
      return Files.list(this.path)
        .filter(this.filter)
        .map(file -> new FilteringLocalFileSource(file, this.filter))
        .collect(Collectors.toList());
    }
  }
}
