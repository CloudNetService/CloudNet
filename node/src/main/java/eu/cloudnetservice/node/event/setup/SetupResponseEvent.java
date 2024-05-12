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

package eu.cloudnetservice.node.event.setup;

import eu.cloudnetservice.node.console.animation.setup.ConsoleSetupAnimation;
import eu.cloudnetservice.node.console.animation.setup.answer.QuestionListEntry;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

public class SetupResponseEvent extends SetupEvent {

  private final QuestionListEntry<?> responseEntry;
  private final Object response;

  public SetupResponseEvent(
    @NonNull ConsoleSetupAnimation setup,
    @NonNull QuestionListEntry<?> responseEntry,
    @Nullable Object response
  ) {
    super(setup);
    this.responseEntry = responseEntry;
    this.response = response;
  }

  public @NonNull QuestionListEntry<?> responseEntry() {
    return this.responseEntry;
  }

  public @Nullable Object response() {
    return this.response;
  }
}
