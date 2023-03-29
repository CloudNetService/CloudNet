/*
 * Copyright 2019-2023 CloudNetService team & contributors
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

package eu.cloudnetservice.driver.document;

import java.util.Collection;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

public interface DocumentFactoryRegistry {

  @UnmodifiableView
  @NonNull Collection<DocumentFactory> documentFactories();

  @NonNull DocumentFactory documentFactory(@NonNull String formatName);

  @Nullable DocumentFactory findDocumentFactory(@NonNull String formatName);

  @Nullable DocumentFactory unregisterDocumentFactory(@NonNull String formatName);

  void registerDocumentFactory(@NonNull String formatName, @NonNull DocumentFactory factory);

  @Nullable DocumentFactory replaceDocumentFactory(@NonNull String formatName, @NonNull DocumentFactory factory);
}
