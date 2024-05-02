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

package eu.cloudnetservice.node.setup;

import eu.cloudnetservice.driver.permission.Permission;
import eu.cloudnetservice.driver.permission.PermissionGroup;
import eu.cloudnetservice.driver.permission.PermissionManagement;
import eu.cloudnetservice.ext.component.ComponentFormats;
import eu.cloudnetservice.node.console.animation.setup.ConsoleSetupAnimation;
import eu.cloudnetservice.node.console.animation.setup.answer.Parsers;
import eu.cloudnetservice.node.console.animation.setup.answer.QuestionAnswerType;
import eu.cloudnetservice.node.console.animation.setup.answer.QuestionListEntry;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

@Singleton
public class PermissionGroupSetup implements DefaultSetup {

  private final Parsers parsers;
  private final PermissionManagement permissionManagement;

  @Inject
  public PermissionGroupSetup(@NonNull Parsers parsers, @NonNull PermissionManagement permissionManagement) {
    this.parsers = parsers;
    this.permissionManagement = permissionManagement;
  }

  @Override
  public void applyQuestions(@NonNull ConsoleSetupAnimation animation) {
    animation.addEntries(
      QuestionListEntry.<Boolean>builder()
        .key("addDefaultGroups")
        .translatedQuestion("cloudnet-init-perms-add-default-groups")
        .answerType(QuestionAnswerType.<Boolean>builder()
          .recommendation("yes")
          .possibleResults("no", "yes")
          .parser(this.parsers.bool()))
        .build());
  }

  @Override
  public void handleResults(@NonNull ConsoleSetupAnimation animation) {
    if (animation.result("addDefaultGroups")) {
      var adminGroup = PermissionGroup.builder()
        .name("admin")
        .potency(100)
        .addPermission(Permission.of("*"))
        .prefix(ComponentFormats.USER_INPUT
          .fromAdventure(Component.empty().color(NamedTextColor.GRAY)
            .append(Component.text("Admin ", NamedTextColor.DARK_RED))
            .append(Component.text("| ", NamedTextColor.DARK_GRAY))
          )
        )
        .color("dark_red")
        .suffix(ComponentFormats.USER_INPUT
          .fromAdventure(Component.empty().color(NamedTextColor.WHITE))
        )
        .display(ComponentFormats.USER_INPUT
          .fromAdventure(Component.empty().color(NamedTextColor.DARK_RED))
        )
        .sortId(10)
        .build();
      var defaultGroup = PermissionGroup.builder()
        .name("default")
        .potency(100)
        .addPermission(Permission.of("bukkit.broadcast.user"))
        .defaultGroup(true)
        .prefix(ComponentFormats.USER_INPUT
          .fromAdventure(Component.empty().color(NamedTextColor.GRAY))
        )
        .color("gray")
        .suffix(ComponentFormats.USER_INPUT
          .fromAdventure(Component.empty().color(NamedTextColor.WHITE))
        )
        .display(ComponentFormats.USER_INPUT
          .fromAdventure(Component.empty().color(NamedTextColor.GRAY))
        )
        .sortId(99)
        .build();

      this.permissionManagement.addPermissionGroup(adminGroup);
      this.permissionManagement.addPermissionGroup(defaultGroup);
    }
  }
}
