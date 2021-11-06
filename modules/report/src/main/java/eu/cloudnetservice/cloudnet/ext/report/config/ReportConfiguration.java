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

package eu.cloudnetservice.cloudnet.ext.report.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class ReportConfiguration {

  public static final ReportConfiguration DEFAULT = new ReportConfiguration(
    true,
    false,
    Paths.get("records"),
    5000L,
    new SimpleDateFormat("yyyy-MM-dd"),
    Collections.singletonList(new PasteService("default", "https://just-paste.it"))
  );

  private final boolean saveRecords;
  private final boolean saveOnCrashOnly;
  private final Path recordDestination;
  private final long serviceLifetime;
  private final SimpleDateFormat dateFormat;
  private final List<PasteService> pasteServers;

  public ReportConfiguration(
    boolean saveRecords,
    boolean saveOnCrashOnly,
    @NotNull Path recordDestination,
    long serviceLifetime,
    @NotNull SimpleDateFormat dateFormat,
    @NotNull List<PasteService> pasteServers
  ) {
    this.saveRecords = saveRecords;
    this.saveOnCrashOnly = saveOnCrashOnly;
    this.recordDestination = recordDestination;
    this.serviceLifetime = serviceLifetime;
    this.dateFormat = dateFormat;
    this.pasteServers = pasteServers;
  }

  public boolean isSaveRecords() {
    return this.saveRecords;
  }

  public boolean isSaveOnCrashOnly() {
    return this.saveOnCrashOnly;
  }

  public @NotNull Path getRecordDestination() {
    return this.recordDestination;
  }

  public long getServiceLifetime() {
    return this.serviceLifetime;
  }

  public @NotNull SimpleDateFormat getDateFormat() {
    return this.dateFormat;
  }

  public @NotNull List<PasteService> getPasteServers() {
    return this.pasteServers;
  }
}
