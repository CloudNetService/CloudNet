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

import de.dytanic.cloudnet.console.animation.AbstractConsoleAnimation;

/**
 * Represents a progress bar animation in the console that by default is updated all 10 milliseconds
 */
public class ConsoleProgressBarAnimation extends AbstractConsoleAnimation {

  private final char progressChar;
  private final char emptyChar;
  private final char lastProgressChar;
  private final String prefix;
  private final String suffix;
  private long length;
  private long currentValue;

  /**
   * Creates a new {@link ConsoleProgressBarAnimation}
   *
   * @param fullLength       the maximum of the animation
   * @param startValue       the initial value for this animation
   * @param progressChar     the {@link Character} for each percent in the animation
   * @param lastProgressChar the {@link Character} which is at the last position of the animation
   * @param prefix           the prefix for this animation
   * @param suffix           the suffix for this animation
   */
  public ConsoleProgressBarAnimation(long fullLength, int startValue, char progressChar, char lastProgressChar,
    char emptyChar, String prefix, String suffix) {
    this.length = fullLength;
    this.currentValue = startValue;
    this.progressChar = progressChar;
    this.lastProgressChar = lastProgressChar;
    this.emptyChar = emptyChar;
    this.prefix = prefix;
    this.suffix = suffix;

    super.setUpdateInterval(10);
  }

  public long getLength() {
    return this.length;
  }

  public void setLength(int length) {
    this.length = length;
  }

  public long getCurrentValue() {
    return this.currentValue;
  }

  public void setCurrentValue(long currentValue) {
    this.currentValue = currentValue;
  }

  public void finish() {
    this.currentValue = this.length;
  }

  @Override
  protected boolean handleTick() {
    if (this.currentValue < this.length) {
      this.doUpdate(((double) this.currentValue / (double) this.length) * 100.0D);
      return false;
    }

    this.doUpdate(100D);
    return true;
  }

  protected String formatCurrentValue(long currentValue) {
    return this.formatValue(currentValue);
  }

  protected String formatLength(long length) {
    return this.formatValue(length);
  }

  protected String formatValue(long value) {
    return String.valueOf(value);
  }

  protected String formatTime(long millis) {
    long seconds = (millis / 1000);
    String min = String.valueOf(seconds / 60);
    String sec = String.valueOf(seconds - ((seconds / 60) * 60));

    if (min.length() == 1) {
      min = "0" + min;
    }

    if (sec.length() == 1) {
      sec = "0" + sec;
    }

    return min + ":" + sec;
  }

  private void doUpdate(double percent) {
    int roundPercent = (int) percent;
    char[] chars = new char[100];
    for (int i = 0; i < roundPercent; i++) {
      chars[i] = this.progressChar;
    }

    for (int i = roundPercent; i < 100; i++) {
      chars[i] = this.emptyChar;
    }

    chars[Math
      .max(0, roundPercent - 1)] = this.lastProgressChar; // make sure that we don't try to modify a negative index
    super.print(this.format(this.prefix, percent), String.valueOf(chars), this.format(this.suffix, percent));
  }

  protected String format(String input, double percent) {
    long millis = System.currentTimeMillis() - this.getStartTime();
    long time = millis / 1000;
    return input == null ? "" : input
      .replace("%value%", this.formatCurrentValue(this.currentValue))
      .replace("%length%", this.formatLength(this.length))
      .replace("%percent%", String.format("%.2f", percent))
      .replace("%time%", this.formatTime(millis))
      .replace("%bips%", String.valueOf(time == 0 ? "0" : (this.currentValue / 1024 * 8) / time)) // bits per second
      .replace("%byps%", String.valueOf(time == 0 ? "0" : (this.currentValue / 1024) / time)); // bytes per second
  }

}
