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

package eu.cloudnetservice.utils.base.resource;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import lombok.NonNull;

/**
 * A small utility class to convert resource values between different formats.
 *
 * @since 4.0
 */
public final class ResourceFormatter {

  private static final long ONE_MB_IN_BYTES = 1024L * 1024L;
  private static final ThreadLocal<DecimalFormat> DECIMAL_FORMAT_2DIGIT_PRECISION =
    ThreadLocal.withInitial(() -> new DecimalFormat(".##", DecimalFormatSymbols.getInstance(Locale.ROOT)));

  private ResourceFormatter() {
    throw new UnsupportedOperationException();
  }

  /**
   * Formats the given double value with a two digit precision after the decimal point. The integer fraction of the
   * given double is kept untouched.
   *
   * @param value the value to format.
   * @return the formatted value.
   */
  public static @NonNull String formatTwoDigitPrecision(double value) {
    var decimalFormat = DECIMAL_FORMAT_2DIGIT_PRECISION.get();
    return decimalFormat.format(value);
  }

  /**
   * Converts the given double value into a percentage. This method only works correctly when given a value in the [0.0,
   * 1.0] interval. If the given value is if smaller than {@code 0}, this method returns {@code -1}. Note: the return
   * value is maxed out at 100, even if an invalid value is passed in, this method always returns a value in the
   * interval [-1, 100].
   *
   * @param value the value to convert to a percentage.
   * @return the converted value as specified.
   */
  public static double convertToPercentage(double value) {
    if (value == 0) {
      return 0;
    } else if (value < 0) {
      return -1;
    } else {
      return Math.min(100D, value * 100D);
    }
  }

  /**
   * Converts the given byte count to a megabyte value. This method makes the assumption that the given value is always
   * a meaningful byte count.
   *
   * @param bytesToConvert the byte count to convert to megabytes.
   * @return the given byte count, converted to megabytes.
   */
  public static long convertBytesToMb(long bytesToConvert) {
    return bytesToConvert / ONE_MB_IN_BYTES;
  }
}
