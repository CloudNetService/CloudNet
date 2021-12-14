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

package de.dytanic.cloudnet.setup;

import static de.dytanic.cloudnet.console.animation.setup.answer.Parsers.bool;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.console.animation.setup.ConsoleSetupAnimation;
import de.dytanic.cloudnet.console.animation.setup.answer.QuestionAnswerType;
import de.dytanic.cloudnet.console.animation.setup.answer.QuestionListEntry;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import org.jetbrains.annotations.NotNull;

public class PermissionGroupSetup implements DefaultSetup {

  @Override
  public void applyQuestions(@NotNull ConsoleSetupAnimation animation) {
    animation.addEntries(
      QuestionListEntry.<Boolean>builder()
        .key("addDefaultGroups")
        .translatedQuestion("cloudnet-init-perms-add-default-groups")
        .answerType(QuestionAnswerType.<Boolean>builder()
          .recommendation("yes")
          .possibleResults("no", "yes")
          .parser(bool()))
        .build());
  }

  @Override
  public void handleResults(@NotNull ConsoleSetupAnimation animation) {
    if (animation.getResult("addDefaultGroups")) {
      var adminPermissionGroup = new PermissionGroup("Admin", 100);
      adminPermissionGroup.addPermission("*");
      adminPermissionGroup.addPermission("Proxy", "*");
      adminPermissionGroup.setPrefix("&4Admin &8| &7");
      adminPermissionGroup.setColor("&7");
      adminPermissionGroup.setSuffix("&f");
      adminPermissionGroup.setDisplay("&4");
      adminPermissionGroup.setSortId(10);

      var defaultPermissionGroup = new PermissionGroup("default", 100);
      defaultPermissionGroup.addPermission("bukkit.broadcast.user", true);
      defaultPermissionGroup.setDefaultGroup(true);
      defaultPermissionGroup.setPrefix("&7");
      defaultPermissionGroup.setColor("&7");
      defaultPermissionGroup.setSuffix("&f");
      defaultPermissionGroup.setDisplay("&7");
      defaultPermissionGroup.setSortId(10);

      CloudNet.getInstance().getPermissionManagement().addPermissionGroup(adminPermissionGroup);
      CloudNet.getInstance().getPermissionManagement().addPermissionGroup(defaultPermissionGroup);
    }
  }
}
