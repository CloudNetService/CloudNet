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

package eu.cloudnetservice.modules.report.config;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import lombok.NonNull;

public record RecordConfiguration(
  boolean saveRecords,
  boolean saveOnForceStopOnly,
  boolean logRecordCreation,
  @NonNull Path recordDestination,
  long serviceLifetime,
  @NonNull SimpleDateFormat dateFormat
) {

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull RecordConfiguration configuration) {
    return builder()
      .saveRecords(configuration.saveRecords())
      .saveOnForceStopOnly(configuration.saveOnForceStopOnly())
      .logRecordCreation(configuration.logRecordCreation())
      .recordDestination(configuration.recordDestination())
      .serviceLifetime(configuration.serviceLifetime())
      .dateFormat(configuration.dateFormat());
  }

  public static class Builder {

    private boolean saveRecords = true;
    private boolean saveOnForceStopOnly = false;
    private boolean logRecordCreation = true;
    private Path recordDestination = Path.of("records");
    private long serviceLifetime = 5000L;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public @NonNull Builder saveRecords(boolean saveRecords) {
      this.saveRecords = saveRecords;
      return this;
    }

    public @NonNull Builder saveOnForceStopOnly(boolean saveOnForceStopOnly) {
      this.saveOnForceStopOnly = saveOnForceStopOnly;
      return this;
    }

    public @NonNull Builder logRecordCreation(boolean logRecordCreation) {
      this.logRecordCreation = logRecordCreation;
      return this;
    }

    public @NonNull Builder recordDestination(@NonNull Path recordDestination) {
      this.recordDestination = recordDestination;
      return this;
    }

    public @NonNull Builder serviceLifetime(long serviceLifetime) {
      this.serviceLifetime = serviceLifetime;
      return this;
    }

    public @NonNull Builder dateFormat(@NonNull SimpleDateFormat dateFormat) {
      this.dateFormat = dateFormat;
      return this;
    }

    public @NonNull RecordConfiguration build() {
      return new RecordConfiguration(
        this.saveRecords,
        this.saveOnForceStopOnly,
        this.logRecordCreation,
        this.recordDestination,
        this.serviceLifetime,
        this.dateFormat);
    }
  }
}
