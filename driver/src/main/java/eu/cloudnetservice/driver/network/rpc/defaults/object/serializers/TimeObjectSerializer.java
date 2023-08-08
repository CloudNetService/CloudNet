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

package eu.cloudnetservice.driver.network.rpc.defaults.object.serializers;

import eu.cloudnetservice.driver.network.rpc.object.ObjectSerializer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Collection of object serializers for all kinds of java.time classes. Calendar systems other than ISO-8601 are not
 * supported (such as the Japanese calendar).
 *
 * @since 4.0
 */
public final class TimeObjectSerializer {

  /**
   * Serializer for a year.
   *
   * @see Year
   */
  public static final ObjectSerializer<Year> YEAR_SERIALIZER = FunctionalObjectSerializer.of(
    dataBuf -> Year.of(dataBuf.readInt()),
    (dataBuf, year) -> dataBuf.writeInt(year.getValue())
  );

  /**
   * Serializer for a year and month mapping.
   *
   * @see YearMonth
   */
  public static final ObjectSerializer<YearMonth> YEAR_MONTH_SERIALIZER = FunctionalObjectSerializer.of(
    dataBuf -> YearMonth.of(dataBuf.readInt(), dataBuf.readByte()),
    (dataBuf, yearMonth) -> {
      dataBuf.writeInt(yearMonth.getYear());
      dataBuf.writeByte((byte) yearMonth.getMonthValue());
    }
  );

  /**
   * Serializer for a day in a month.
   *
   * @see MonthDay
   */
  public static final ObjectSerializer<MonthDay> MONTH_DAY_SERIALIZER = FunctionalObjectSerializer.of(
    dataBuf -> MonthDay.of(dataBuf.readByte(), dataBuf.readByte()),
    (dataBuf, monthDay) -> {
      dataBuf.writeByte((byte) monthDay.getMonthValue());
      dataBuf.writeByte((byte) monthDay.getDayOfMonth());
    }
  );

  /**
   * A serializer for a standard zone region or any zone offset. Custom implementations are not supported.
   *
   * @see ZoneId
   */
  public static final ObjectSerializer<ZoneId> ZONE_ID_SERIALIZER = FunctionalObjectSerializer.of(
    dataBuf -> ZoneId.of(dataBuf.readString()),
    (dataBuf, zoneId) -> dataBuf.writeString(zoneId.getId())
  );

  /**
   * A serializer for a duration.
   *
   * @see Duration
   */
  public static final ObjectSerializer<Duration> DURATION_SERIALIZER = FunctionalObjectSerializer.of(
    dataBuf -> Duration.ofSeconds(dataBuf.readLong(), dataBuf.readInt()),
    (dataBuf, duration) -> {
      dataBuf.writeLong(duration.getSeconds());
      dataBuf.writeInt(duration.getNano());
    }
  );

  /**
   * A serializer for a period.
   *
   * @see Period
   */
  public static final ObjectSerializer<Period> PERIOD_SERIALIZER = FunctionalObjectSerializer.of(
    dataBuf -> Period.of(dataBuf.readInt(), dataBuf.readInt(), dataBuf.readInt()),
    (dataBuf, period) -> {
      dataBuf.writeInt(period.getYears());
      dataBuf.writeInt(period.getMonths());
      dataBuf.writeInt(period.getDays());
    }
  );

  /**
   * A serializer for an instant.
   *
   * @see Instant
   */
  public static final ObjectSerializer<Instant> INSTANT_SERIALIZER = FunctionalObjectSerializer.of(
    dataBuf -> Instant.ofEpochSecond(dataBuf.readLong(), dataBuf.readInt()),
    (dataBuf, instant) -> {
      dataBuf.writeLong(instant.getEpochSecond());
      dataBuf.writeInt(instant.getNano());
    }
  );

  /**
   * A serializer for a local date.
   *
   * @see LocalDate
   */
  public static final ObjectSerializer<LocalDate> LOCAL_DATE_SERIALIZER = FunctionalObjectSerializer.of(
    dataBuf -> LocalDate.of(dataBuf.readInt(), dataBuf.readByte(), dataBuf.readByte()),
    (dataBuf, localDate) -> {
      dataBuf.writeInt(localDate.getYear());
      dataBuf.writeByte((byte) localDate.getMonthValue());
      dataBuf.writeByte((byte) localDate.getDayOfMonth());
    }
  );

