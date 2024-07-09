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

package eu.cloudnetservice.launcher.java22;

import eu.cloudnetservice.ext.updater.UpdaterRegistry;
import eu.cloudnetservice.launcher.java22.cnl.CnlInterpreter;
import eu.cloudnetservice.launcher.java22.cnl.defaults.VarCnlCommand;
import eu.cloudnetservice.launcher.java22.dependency.DependencyHelper;
import eu.cloudnetservice.launcher.java22.updater.LauncherUpdaterContext;
import eu.cloudnetservice.launcher.java22.updater.LauncherUpdaterRegistry;
import eu.cloudnetservice.launcher.java22.updater.updaters.LauncherChecksumsFileUpdater;
import eu.cloudnetservice.launcher.java22.updater.updaters.LauncherCloudNetUpdater;
import eu.cloudnetservice.launcher.java22.updater.updaters.LauncherModuleJsonUpdater;
import eu.cloudnetservice.launcher.java22.updater.updaters.LauncherPatcherUpdater;
import eu.cloudnetservice.launcher.java22.updater.updaters.LauncherUpdater;
import eu.cloudnetservice.launcher.java22.util.BootstrapUtil;
import eu.cloudnetservice.launcher.java22.util.CommandLineHelper;
import eu.cloudnetservice.launcher.java22.util.Environment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import lombok.NonNull;

@SuppressWarnings("unused") // called by reflection
public final class CloudNetLauncher {

  private final Path workingDirectory;
  private final String[] originalArgs;
  private final Properties commandLineArguments;

  private final CnlInterpreter cnlInterpreter;
  private final UpdaterRegistry<LauncherUpdaterContext, Object> registry;

  public CloudNetLauncher(@NonNull String[] args) throws Exception {
    this.originalArgs = args.clone();
    this.commandLineArguments = CommandLineHelper.parseCommandLine(args);

    // cnl init
    this.cnlInterpreter = new CnlInterpreter();
    this.runCnlInterpreter();

    // the working directory can be set using a variable - run after the interpreter
    this.workingDirectory = CommandLineHelper.findProperty(
      this.commandLineArguments,
      "launcherdir",
      "launcher",
      Path::of);
    Files.createDirectories(this.workingDirectory);

    // ensure that the application only runs once
    var lock = new ApplicationLock();
    if (lock.acquireLock(this.workingDirectory)) {
      // successfully got the lock, release on exit
      Runtime.getRuntime().addShutdownHook(new Thread(lock::releaseLock));
    } else {
      // CHECKSTYLE.OFF: Launcher has no proper logger
      // the application is already running, warn about that
      System.err.println("╔═══════════════════════════════════════════════════════════════════╗");
      System.err.println("║                              WARNING                              ║");
      System.err.println("║                                                                   ║");
      System.err.println("║     It looks like CloudNet is already running! Stop the other     ║");
      System.err.println("║          running instance and try running CloudNet again!         ║");
      System.err.println("║                                                                   ║");
      System.err.println("║              This instance will stop in 5 Seconds!                ║");
      System.err.println("║                                                                   ║");
      System.err.println("║                                                                   ║");
      System.err.println("║  If you are sure that there are no instances running, delete the  ║");
      System.err.println("║          app.lock file located in the launcher directory.         ║");
      System.err.println("╚═══════════════════════════════════════════════════════════════════╝");
      // wait 10 Seconds and stop
      BootstrapUtil.waitAndExit();
      // CHECKSTYLE.ON
    }

    // updater init - must be after the cnl run to ensure that all updater variables are present
    this.registry = new LauncherUpdaterRegistry(
      CommandLineHelper.findProperty(this.commandLineArguments, "updateRepo", "CloudNetService/launchermeta"),
      CommandLineHelper.findProperty(this.commandLineArguments, "updateBranch", "release"),
      this);

    // start the application
    this.startApplication();
  }

