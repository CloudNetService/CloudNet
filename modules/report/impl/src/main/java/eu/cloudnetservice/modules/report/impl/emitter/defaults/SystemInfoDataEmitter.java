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

package eu.cloudnetservice.modules.report.impl.emitter.defaults;

import eu.cloudnetservice.driver.registry.AutoService;
import eu.cloudnetservice.modules.report.impl.emitter.ReportDataEmitter;
import eu.cloudnetservice.modules.report.impl.emitter.ReportDataWriter;
import jakarta.inject.Singleton;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import lombok.NonNull;
import oshi.SystemInfo;
import oshi.util.FormatUtil;

@Singleton
@AutoService(services = ReportDataEmitter.class, name = "SystemInfo")
public final class SystemInfoDataEmitter implements ReportDataEmitter {

  private static final SystemInfo SYSTEM_INFO = new SystemInfo();
  private static final RuntimeMXBean RUNTIME_MX_BEAN = ManagementFactory.getRuntimeMXBean();

  private static @NonNull String formatSeconds(long seconds) {
    return String.format("%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
  }

  @Override
  public @NonNull ReportDataWriter emitData(@NonNull ReportDataWriter writer) {
    var hardware = SYSTEM_INFO.getHardware();
    var operatingSystem = SYSTEM_INFO.getOperatingSystem();
    var fileSystem = operatingSystem.getFileSystem();

    // general information about the system
    var dataWriter = writer
      .beginSection("System Information")
      // Microsoft Windows 11 build 22621 x64; Uptime: 02:09:07
      .appendString(operatingSystem.toString())
      .appendString(" x")
      .appendInt(operatingSystem.getBitness())
      .appendString("; ")
      .appendString("Uptime: ").appendString(formatSeconds(operatingSystem.getSystemUptime()))
      .appendNewline()
      // Java: Eclipse Adoptium 17 (OpenJDK 64-Bit Server VM 17.0.2+8)
      .appendString("Java: ")
      .appendString(RUNTIME_MX_BEAN.getVmVendor())
      .appendString(" ")
      .appendString(RUNTIME_MX_BEAN.getSpecVersion())
      .appendString(" (")
      .appendString(RUNTIME_MX_BEAN.getVmName())
      .appendString(" ")
      .appendString(RUNTIME_MX_BEAN.getVmVersion())
      .appendString(")")
      .appendNewline()
      .appendNewline()
      // CPU: AMD Ryzen 7 5800X 8-Core Processor; Physical Cores: 8; Logical Cores: 16
      .appendString("CPU: ")
      .appendString(hardware.getProcessor().getProcessorIdentifier().getName().trim())
      .appendString("; Physical Cores: ").appendInt(hardware.getProcessor().getPhysicalProcessorCount())
      .appendString("; Logical Cores: ").appendInt(hardware.getProcessor().getLogicalProcessorCount())
      .appendNewline()
      // Memory: 31,9 GiB; Available: 13,2 GiB
      .appendString("Memory: ").appendString(FormatUtil.formatBytes(hardware.getMemory().getTotal()))
      .appendString("; Available: ").appendString(FormatUtil.formatBytes(hardware.getMemory().getAvailable()))
      .appendNewline()
      // File Descriptors: Open: 1152; Max: 9223372036854775807; Per-Process-Max: 9223372036854775807
      .appendString("File Descriptors: Open: ").appendLong(fileSystem.getOpenFileDescriptors())
      .appendString("; Max: ").appendLong(fileSystem.getMaxFileDescriptors())
      .appendString("; Per-Process-Max: ").appendLong(fileSystem.getMaxFileDescriptorsPerProcess())
      .appendNewline()
      .appendNewline();

    // Network Interfaces: 3:
    var networkInterfaces = hardware.getNetworkIFs();
    dataWriter
      .appendString("Network Interfaces: ")
      .appendInt(networkInterfaces.size())
      .appendString(":")
      .appendNewline();

    for (var networkInterface : networkInterfaces) {
      dataWriter
        .indent()
        // eth7: MTU: 1500; Max-Speed: 1 GB/s
        .appendString(networkInterface.getName())
        .appendString(": ")
        .appendString("MTU: ")
        .appendLong(networkInterface.getMTU())
        .appendString("; Max-Speed: ")
        .appendString(FormatUtil.formatBytesDecimal(networkInterface.getSpeed()))
        .appendString("/s")
        .appendNewline()
        // IPv4 Addresses: 192.168.178.55
        .indent()
        .indent()
        .appendString("IPv4 Addresses: ").appendString(String.join(", ", networkInterface.getIPv4addr()))
        .appendNewline()
        // IPv6 Addresses: fe80:0:0:0:e90b:3b0a:d1f:39bc
        .indent()
        .indent()
        .appendString("IPv6 Addresses: ").appendString(String.join(", ", networkInterface.getIPv6addr()))
        .appendNewline()
        // Received Traffic: 1301964 packets; 1,7 GiB; 0 err, 0 drop
        .indent()
        .indent()
        .appendString("Received Traffic: ")
        .appendLong(networkInterface.getPacketsRecv())
        .appendString(" packets; ")
        .appendString(FormatUtil.formatBytes(networkInterface.getBytesRecv()))
        .appendString("; ")
        .appendLong(networkInterface.getInErrors())
        .appendString(" err, ")
        .appendLong(networkInterface.getInDrops())
        .appendString(" drop")
        .appendNewline()
        // Transmitted Traffic: 689973 packets; 48,0 MiB; 0 err, 0 coll
        .indent()
        .indent()
        .appendString("Transmitted Traffic: ")
        .appendLong(networkInterface.getPacketsSent())
        .appendString(" packets; ")
        .appendString(FormatUtil.formatBytes(networkInterface.getBytesSent()))
        .appendString("; ")
        .appendLong(networkInterface.getOutErrors())
        .appendString(" err, ")
        .appendLong(networkInterface.getCollisions())
        .appendString(" coll")
        .appendNewline()
        .appendNewline();
    }

    // Disks: 3:
    var disks = hardware.getDiskStores();
    dataWriter.appendNewline().appendString("Disks: ").appendInt(disks.size()).appendString(":").appendNewline();

    for (var disk : disks) {
      // \\.\PHYSICALDRIVE2 1,0 TB
      dataWriter
        .indent()
        .appendString(disk.getName())
        .appendString(" ")
        .appendString(FormatUtil.formatBytesDecimal(disk.getSize()))
        .appendNewline();

      // Partitions (1):
      var partitions = disk.getPartitions();
      dataWriter
        .indent()
        .appendString("Partitions (")
        .appendInt(partitions.size())
        .appendString("):")
        .appendNewline();

      for (var partition : partitions) {
        // - Datenträgernr. 2, Partitionsnr. 2 (GPT: Standarddaten); Mounted at C:\; Size: 999,0 GB
        dataWriter
          .indent()
          .indent()
          .appendString("- ")
          .appendString(partition.getIdentification())
          .appendString(" (")
          .appendString(partition.getType())
          .appendString(")")
          .appendString("; Mounted at ")
          .appendString(partition.getMountPoint())
          .appendString("; Size: ")
          .appendString(FormatUtil.formatBytesDecimal(partition.getSize()))
          .appendNewline();
      }

      dataWriter.appendNewline();
    }

    // finish the information
    return dataWriter.endSection();
  }
}
