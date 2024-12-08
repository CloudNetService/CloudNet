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

package eu.cloudnetservice.modules.report.impl.emitter.defaults;

import eu.cloudnetservice.driver.module.ModuleProvider;
import eu.cloudnetservice.driver.module.ModuleWrapper;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.driver.registry.AutoService;
import eu.cloudnetservice.modules.report.impl.emitter.ReportDataEmitter;
import eu.cloudnetservice.modules.report.impl.emitter.ReportDataWriter;
import eu.cloudnetservice.modules.report.impl.emitter.SpecificReportDataEmitter;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import lombok.NonNull;

@Singleton
@AutoService(services = ReportDataEmitter.class, name = "LocalModule")
public final class LocalModuleDataEmitter extends SpecificReportDataEmitter<ModuleWrapper> {

  private final ModuleProvider moduleProvider;

  @Inject
  public LocalModuleDataEmitter(@NonNull ModuleProvider moduleProvider) {
    super((writer, modules) -> writer.appendString("Local Modules (").appendInt(modules.size()).appendString("):"));
    this.moduleProvider = moduleProvider;
  }

  @Override
  public @NonNull Class<ModuleWrapper> emittingType() {
    return ModuleWrapper.class;
  }

  @Override
  public @NonNull Collection<ModuleWrapper> collectData() {
    return this.moduleProvider.modules();
  }

  @Override
  public @NonNull ReportDataWriter emitData(@NonNull ReportDataWriter writer, @NonNull ModuleWrapper value) {
    var module = value.module();
    writer = writer
      .beginSection(module.name())
      // Group: eu.cloudnetservice; Name: CloudFlare; Version: 1.0; Lifecycle: STARTED
      .appendString("Group: ")
      .appendString(module.group())
      .appendString("; Name: ")
      .appendString(module.name())
      .appendString("; Version: ")
      .appendString(module.version())
      .appendString("; Lifecycle: ")
      .appendString(value.moduleLifeCycle().name())
      .appendNewline();

    // append configuration if possible
    writer.appendString("Module Configuration:").appendNewline();
    if (module.moduleConfig().storesSensitiveData()) {
      // sensitive data, don't print that out
      writer.appendString("<retracted, stores sensitive data>");
    } else if (module instanceof DriverModule driverModule) {
      try {
        // print out the whole config
        var configPath = driverModule.configPath();
        writer.appendString(Files.readString(configPath, StandardCharsets.UTF_8));
      } catch (IOException exception) {
        writer.appendString("<unable to read configuration file: " + exception.getMessage() + ">");
      }
    } else {
      // unable to read the config
      writer.appendString("<unable to read configuration>");
    }

    // end the section
    return writer.endSection();
  }
}
