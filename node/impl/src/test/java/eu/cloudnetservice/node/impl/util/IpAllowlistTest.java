/*
 * Copyright 2019-present CloudNetService team & contributors
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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class IpAllowlistTest {

  @Test
  void testShouldNotUpdateAllowlistForSameEntries() {
    var setA = Set.of("127.0.0.1", "137.7.69.69", "1.1.1.1", "2606:4700:4700::1001");
    var setB = Set.copyOf(setA);
    var allowlistA = IpAllowlist.parse(setA);
    var allowlistB = allowlistA.updateIfNecessary(setB);
    Assertions.assertSame(allowlistA, allowlistB);
  }

  @Test
  void testShouldUpdateAllowlistForDifferentEntries() {
    var setA = Set.of("127.0.0.1", "137.7.69.69", "1.1.1.1", "2606:4700:4700::1001");
    var setB = Set.of("127.0.0.1", "137.7.69.69", "1.1.1.1", "2606:4700:4700::1001", "1.1.1.2");
    var allowlistA = IpAllowlist.parse(setA);
    var allowlistB = allowlistA.updateIfNecessary(setB);
    Assertions.assertNotSame(allowlistA, allowlistB);
  }

  @Test
  void testGlobalIpv4Allowlist() {
    var allowlist = IpAllowlist.parse(Set.of("0.0.0.0/0"));
    Assertions.assertTrue(allowlist.allows(InetAddresses.forString("127.0.0.1")));
    Assertions.assertTrue(allowlist.allows(InetAddresses.forString("192.168.1.1")));
    Assertions.assertTrue(allowlist.allows(InetAddresses.forString("172.16.17.32")));
    Assertions.assertTrue(allowlist.allows(InetAddresses.forString("255.255.255.255")));
    Assertions.assertFalse(allowlist.allows(InetAddresses.forString("::1")));
    Assertions.assertFalse(allowlist.allows(InetAddresses.forString("fd38:1689:a7a8::")));
    Assertions.assertFalse(allowlist.allows(InetAddresses.forString("fff1:622a:b4e8:3de7:0983:e58a:b80f:6224")));
    Assertions.assertFalse(allowlist.allows(InetAddresses.forString("559f:be3c:fe00:baa5:46d6:5365:45d6:78e1")));
  }

  @Test
  void testGlobalIpv6Allowlist() {
    var allowlist = IpAllowlist.parse(Set.of("::0/0"));
    Assertions.assertFalse(allowlist.allows(InetAddresses.forString("127.0.0.1")));
    Assertions.assertFalse(allowlist.allows(InetAddresses.forString("192.168.1.1")));
    Assertions.assertFalse(allowlist.allows(InetAddresses.forString("172.16.17.32")));
    Assertions.assertFalse(allowlist.allows(InetAddresses.forString("255.255.255.255")));
    Assertions.assertTrue(allowlist.allows(InetAddresses.forString("::1")));
    Assertions.assertTrue(allowlist.allows(InetAddresses.forString("fd38:1689:a7a8::")));
    Assertions.assertTrue(allowlist.allows(InetAddresses.forString("fff1:622a:b4e8:3de7:0983:e58a:b80f:6224")));
    Assertions.assertTrue(allowlist.allows(InetAddresses.forString("559f:be3c:fe00:baa5:46d6:5365:45d6:78e1")));
  }

  @Test
  void testMixedIpAllowlist() {
    var allowlist = IpAllowlist.parse(Set.of(
      "1.1.1.1",
      "172.17.0.0/16",
      "192.168.0.0/24",
      "fdb6:451d:3aa1::/48",
      "fde4:4694:57d5:66a7::/64",
      "c3a6:4ad7:851f:bbab:86c5:6ff6:35b1:aedf"
    ));

    // 1.1.1.1
    Assertions.assertTrue(allowlist.allows(InetAddresses.forString("1.1.1.1")));
    Assertions.assertFalse(allowlist.allows(InetAddresses.forString("1.0.0.1")));

    // 172.17.0.0/16
    Assertions.assertTrue(allowlist.allows(InetAddresses.forString("172.17.53.142")));
    Assertions.assertTrue(allowlist.allows(InetAddresses.forString("172.17.200.33")));
    Assertions.assertFalse(allowlist.allows(InetAddresses.forString("172.16.0.1")));
    Assertions.assertFalse(allowlist.allows(InetAddresses.forString("178.17.145.16")));

    // 192.168.0.0/24
    Assertions.assertTrue(allowlist.allows(InetAddresses.forString("192.168.0.34")));
    Assertions.assertTrue(allowlist.allows(InetAddresses.forString("192.168.0.237")));
    Assertions.assertFalse(allowlist.allows(InetAddresses.forString("192.176.0.1")));
    Assertions.assertFalse(allowlist.allows(InetAddresses.forString("192.168.1.16")));

    // fdb6:451d:3aa1::/48
    Assertions.assertTrue(allowlist.allows(InetAddresses.forString("fdb6:451d:3aa1:12ab:dead:beef:cafe:1234")));
    Assertions.assertTrue(allowlist.allows(InetAddresses.forString("fdb6:451d:3aa1:1a2b:3c4d:5e6f:7a8b:9c0d")));
    Assertions.assertFalse(allowlist.allows(InetAddresses.forString("fdb6:451d:3ab1:1a2b:3c4d:5e6f:7a8b:9c0d")));
    Assertions.assertFalse(allowlist.allows(InetAddresses.forString("fde4:451d:3aa1:1a2b:3c4d:5e6f:7a8b:9c0d")));

    // fde4:4694:57d5:66a7::/64
    Assertions.assertTrue(allowlist.allows(InetAddresses.forString("fde4:4694:57d5:66a7:1234:5678:9abc:def0")));
    Assertions.assertTrue(allowlist.allows(InetAddresses.forString("fde4:4694:57d5:66a7:abcd:ef01:2345:6789")));
    Assertions.assertFalse(allowlist.allows(InetAddresses.forString("fde4:4692:57d5:66a7:1234:5678:9abc:def0")));
    Assertions.assertFalse(allowlist.allows(InetAddresses.forString("adb6:451d:3aa1:1a2b:3c4d:5e6f:7a8b:9c0d")));

    // c3a6:4ad7:851f:bbab:86c5:6ff6:35b1:aedf
    Assertions.assertTrue(allowlist.allows(InetAddresses.forString("c3a6:4ad7:851f:bbab:86c5:6ff6:35b1:aedf")));
    Assertions.assertFalse(allowlist.allows(InetAddresses.forString("7946:dd68:8a70:d7f6:e90f:0dfd:5700:0a38")));
  }

  @Test
  void testBulkIpRangesAndAllowStatus() throws IOException {
    var testDataStream = IpAllowlistTest.class.getClassLoader().getResourceAsStream("ip_allowlist_test_ips.txt");
    Assertions.assertNotNull(testDataStream);
    try (var testDataReader = new BufferedReader(new InputStreamReader(testDataStream, StandardCharsets.UTF_8))) {
      var testDataLine = testDataReader.readLine();
      var testDataLineParts = testDataLine.split(" "); // <range to test> <range first> <range last> <3 in> <3 out>
      var rangeIpAllowlist = IpAllowlist.parse(Set.of(testDataLineParts[0]));

      var firstInSubnet = InetAddresses.forString(testDataLineParts[1]);
      var lastInSubnet = InetAddresses.forString(testDataLineParts[2]);
      Assertions.assertTrue(rangeIpAllowlist.allows(firstInSubnet));
      Assertions.assertTrue(rangeIpAllowlist.allows(lastInSubnet));

      var ipsInSubnet = testDataLineParts[3].split(";");
      Arrays.stream(ipsInSubnet)
        .map(InetAddresses::forString)
        .forEach(address -> Assertions.assertTrue(rangeIpAllowlist.allows(address)));

      var ipsOutsideSubnet = testDataLineParts[4].split(";");
      Arrays.stream(ipsOutsideSubnet)
        .map(InetAddresses::forString)
        .forEach(address -> Assertions.assertFalse(rangeIpAllowlist.allows(address)));
    }
  }
}
