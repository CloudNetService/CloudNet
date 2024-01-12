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

package eu.cloudnetservice.common.hash;

import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import lombok.NonNull;

/**
 * @deprecated for removal in a future version, do not use.
 */
@Deprecated(forRemoval = true)
public final class HashUtil {

  private HashUtil() {
    throw new UnsupportedOperationException();
  }

  public static byte @NonNull [] toSha256(@NonNull String text) {
    return Hashing.sha256().hashString(text, StandardCharsets.UTF_8).asBytes();
  }

  public static byte @NonNull [] toSha256(byte @NonNull [] bytes) {
    return Hashing.sha256().hashBytes(bytes).asBytes();
  }
}
