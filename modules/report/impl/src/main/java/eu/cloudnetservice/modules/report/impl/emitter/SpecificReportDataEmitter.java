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

package eu.cloudnetservice.modules.report.impl.emitter;

import java.util.Collection;
import java.util.function.BiConsumer;
import lombok.NonNull;

public abstract class SpecificReportDataEmitter<T> implements ReportDataEmitter {

  private final BiConsumer<ReportDataWriter, Collection<T>> sectionTitleDecorator;

  public SpecificReportDataEmitter(@NonNull String sectionTitle) {
    this((writer, ignored) -> writer.appendString(sectionTitle));
  }

  public SpecificReportDataEmitter(@NonNull BiConsumer<ReportDataWriter, Collection<T>> sectionTitleDecorator) {
    this.sectionTitleDecorator = sectionTitleDecorator;
  }

  @Override
  public @NonNull ReportDataWriter emitData(@NonNull ReportDataWriter writer) {
    var data = this.collectData();
    writer = writer.beginSection(sectionWriter -> this.sectionTitleDecorator.accept(sectionWriter, data));

    // write the data
    for (var value : data) {
      writer = this.emitData(writer, value);
    }

    // end the section
    return writer.endSection();
  }

  public abstract @NonNull Class<T> emittingType();

  public abstract @NonNull Collection<T> collectData();

  public abstract @NonNull ReportDataWriter emitData(@NonNull ReportDataWriter writer, @NonNull T value);
}
