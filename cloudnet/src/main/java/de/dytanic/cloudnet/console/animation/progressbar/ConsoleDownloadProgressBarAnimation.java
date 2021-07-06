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

package de.dytanic.cloudnet.console.animation.progressbar;

public class ConsoleDownloadProgressBarAnimation extends ConsoleProgressBarAnimation {

  public ConsoleDownloadProgressBarAnimation(long fullLength, int startValue, char progressChar,
    char lastProgressChar, char emptyChar, String prefix, String suffix) {
    super(fullLength, startValue, progressChar, lastProgressChar, emptyChar, prefix, suffix);
  }

  @Override
  protected String formatValue(long value) {
    return String.format("%.3f", (double) value / 1024D / 1024D); // format to MB
  }
}