  /**
   * A serializer for a local time.
   *
   * @see LocalTime
   */
  public static final ObjectSerializer<LocalTime> LOCAL_TIME_SERIALIZER = FunctionalObjectSerializer.of(
    dataBuf -> {
      var hour = dataBuf.readByte();
      if (hour < 0) {
        return LocalTime.of(~hour, 0);
      }

      var minute = dataBuf.readByte();
      if (minute < 0) {
        return LocalTime.of(hour, ~minute);
      }

      var second = dataBuf.readByte();
      if (second < 0) {
        return LocalTime.of(hour, minute, ~second);
      }

      var nano = dataBuf.readInt();
      return LocalTime.of(hour, minute, second, nano);
    },
    (dataBuf, localTime) -> {
      var nano = localTime.getNano();
      var second = localTime.getSecond();
      var minute = localTime.getMinute();
      var hour = localTime.getHour();

      if (nano == 0) {
        if (second == 0) {
          if (minute == 0) {
            dataBuf.writeByte((byte) ~hour);
          } else {
            dataBuf.writeByte((byte) hour);
            dataBuf.writeByte((byte) ~minute);
          }
        } else {
          dataBuf.writeByte((byte) hour);
          dataBuf.writeByte((byte) minute);
          dataBuf.writeByte((byte) ~second);
        }
      } else {
        dataBuf.writeByte((byte) hour);
        dataBuf.writeByte((byte) minute);
        dataBuf.writeByte((byte) second);
        dataBuf.writeInt(nano);
      }
    }
  );

  /**
   * A serializer for a local date and a local time mapping.
   *
   * @see LocalDateTime
   */
  public static final ObjectSerializer<LocalDateTime> LOCAL_DATE_TIME_SERIALIZER = FunctionalObjectSerializer.of(
    dataBuf -> {
      var date = dataBuf.readObject(LocalDate.class);
      var time = dataBuf.readObject(LocalTime.class);
      return LocalDateTime.of(date, time);
    },
    (dataBuf, localDateTime) -> {
      dataBuf.writeObject(localDateTime.toLocalDate());
      dataBuf.writeObject(localDateTime.toLocalTime());
    }
  );

  /**
   * A serializer for a date and time mapping in a specific time zone. Only the standard zone id implementations (zone
   * offset and zone region) are supported.
   *
   * @see ZonedDateTime
   */
  public static final ObjectSerializer<ZonedDateTime> ZONED_DATE_TIME_SERIALIZER = FunctionalObjectSerializer.of(
    dataBuf -> {
      var zoneId = dataBuf.readObject(ZoneId.class);
      var dateTime = dataBuf.readObject(LocalDateTime.class);
      return ZonedDateTime.of(dateTime, zoneId);
    },
    (dataBuf, zonedDateTime) -> {
      dataBuf.writeObject(zonedDateTime.getZone());
      dataBuf.writeObject(zonedDateTime.toLocalDateTime());
    }
  );

  /**
   * A serializer for a time with a zone offset.
   *
   * @see OffsetTime
   */
  public static final ObjectSerializer<OffsetTime> OFFSET_TIME_SERIALIZER = FunctionalObjectSerializer.of(
    dataBuf -> {
      var time = dataBuf.readObject(LocalTime.class);
      var offset = ZoneOffset.ofTotalSeconds(dataBuf.readInt());
      return OffsetTime.of(time, offset);
    },
    (dataBuf, offsetTime) -> {
      dataBuf.writeObject(offsetTime.toLocalTime());
      dataBuf.writeInt(offsetTime.getOffset().getTotalSeconds());
    }
  );

  /**
   * A serializer for a time and date mapping with a zone offset.
   *
   * @see OffsetDateTime
   */
  public static final ObjectSerializer<OffsetDateTime> OFFSET_DATE_TIME_SERIALIZER = FunctionalObjectSerializer.of(
    dataBuf -> {
      var dateTime = dataBuf.readObject(LocalDateTime.class);
      var offset = ZoneOffset.ofTotalSeconds(dataBuf.readInt());
      return OffsetDateTime.of(dateTime, offset);
    },
    (dataBuf, offsetTime) -> {
      dataBuf.writeObject(offsetTime.toLocalDateTime());
      dataBuf.writeInt(offsetTime.getOffset().getTotalSeconds());
    }
  );

  private TimeObjectSerializer() {
    throw new UnsupportedOperationException();
  }
}
