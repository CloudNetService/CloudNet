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

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.console.IConsole;
import de.dytanic.cloudnet.console.animation.questionlist.ConsoleQuestionListAnimation;
import de.dytanic.cloudnet.driver.permission.IPermissionGroup;
import de.dytanic.cloudnet.driver.permission.PermissionGroup;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

public class DefaultInstallation {

  private final Collection<DefaultSetup> setups = Arrays.asList(
    new DefaultConfigSetup(),
    new DefaultClusterSetup(),
    new DefaultTaskSetup()
  );

  private ConsoleQuestionListAnimation animation;

  private ConsoleQuestionListAnimation createAnimation() {
    return new ConsoleQuestionListAnimation(
      "DefaultInstallation",
      null,
      () -> "&f   ___  _                    _ &b     __    __  _____  &3  _____              _           _  _ \n" +
        "&f  / __\\| |  ___   _   _   __| |&b  /\\ \\ \\  /__\\/__   \\ &3  \\_   \\ _ __   ___ | |_   __ _ | || |\n" +
        "&f / /   | | / _ \\ | | | | / _` |&b /  \\/ / /_\\    / /\\/ &3   / /\\/| '_ \\ / __|| __| / _` || || |\n" +
        "&f/ /___ | || (_) || |_| || (_| |&b/ /\\  / //__   / /    &3/\\/ /_  | | | |\\__ \\| |_ | (_| || || |\n" +
        "&f\\____/ |_| \\___/  \\__,_| \\__,_|&b\\_\\ \\/  \\__/   \\/     &3\\____/  |_| |_||___/ \\__| \\__,_||_||_|\n"
        +
        "&f                               &b                      &3                                      ",
      () -> null,
      "&r> &e"
    );
  }

  public void executeFirstStartSetup(IConsole console, boolean configFileAvailable) throws Exception {
    Collection<DefaultSetup> executedSetups = new ArrayList<>();

    for (DefaultSetup setup : this.setups) {
      if (setup.shouldAsk(configFileAvailable)) {
        if (this.animation == null) {
          this.animation = this.createAnimation();
        }

        setup.applyQuestions(this.animation);
        executedSetups.add(setup);
      }
    }

    if (this.animation != null) {
      this.animation.setCancellable(false);

      console.clearScreen();
      console.startAnimation(this.animation);

      ITask<Void> task = new ListenableTask<>(() -> null);

      this.animation.addFinishHandler(() -> {

        for (DefaultSetup setup : executedSetups) {
          setup.execute(this.animation);
        }

        try {
          task.call();
        } catch (Exception exception) {
          exception.printStackTrace();
        }
      });

      try {
        task.get(); //wait for the results by the user
      } catch (InterruptedException | ExecutionException exception) {
        exception.printStackTrace();
      }

    }
  }

  public void postExecute() {
    for (DefaultSetup setup : this.setups) {
      setup.postExecute(this.animation);
    }
  }

  public void initDefaultPermissionGroups() {
    if (CloudNet.getInstance().getPermissionManagement().getGroups().isEmpty()
      && System.getProperty("cloudnet.default.permissions.skip") == null) {
      IPermissionGroup adminPermissionGroup = new PermissionGroup("Admin", 100);
      adminPermissionGroup.addPermission("*");
      adminPermissionGroup.addPermission("Proxy", "*");
      adminPermissionGroup.setPrefix("&4Admin &8| &7");
      adminPermissionGroup.setColor("&7");
      adminPermissionGroup.setSuffix("&f");
      adminPermissionGroup.setDisplay("&4");
      adminPermissionGroup.setSortId(10);

      CloudNet.getInstance().getPermissionManagement().addGroup(adminPermissionGroup);

      IPermissionGroup defaultPermissionGroup = new PermissionGroup("default", 100);
      defaultPermissionGroup.addPermission("bukkit.broadcast.user", true);
      defaultPermissionGroup.setDefaultGroup(true);
      defaultPermissionGroup.setPrefix("&7");
      defaultPermissionGroup.setColor("&7");
      defaultPermissionGroup.setSuffix("&f");
      defaultPermissionGroup.setDisplay("&7");
      defaultPermissionGroup.setSortId(10);

      CloudNet.getInstance().getPermissionManagement().addGroup(defaultPermissionGroup);
    }
  }

}
