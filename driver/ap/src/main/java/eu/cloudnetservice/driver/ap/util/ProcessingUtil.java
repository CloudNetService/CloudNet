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

package eu.cloudnetservice.driver.ap.util;

import java.util.List;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import lombok.NonNull;

public final class ProcessingUtil {

  private ProcessingUtil() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull List<? extends TypeMirror> getTypesFromAnnotationProperty(@NonNull Runnable extractor) {
    try {
      extractor.run();
      throw new IllegalStateException("Extractor did not access type(s) from annotation member");
    } catch (MirroredTypeException exception) {
      // a single type was mirrored (Class)
      return List.of(exception.getTypeMirror());
    } catch (MirroredTypesException exception) {
      // multiple types were mirrored (Class[])
      return exception.getTypeMirrors();
    }
  }
}
