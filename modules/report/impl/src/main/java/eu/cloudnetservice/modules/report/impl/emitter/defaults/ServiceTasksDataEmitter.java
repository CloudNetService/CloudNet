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

import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.registry.AutoService;
import eu.cloudnetservice.driver.service.ServiceTask;
import eu.cloudnetservice.modules.report.impl.emitter.ReportDataEmitter;
import eu.cloudnetservice.modules.report.impl.emitter.ReportDataWriter;
import eu.cloudnetservice.modules.report.impl.emitter.SpecificReportDataEmitter;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Collection;
import lombok.NonNull;

@Singleton
@AutoService(services = ReportDataEmitter.class, name = "ServiceTasks")
public final class ServiceTasksDataEmitter extends SpecificReportDataEmitter<ServiceTask> {

  private final ServiceTaskProvider serviceTaskProvider;

  @Inject
  public ServiceTasksDataEmitter(@NonNull ServiceTaskProvider serviceTaskProvider) {
    super((writer, tasks) -> writer.appendString("Tasks (").appendInt(tasks.size()).appendString("):"));
    this.serviceTaskProvider = serviceTaskProvider;
  }

  @Override
  public @NonNull Class<ServiceTask> emittingType() {
    return ServiceTask.class;
  }

  @Override
  public @NonNull Collection<ServiceTask> collectData() {
    return this.serviceTaskProvider.serviceTasks();
  }

  @Override
  public @NonNull ReportDataWriter emitData(@NonNull ReportDataWriter writer, @NonNull ServiceTask value) {
    return writer.beginSection(value.name()).appendAsJson(value).endSection();
  }
}
