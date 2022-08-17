/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

package eu.cloudnetservice.modules.npc.platform.util;

import com.google.common.base.CharMatcher;
import eu.cloudnetservice.common.StringUtil;
import lombok.NonNull;

public final class UserNameUtil {

  private static final CharMatcher USER_NAME_MATCHER = CharMatcher.inRange('a', 'z')
    .or(CharMatcher.inRange('A', 'Z'))
    .or(CharMatcher.inRange('0', '9'))
    .or(CharMatcher.is('_'))
    .negate();

  private UserNameUtil() {
    throw new UnsupportedOperationException();
  }

  public static @NonNull String convertStringToValidName(@NonNull String input) {
    // remove all forbidden chars from the input string & check if the name would
    // be smaller than the minimum required of 3 chars - if so generate a random name
    var extracted = USER_NAME_MATCHER.removeFrom(input);
    var userNameLength = extracted.length();
    if (userNameLength < 3) {
      return StringUtil.generateRandomString(15);
    }

    // check if the name would be longer than allowed and shorten it in that case
    if (userNameLength > 16) {
      return extracted.substring(0, 16);
    }

    // username is ok
    return extracted;
  }
}
