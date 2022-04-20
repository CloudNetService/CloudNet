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

import java.util.List;
import lombok.NonNull;

public record ReportConfiguration(
  @NonNull RecordConfiguration records,
  @NonNull List<PasteService> pasteServers
) {

  public static @NonNull Builder builder() {
    return new Builder();
  }

  public static @NonNull Builder builder(@NonNull ReportConfiguration configuration) {
    return builder()
      .records(configuration.records())
      .pasteServers(configuration.pasteServers());
  }

  public static final class Builder {

    private RecordConfiguration records = RecordConfiguration.builder().build();
    private List<PasteService> pasteServers = List.of(new PasteService("default", "https://just-paste.it"));

    public @NonNull Builder pasteServers(@NonNull List<PasteService> pasteServers) {
      this.pasteServers = List.copyOf(pasteServers);
      return this;
    }

    public @NonNull Builder records(@NonNull RecordConfiguration records) {
      this.records = records;
      return this;
    }

    public @NonNull ReportConfiguration build() {
      return new ReportConfiguration(this.records, this.pasteServers);
    }
  }
}
