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

package de.dytanic.cloudnet.launcher.cnl.install;

import de.dytanic.cloudnet.launcher.cnl.CNLCommand;
import de.dytanic.cloudnet.launcher.version.util.Dependency;
import java.util.Collection;
import java.util.Map;

public final class CNLCommandInclude extends CNLCommand {

  private final Collection<Dependency> includes;

  public CNLCommandInclude(Collection<Dependency> includes) {
    super("include");
    this.includes = includes;
  }

  @Override
  public void execute(Map<String, String> variables, String commandLine, String... args) {
    if (args.length >= 4) {
      Dependency dependency = new Dependency(args[0], args[1], args[2], args[3], args.length == 5 ? args[4] : null);

      if (!this.includes.contains(dependency)) {
        this.includes.add(dependency);
      }
    }
  }
}
