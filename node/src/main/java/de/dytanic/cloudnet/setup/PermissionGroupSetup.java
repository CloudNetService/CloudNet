/*
 * Copyright 2019-2022 CloudNetService team & contributors
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
import lombok.NonNull;

public class PermissionGroupSetup implements DefaultSetup {

  @Override
  public void applyQuestions(@NonNull ConsoleSetupAnimation animation) {
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
  public void handleResults(@NonNull ConsoleSetupAnimation animation) {
    if (animation.result("addDefaultGroups")) {
      var adminPermissionGroup = new PermissionGroup("Admin", 100);
      adminPermissionGroup.addPermission("*");
      adminPermissionGroup.addPermission("Proxy", "*");
      adminPermissionGroup.prefix("&4Admin &8| &7");
      adminPermissionGroup.color("&7");
      adminPermissionGroup.suffix("&f");
      adminPermissionGroup.display("&4");
      adminPermissionGroup.sortId(10);

      var defaultPermissionGroup = new PermissionGroup("default", 100);
      defaultPermissionGroup.addPermission("bukkit.broadcast.user", true);
      defaultPermissionGroup.defaultGroup(true);
      defaultPermissionGroup.prefix("&7");
      defaultPermissionGroup.color("&7");
      defaultPermissionGroup.suffix("&f");
      defaultPermissionGroup.display("&7");
      defaultPermissionGroup.sortId(10);

      CloudNet.instance().permissionManagement().addPermissionGroup(adminPermissionGroup);
      CloudNet.instance().permissionManagement().addPermissionGroup(defaultPermissionGroup);
    }
  }
}
