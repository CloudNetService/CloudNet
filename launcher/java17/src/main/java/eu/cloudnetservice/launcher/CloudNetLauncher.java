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

package eu.cloudnetservice.launcher;

import eu.cloudnetservice.launcher.cnl.CnlInterpreter;
import eu.cloudnetservice.launcher.cnl.defaults.VarCnlCommand;
import eu.cloudnetservice.launcher.dependency.DependencyHelper;
import eu.cloudnetservice.launcher.updater.LauncherUpdaterContext;
import eu.cloudnetservice.launcher.updater.LauncherUpdaterRegistry;
import eu.cloudnetservice.launcher.updater.updaters.LauncherChecksumsFileUpdater;
import eu.cloudnetservice.launcher.updater.updaters.LauncherCloudNetUpdater;
import eu.cloudnetservice.launcher.updater.updaters.LauncherModuleJsonUpdater;
import eu.cloudnetservice.launcher.updater.updaters.LauncherPatcherUpdater;
import eu.cloudnetservice.launcher.updater.updaters.LauncherUpdater;
import eu.cloudnetservice.launcher.utils.CommandLineHelper;
import eu.cloudnetservice.updater.UpdaterRegistry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;
import java.util.jar.JarInputStream;
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

    // updater init - must be after the cnl run to ensure that all updater variables are present
    this.registry = new LauncherUpdaterRegistry(
      CommandLineHelper.findProperty(this.commandLineArguments, "updateRepo", "CloudNetService/launchermeta"),
      CommandLineHelper.findProperty(this.commandLineArguments, "updateBranch", "master"),
      this);

    // start the application
    this.startApplication();
  }

  private void startApplication() throws Exception {
    var devMode = CommandLineHelper.findProperty(this.commandLineArguments, "dev", "false", Boolean::parseBoolean);
    if (devMode) {
      // do not run the updater - load from the jar which must be located in the root directory
      Path cloudNetPath = Path.of("cloudnet.jar");
      if (Files.notExists(cloudNetPath)) {
        throw new IllegalArgumentException("CloudNet is not at the required path");
      }
      // load the dependencies from the jar
      DependencyHelper.loadFromLibrariesFile(cloudNetPath);
      // start the application
      this.startApplication(cloudNetPath);
    } else {
      // register the default updaters
      this.registry.registerUpdater(new LauncherUpdater());
      this.registry.registerUpdater(new LauncherPatcherUpdater());
      this.registry.registerUpdater(new LauncherCloudNetUpdater());
      this.registry.registerUpdater(new LauncherModuleJsonUpdater());
      this.registry.registerUpdater(new LauncherChecksumsFileUpdater());
      // run the updater - use a new object as a placeholder as we need no special context here
      this.registry.runUpdater(new Object());
      // start the application
      this.startApplication(Path.of("launcher", "cloudnet.jar"));
    }
  }

  private void startApplication(@NonNull Path cloudNetJarPath) throws Exception {
    // create the launcher loader and append the cloudnet jar path to it
    var launcherLoader = this.buildLauncherClassLoader();
    launcherLoader.addURL(cloudNetJarPath.toUri().toURL());
    // get the main class from
    String mainClass;
    try (var stream = new JarInputStream(Files.newInputStream(cloudNetJarPath))) {
      mainClass = stream.getManifest().getMainAttributes().getValue("Main-Class");
      Objects.requireNonNull(mainClass, "Unable to resolve main class in application file " + cloudNetJarPath);
    }
    // start the jar
    var main = launcherLoader.loadClass(mainClass).getDeclaredMethod("main", String[].class);
    main.setAccessible(true);
    // let's start it!
    main.invoke(null, (Object) this.originalArgs);
  }

  private @NonNull LauncherClassLoader buildLauncherClassLoader() throws IOException {
    // add all jars in the libraries' folder to the loader. This allows users to load own dependencies if needed
    try (var stream = Files.walk(DependencyHelper.LIB_PATH)) {
      return new LauncherClassLoader(stream
        .filter(path -> !Files.isDirectory(path))
        .map(Path::toAbsolutePath)
        .map(path -> {
          try {
            // we can do this unchecked - this should never fail (if it fails everything fails...)
            return path.toUri().toURL();
          } catch (MalformedURLException exception) {
            throw new UncheckedIOException(exception);
          }
        })
        .toArray(URL[]::new));
    }
  }

  private void runCnlInterpreter() throws Exception {
    var launcherCnlPath = Path.of("launcher.cnl");
    if (Files.notExists(launcherCnlPath)) {
      // copy the launcher.cnl file now
      try (var launcherCnlStream = CloudNetLauncher.class.getClassLoader().getResourceAsStream("launcher.cnl")) {
        Files.copy(launcherCnlPath, launcherCnlPath);
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
