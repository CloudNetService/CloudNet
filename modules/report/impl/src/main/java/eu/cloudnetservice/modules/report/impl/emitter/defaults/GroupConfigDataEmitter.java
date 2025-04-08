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

import eu.cloudnetservice.driver.provider.GroupConfigurationProvider;
import eu.cloudnetservice.driver.registry.AutoService;
import eu.cloudnetservice.driver.service.GroupConfiguration;
import eu.cloudnetservice.modules.report.impl.emitter.ReportDataEmitter;
import eu.cloudnetservice.modules.report.impl.emitter.ReportDataWriter;
import eu.cloudnetservice.modules.report.impl.emitter.SpecificReportDataEmitter;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Collection;
import lombok.NonNull;

@Singleton
@AutoService(services = ReportDataEmitter.class, name = "GroupConfig")
public final class GroupConfigDataEmitter extends SpecificReportDataEmitter<GroupConfiguration> {

  private final GroupConfigurationProvider groupConfigurationProvider;

  @Inject
  public GroupConfigDataEmitter(@NonNull GroupConfigurationProvider groupConfigurationProvider) {
    super((writer, groups) -> writer.appendString("Groups (").appendInt(groups.size()).appendString("):"));
    this.groupConfigurationProvider = groupConfigurationProvider;
  }

  @Override
  public @NonNull Class<GroupConfiguration> emittingType() {
    return GroupConfiguration.class;
  }

  @Override
  public @NonNull Collection<GroupConfiguration> collectData() {
    return this.groupConfigurationProvider.groupConfigurations();
  }

  @Override
  public @NonNull ReportDataWriter emitData(@NonNull ReportDataWriter writer, @NonNull GroupConfiguration value) {
    return writer.beginSection(value.name()).appendAsJson(value).endSection();
  }
}
