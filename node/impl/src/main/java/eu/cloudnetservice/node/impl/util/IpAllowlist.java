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

package eu.cloudnetservice.node.impl.util;

import com.google.common.net.InetAddresses;
import java.math.BigInteger;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.NonNull;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An allowlist for IP addresses that is able to parse and process subnets and static IP addresses.
 *
 * @since 4.0
 */
public final class IpAllowlist {

  private static final Logger LOGGER = LoggerFactory.getLogger(IpAllowlist.class);

  private final int originalAllowlistHashCode;
  private final Collection<ParsedAllowlistEntry> entries;

  /**
   * Constructs a new allowlist instance. For internal use only, use {@link #parse(Collection)} instead.
   *
   * @param originalAllowlistHashCode the hashcode of the original allowlist that was parsed.
   * @param entries                   the entries of the newly constructed allowlist.
   * @throws NullPointerException if the given entries collection is null.
   */
  private IpAllowlist(int originalAllowlistHashCode, @NonNull Collection<ParsedAllowlistEntry> entries) {
    this.originalAllowlistHashCode = originalAllowlistHashCode;
    this.entries = entries;
  }

  /**
   * Parses an allowlist from the given collection of subnets or static ip addresses. All null entries are ignored, for
   * invalid entries a warning log message is printed.
   *
   * @param allowlist the allowlist to parse.
   * @return a new allowlist instance based on the parsed given allowlist.
   * @throws NullPointerException if the given allowlist is null.
   */
  public static @NonNull IpAllowlist parse(@NonNull Collection<String> allowlist) {
    var allowListEntries = allowlist.stream()
      .filter(Objects::nonNull)
      .distinct()
      .map(ParsedAllowlistEntry::parse)
      .filter(Objects::nonNull) // remove all entries that couldn't be parsed
      .collect(Collectors.toUnmodifiableSet());
    return new IpAllowlist(allowlist.hashCode(), allowListEntries);
  }

  /**
   * Updates this allowlist instance if the given allowlist is different from the original allowlist that was used to
   * construct this instance. If the given allowlist is the same as the original allowlist, this instance is returned.
   * Otherwise, a new instance is created for the given allowlist and returned instead.
   *
   * @param allowList the allowlist to check for changes and return a new instance for, if necessary.
   * @return a new allowlist if the given allowlist is different from the original allowlist, otherwise this instance.
   * @throws NullPointerException if the given allowlist is null.
   */
  @CheckReturnValue
  public @NonNull IpAllowlist updateIfNecessary(@NonNull Collection<String> allowList) {
    var allowListHashCode = allowList.hashCode();
    return this.originalAllowlistHashCode == allowListHashCode ? this : parse(allowList);
  }

  /**
   * Checks if the given address is allowed by an entry of this allowlist.
   *
   * @param address the address to check.
   * @return true if the given address is allowed, false otherwise.
   * @throws NullPointerException if the given address is null.
   */
  public boolean allows(@NonNull InetAddress address) {
    var addressBytes = address.getAddress();
    var addressBits = addressBytes.length * Byte.SIZE;
    var addressBigInt = new BigInteger(1, addressBytes);

    for (var entry : this.entries) {
      if (entry.accepts(addressBits, addressBigInt)) {
        LOGGER.trace("Address {} passed allowlist check, endorsed by entry '{}'", address, entry.originalSubnet);
        return true;
      }
    }

    return false;
  }

  /**
   * An allowlist entry that was parsed from a subnet.
   *
   * @param addressBits      the number of bits that are in the parsed address.
   * @param originalSubnet   the original subnet that was parsed.
   * @param maskedAddress    the masked subnet address where the dynamic part zeroed out.
   * @param variableBitsMask the mask used for zeroing out the dynamic part of the address.
   * @since 4.0
   */
  private record ParsedAllowlistEntry(
    int addressBits,
    @NonNull String originalSubnet,
    @NonNull BigInteger maskedAddress,
    @NonNull BigInteger variableBitsMask
  ) {

    /**
     * Parses the given subnet into an allowlist entry, if possible. Returns null if the subnet couldn't be parsed. Note
     * that this method does not throw any exceptions on a parsing failure; they are just logged.
     *
     * @param subnet the subnet to parse and convert.
     * @return the parsed subnet, null if parsing was not possible.
     * @throws NullPointerException if the given subnet is null.
     */
    public static @Nullable IpAllowlist.ParsedAllowlistEntry parse(@NonNull String subnet) {
      try {
        var parts = subnet.split("/", 2);
        var addressBytes = InetAddresses.forString(parts[0]).getAddress();
        var addressBits = addressBytes.length * Byte.SIZE; // 32 for IPv4, 128 for IPv6
        var prefixLength = switch (parts.length) {
          case 1 -> addressBits;
          case 2 -> Integer.parseInt(parts[1]);
          default -> throw new AssertionError();
        };

        // constructs a mask of the variable bits in the address. for example, for an address with a prefix length of 16
        // this would mean that the leading 16 bits are static while the remaining 16 bits are variable, resulting in:
        // 11111111111111110000000000000000
        var variableBits = addressBits - prefixLength;
        var variableBitsMask = BigInteger.ONE
          .shiftLeft(addressBits)
          .subtract(BigInteger.ONE)
          .shiftRight(variableBits)
          .shiftLeft(variableBits);

        // converts the IP bytes into a positive big integer, zeroing-out the dynamic part of the address while
        // leaving the static bits in place. for example, for 172.16.32.8/16 this would result in:
        // 10101100000100000000000000000000
        var addressBigInt = new BigInteger(1, addressBytes);
        var maskedAddress = addressBigInt.and(variableBitsMask);
        return new ParsedAllowlistEntry(addressBits, subnet, maskedAddress, variableBitsMask);
      } catch (IllegalArgumentException exception) {
        LOGGER.warn("Unable to parse subnet '{}' into an allow list entry: {}", subnet, exception.getMessage());
        return null;
      }
    }

    /**
     * Checks if the given address matches the wrapped address. This is if the static part of the wrapped address equals
     * the same part in the given address.
     *
     * @param addressBits the number of bits that are in the given address.
     * @param address     the address to check, in big integer representation.
     * @return true if the given address matches the wrapped address, false otherwise.
     * @throws NullPointerException if the given address is null.
     */
    public boolean accepts(int addressBits, @NonNull BigInteger address) {
      // check if the given address is of the same type as the wrapped address
      if (this.addressBits != addressBits) {
        return false;
      }

      // check if the non-dynamic part of the given address matches the wrapped address
      var maskedAddress = address.and(this.variableBitsMask);
      return this.maskedAddress.equals(maskedAddress);
    }
  }
}
