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

package eu.cloudnetservice.cloudnet.ext.report.paste.emitter;

import de.dytanic.cloudnet.service.CloudService;

/**
 * Represents an emitter for collecting data for reports of this module. An emitter can be registered using {@link
 * EmitterRegistry#registerDataEmitter(Class, ReportDataEmitter[])}
 *
 * @param <T> the context type that the data is collected for
 */
@FunctionalInterface
public interface ReportDataEmitter<T> {

  /**
   * Adds data while creating a paste for support and debugging purposes.
   *
   * @param builder the already collected data, just append it to this one
   * @param context the context of the report, like {@link CloudService}
   */
  void emitData(StringBuilder builder, T context);

}
