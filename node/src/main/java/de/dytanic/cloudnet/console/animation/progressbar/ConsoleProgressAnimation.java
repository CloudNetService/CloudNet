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

import de.dytanic.cloudnet.common.StringUtil;
import de.dytanic.cloudnet.console.animation.AbstractConsoleAnimation;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

/**
 * Represents a progress bar animation in the console that by default is updated all 10 milliseconds
 */
public class ConsoleProgressAnimation extends AbstractConsoleAnimation {

  // style
  private final char blockChar;
  private final char spaceChar;
  private final String fractions;
  private final char leftBracket;
  private final char rightBracket;

  // animation
  private final String prefix;
  private final String suffix;

  // unit settings
  private final long unitSize;
  private final String unitName;
  private final DecimalFormat unitFormat;

  // 0            current   max
  // [============>         ]
  private long current;
  private long maximum;

  public ConsoleProgressAnimation(
    char blockChar,
    char spaceChar,
    @NotNull String fractions,
    char leftBracket,
    char rightBracket,
    @NotNull String prefix,
    @NotNull String suffix,
    long unitSize,
    @NotNull String unitName,
    @NotNull DecimalFormat unitFormat,
    long maximum
  ) {
    super(10);

    // style
    this.blockChar = blockChar;
    this.spaceChar = spaceChar;
    this.fractions = fractions;
    this.leftBracket = leftBracket;
    this.rightBracket = rightBracket;
    // animation
    this.prefix = prefix;
    this.suffix = suffix;
    // unit
    this.unitSize = unitSize;
    this.unitName = unitName;
    this.unitFormat = unitFormat;

    this.maximum = maximum;
    this.startInstant = Instant.now();
  }

  @Override
  protected boolean handleTick() {
    super.print(this.render());
    return this.maximum <= this.current;
  }

  public void step() {
    this.stepBy(1);
  }

  public void stepBy(long length) {
    this.stepTo(this.current + length);
  }

  public void stepToEnd() {
    this.stepTo(this.maximum);
  }

  public void stepTo(long length) {
    this.current = length;
    // prevent issues with unknown or lager sized streams
    if (length > this.maximum) {
      this.maximum = length;
    }
  }

  public @Range(from = 0, to = 1) double normalizeProgress() {
    return this.maximum > 0 ? this.current > this.maximum ? 1 : ((double) this.current / this.maximum) : 1;
  }

  protected @NotNull String render() {
    var elapsed = Duration.between(this.startInstant, Instant.now());

    var prefix = this.replacePlaceholders(this.prefix, elapsed);
    var suffix = this.replacePlaceholders(this.suffix, elapsed);

    var length =
      this.console.getWidth() - 5 - this.console.getDisplayLength(prefix) - this.console.getDisplayLength(suffix);

    var builder = new StringBuilder(prefix).append(this.leftBracket);
    // append the full blocks
    builder.append(StringUtil.repeat(this.blockChar, (int) (length * this.normalizeProgress())));
    // check if we have reached the end yet
    if (this.maximum > this.current) {
      // append the best fitting fraction char
      builder.append(this.fractions.charAt(this.fraction(length)));
      // fill the line with spaces
      builder.append(StringUtil.repeat(this.spaceChar, length - (int) (length * this.normalizeProgress())));
    }
    // append the suffix and finish rendering
    return builder.append(this.rightBracket).append(suffix).toString();
  }

  protected @NotNull String formatPercentage() {
    String percent;
    if (this.maximum > 0) {
      percent = (int) Math.floor(100D * this.current / this.maximum) + "%";
    } else {
      percent = "?%";
    }

    return StringUtil.repeat(' ', 4 - percent.length()) + percent;
  }

  protected @NotNull String formatSpeed(@NotNull Duration elapsed) {
    // check if no time yet elapsed
    if (elapsed.getSeconds() == 0) {
      return "?" + this.unitName + "/s";
    }
    // format to minutes
    var speed = (double) (this.current / elapsed.getSeconds()) / this.unitSize;
    return this.unitFormat.format(speed) + this.unitName + "/s";
  }

  protected @NotNull String formatRatio() {
    // convert current and max to united values
    var unitedCurrent = String.valueOf(this.current / this.unitSize);
    var unitedMaximum = String.valueOf(this.maximum / this.unitSize);
    // format the ratio
    return String.format("%s%s/%s%s",
      StringUtil.repeat(' ', unitedMaximum.length() - unitedCurrent.length()),
      unitedCurrent,
      unitedMaximum,
      this.unitName);
  }

  protected @NotNull String formatElapsedTime(@NotNull Duration duration) {
    var seconds = duration.getSeconds();
    return String.format("%d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
  }

  protected @NotNull String formatEta(@NotNull Duration elapsed) {
    return this.maximum <= 0 || this.current <= 0
      ? this.formatElapsedTime(elapsed)
      : this.formatElapsedTime(elapsed.dividedBy(this.current).multipliedBy(this.maximum - this.current));
  }

  protected int fraction(int length) {
    var full = this.normalizeProgress() * length;
    return (int) Math.floor((full - Math.floor(full)) * this.fractions.length());
  }

  protected @NotNull String replacePlaceholders(@NotNull String input, @NotNull Duration elapsed) {
    return input
      .replace("%ratio%", this.formatRatio())
      .replace("%eta%", this.formatEta(elapsed))
      .replace("%percent%", this.formatPercentage())
      .replace("%speed%", this.formatSpeed(elapsed))
      .replace("%elapsed%", this.formatElapsedTime(elapsed));
  }

  public long getCurrent() {
    return this.current;
  }

  public long getMaximum() {
    return this.maximum;
  }
}
