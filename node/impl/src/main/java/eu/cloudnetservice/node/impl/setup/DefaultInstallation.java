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

package eu.cloudnetservice.node.impl.setup;

import eu.cloudnetservice.driver.event.EventManager;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.language.I18n;
import eu.cloudnetservice.driver.registry.Service;
import eu.cloudnetservice.node.impl.console.Console;
import eu.cloudnetservice.node.impl.console.animation.setup.ConsoleSetupAnimation;
import eu.cloudnetservice.node.impl.log.QueuedConsoleLogAppender;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.LockSupport;
import lombok.NonNull;

@Singleton
public final class DefaultInstallation {

  private final Queue<Class<? extends DefaultSetup>> setups = new ConcurrentLinkedQueue<>();

  private final I18n i18n;
  private final Console console;
  private final EventManager eventManager;
  private final QueuedConsoleLogAppender logHandler;

  @Inject
  public DefaultInstallation(
    @NonNull @Service I18n i18n,
    @NonNull Console console,
    @NonNull EventManager eventManager,
    @NonNull QueuedConsoleLogAppender logHandler
  ) {
    this.i18n = i18n;
    this.console = console;
    this.eventManager = eventManager;
    this.logHandler = logHandler;
  }

  public void executeFirstStartSetup() {
    if (!Boolean.getBoolean("cloudnet.installation.skip") && !this.setups.isEmpty()) {
      var animation = this.createAnimation();
      var runningThread = Thread.currentThread();

      // construct all setups & apply the questions
      Queue<DefaultSetup> setups = new LinkedList<>();
      for (var setupClass : this.setups) {
        // construct
        var injectionLayer = InjectionLayer.findLayerOf(setupClass);
        var setupInstance = injectionLayer.instance(setupClass);

        // apply questions & register
        setups.add(setupInstance);
        setupInstance.applyQuestions(animation);
      }

      // start the animation
      animation.cancellable(false);
      this.console.startAnimation(animation);

      animation.addFinishHandler(() -> {
        // post the finish handling to the installations
        DefaultSetup setup;
        while ((setup = setups.poll()) != null) {
          setup.handleResults(animation);
        }
        // notify the monitor about the success
        LockSupport.unpark(runningThread);
      });

      // wait for the finish signal
      LockSupport.park();
    }
  }

  public void registerSetup(@NonNull Class<? extends DefaultSetup> setup) {
    this.setups.add(setup);
  }

  private @NonNull ConsoleSetupAnimation createAnimation() {
    return new ConsoleSetupAnimation(
      this.i18n,
      this.eventManager,
      this.logHandler,
      """
        &f   ___  _                    _ &b     __    __  _____  &3  _____              _           _  _\s
        &f  / __\\| |  ___   _   _   __| |&b  /\\ \\ \\  /__\\/__   \\ &3  \\_   \\ _ __   ___ | |_   __ _ | || |
        &f / /   | | / _ \\ | | | | / _` |&b /  \\/ / /_\\    / /\\/ &3   / /\\/| '_ \\ / __|| __| / _` || || |
        &f/ /___ | || (_) || |_| || (_| |&b/ /\\  / //__   / /    &3/\\/ /_  | | | |\\__ \\| |_ | (_| || || |
        &f\\____/ |_| \\___/  \\__,_| \\__,_|&b\\_\\ \\/  \\__/   \\/     &3\\____/  |_| |_||___/ \\__| \\__,_||_||_|
        &f                               &b                      &3                                     \s""",
      null,
      "&r> &e");
  }
}