  private void startApplication() throws Exception {
    // store the path of the cloudnet jar here as it differs based on the execution mode
    Path cloudNetJarPath;
    var devMode = CommandLineHelper.findProperty(this.commandLineArguments, "dev", "false", Boolean::parseBoolean);
    if (devMode) {
      // do not run the updater - load from the jar which must be located in the root directory
      cloudNetJarPath = Path.of("cloudnet.jar");
      if (Files.notExists(cloudNetJarPath)) {
        throw new IllegalArgumentException("CloudNet is not at the required path for running in dev-mode");
      }
    } else {
      // not running in development - warn when running as root or admin
      if (Environment.runningAsRootOrAdmin()) {
        // CHECKSTYLE.OFF: Launcher has no proper logger
        System.err.println("╔═══════════════════════════════════════════════════════════════════╗");
        System.err.println("║                              WARNING                              ║");
        System.err.println("║                                                                   ║");
        System.err.println("║    It looks like CloudNet is running as an administrative user!   ║");
        System.err.println("║      This is not recommended as it allows attackers to f. ex.     ║");
        System.err.println("║              easily delete files on your system!                  ║");
        System.err.println("║                                                                   ║");
        System.err.println("║ More info: https://madelinemiller.dev/blog/root-minecraft-server/ ║");
        System.err.println("║                                                                   ║");
        System.err.println("║                 CloudNet will start in 5 seconds!                 ║");
        System.err.println("║                                                                   ║");
        System.err.println("╚═══════════════════════════════════════════════════════════════════╝");
        // wait 5 seconds
        TimeUnit.SECONDS.sleep(5);
        // CHECKSTYLE.ON
      }

      // this path must not exist yet, the updaters are required to create it
      cloudNetJarPath = Path.of("launcher", "cloudnet.jar");
      var autoUpdaterEnabled = CommandLineHelper.findProperty(
        this.commandLineArguments,
        "auto.update",
        "true",
        Boolean::parseBoolean);
      // register the default updaters
      this.registry.registerUpdater(new LauncherUpdater());
      this.registry.registerUpdater(new LauncherPatcherUpdater());
      this.registry.registerUpdater(new LauncherCloudNetUpdater());
      this.registry.registerUpdater(new LauncherModuleJsonUpdater());
      this.registry.registerUpdater(new LauncherChecksumsFileUpdater());
      // run the updater - use a new object as a placeholder as we need no special context here
      this.registry.runUpdater(new Object(), !autoUpdaterEnabled);
    }

    // resolve the debugger port; use -1 if not given to disable the debugger
    int debugPort = CommandLineHelper.findProperty(this.commandLineArguments, "debug.port", "-1", Integer::parseInt);

    // resolve the memory for the node process
    int nodeMemory = CommandLineHelper.findProperty(this.commandLineArguments, "node.memory", "256", Integer::parseInt);

    // resolve all dependencies & start the application
    var dependencies = DependencyHelper.loadFromLibrariesFile(cloudNetJarPath);
    ApplicationBootstrap.bootstrap(cloudNetJarPath, dependencies, this.originalArgs, debugPort, nodeMemory);
  }

  private void runCnlInterpreter() throws Exception {
    var launcherCnlPath = Path.of("launcher.cnl");
    if (Files.notExists(launcherCnlPath)) {
      // copy the launcher.cnl file now
      try (var launcherCnlStream = CloudNetLauncher.class.getClassLoader().getResourceAsStream("launcher.cnl")) {
        // the resource is present, just to make intellij happy
        if (launcherCnlStream != null) {
          Files.copy(launcherCnlStream, launcherCnlPath);
        }
      }
    }
    // register the default cnl commands
    this.cnlInterpreter.registerCommand("var", new VarCnlCommand());
    // run the interpreter on the file
    this.cnlInterpreter.interpret(launcherCnlPath);
  }

  public @NonNull Path workingDirectory() {
    return this.workingDirectory;
  }
}
