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

package de.dytanic.cloudnet.launcher.cnl.defaults;

import de.dytanic.cloudnet.launcher.cnl.CNLCommand;
import java.util.Arrays;
import java.util.Map;

public final class CNLCommandVar extends CNLCommand {

  public CNLCommandVar() {
    super("var");
  }


  @Override
  public void execute(Map<String, String> variables, String commandLine, String... args) {
    if (args.length > 1) {
      variables.put(args[0], String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
    }
  }
}
