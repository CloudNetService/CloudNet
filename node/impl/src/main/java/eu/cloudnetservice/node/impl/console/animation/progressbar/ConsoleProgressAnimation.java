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

package eu.cloudnetservice.node.impl.console.animation.progressbar;

import eu.cloudnetservice.node.impl.console.animation.AbstractConsoleAnimation;
import eu.cloudnetservice.utils.base.StringUtil;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import lombok.NonNull;
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
    @NonNull String fractions,
    char leftBracket,
    char rightBracket,
    @NonNull String prefix,
    @NonNull String suffix,
    long unitSize,
    @NonNull String unitName,
    @NonNull DecimalFormat unitFormat,
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

  public static @NonNull ConsoleProgressAnimation createDefault(
    @NonNull String task,
    @NonNull String unitName,
    int unitSize,
    long maximum
  ) {
    return new ConsoleProgressAnimation(
      '█',
      ' ',
      " ▏▎▍▌▋▊▉",
      '[',
      ']',
      task + " (%ratio%): %percent%  ",
      " %speed% (%elapsed%/%eta%)",
      unitSize,
      unitName,
      new DecimalFormat("#.0"),
      maximum);
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

  protected @NonNull String render() {
    var elapsed = Duration.between(this.startInstant, Instant.now());

    var prefix = this.replacePlaceholders(this.prefix, elapsed);
    var suffix = this.replacePlaceholders(this.suffix, elapsed);

    var length =
      this.console.width() - 5 - this.console.displayLength(prefix) - this.console.displayLength(suffix);

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

  protected @NonNull String formatPercentage() {
    String percent;
    if (this.maximum > 0) {
      percent = (int) Math.floor(100D * this.current / this.maximum) + "%";
    } else {
      percent = "?%";
    }

    return " ".repeat(4 - percent.length()) + percent;
  }

  protected @NonNull String formatSpeed(@NonNull Duration elapsed) {
    // check if no time yet elapsed
    if (elapsed.getSeconds() == 0) {
      return "?" + this.unitName + "/s";
    }
    // format to minutes
    var speed = (double) (this.current / elapsed.getSeconds()) / this.unitSize;
    return this.unitFormat.format(speed) + this.unitName + "/s";
  }

  protected @NonNull String formatRatio() {
    // convert current and max to united values
    var unitedCurrent = String.valueOf(this.current / this.unitSize);
    var unitedMaximum = String.valueOf(this.maximum / this.unitSize);
    // format the ratio
    return String.format("%s%s/%s%s",
      " ".repeat(unitedMaximum.length() - unitedCurrent.length()),
      unitedCurrent,
      unitedMaximum,
      this.unitName);
  }

  protected @NonNull String formatElapsedTime(@NonNull Duration duration) {
    var seconds = duration.getSeconds();
    return String.format("%d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
  }

  protected @NonNull String formatEta(@NonNull Duration elapsed) {
    return this.maximum <= 0 || this.current <= 0
      ? this.formatElapsedTime(elapsed)
      : this.formatElapsedTime(elapsed.dividedBy(this.current).multipliedBy(this.maximum - this.current));
  }

  protected int fraction(int length) {
    var full = this.normalizeProgress() * length;
    return (int) Math.floor((full - Math.floor(full)) * this.fractions.length());
  }

  protected @NonNull String replacePlaceholders(@NonNull String input, @NonNull Duration elapsed) {
    return input
      .replace("%ratio%", this.formatRatio())
      .replace("%eta%", this.formatEta(elapsed))
      .replace("%percent%", this.formatPercentage())
      .replace("%speed%", this.formatSpeed(elapsed))
      .replace("%elapsed%", this.formatElapsedTime(elapsed));
  }

  public long current() {
    return this.current;
  }

  public long maximum() {
    return this.maximum;
  }
}
