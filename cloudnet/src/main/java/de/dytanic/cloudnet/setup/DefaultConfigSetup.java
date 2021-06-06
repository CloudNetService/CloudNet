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

package de.dytanic.cloudnet.setup;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.common.unsafe.CPUUsageResolver;
import de.dytanic.cloudnet.console.animation.questionlist.ConsoleQuestionListAnimation;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionListEntry;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeBoolean;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeHostAndPort;
import de.dytanic.cloudnet.console.animation.questionlist.answer.QuestionAnswerTypeInt;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import de.dytanic.cloudnet.driver.network.cluster.NetworkClusterNode;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public class DefaultConfigSetup implements DefaultSetup {

  private final List<String> suggestionIPs = new ArrayList<>();
  private final List<String> whitelistDefaultIPs = new ArrayList<>();

  private String preferredIP;

  private void detectAllIPAddresses() throws SocketException {
    this.whitelistDefaultIPs.add("127.0.1.1");
    this.whitelistDefaultIPs.add("127.0.0.1");

    this.suggestionIPs.add("127.0.0.1");
    this.suggestionIPs.add("127.0.1.1");

    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
    while (interfaces.hasMoreElements()) {
      NetworkInterface networkInterface = interfaces.nextElement();
      Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
      while (addresses.hasMoreElements()) {
        InetAddress inetAddress = addresses.nextElement();

        String address = inetAddress.getHostAddress();
        if (!this.whitelistDefaultIPs.contains(address)) {
          this.whitelistDefaultIPs.add(address);
        }

        String formattedAddress = inetAddress instanceof Inet6Address ? String.format("[%s]", address) : address;
        if (!this.suggestionIPs.contains(formattedAddress)) {
          this.suggestionIPs.add(formattedAddress);
        }
      }
    }
  }

  private String detectPreferredIP() {
    try {
      return InetAddress.getLocalHost().getHostAddress();
    } catch (Exception exception) {
      return this.suggestionIPs.get(0);
    }
  }

  @Override
  public void applyQuestions(ConsoleQuestionListAnimation animation) throws Exception {
    this.detectAllIPAddresses();
    this.preferredIP = this.detectPreferredIP();

    animation.addEntry(new QuestionListEntry<>(
      "eula",
      LanguageManager.getMessage("cloudnet-init-eula"),
      new QuestionAnswerTypeBoolean() {
        @Override
        public boolean isValidInput(@NotNull String input) {
          return input.equalsIgnoreCase(super.getTrueString());
        }

        @Override
        public String getInvalidInputMessage(@NotNull String input) {
          return LanguageManager.getMessage("cloudnet-init-eula-not-accepted");
        }
      }
    ));

    animation.addEntry(new QuestionListEntry<>(
      "internalHost",
      LanguageManager.getMessage("cloudnet-init-setup-internal-host"),
      new QuestionAnswerTypeHostAndPort() {
        @Override
        public String getRecommendation() {
          return DefaultConfigSetup.this.preferredIP + ":1410";
        }

        @Override
        public List<String> getCompletableAnswers() {
          return DefaultConfigSetup.this.suggestionIPs.stream()
            .map(internalIP -> internalIP + ":1410")
            .collect(Collectors.toList());
        }
      }
    ));

    animation.addEntry(new QuestionListEntry<>(
      "webHost",
      LanguageManager.getMessage("cloudnet-init-setup-web-host"),
      new QuestionAnswerTypeHostAndPort() {
        @Override
        public String getRecommendation() {
          return DefaultConfigSetup.this.preferredIP + ":2812";
        }

        @Override
        public List<String> getCompletableAnswers() {
          return DefaultConfigSetup.this.suggestionIPs.stream()
            .map(internalIP -> internalIP + ":2812")
            .collect(Collectors.toList());
        }
      }
    ));

    animation.addEntry(new QuestionListEntry<>(
      "hostAddress",
      LanguageManager.getMessage("cloudnet-init-setup-host-address"),
      new QuestionAnswerTypeHostAndPort(false) {
        @Override
        public String getRecommendation() {
          return DefaultConfigSetup.this.preferredIP;
        }

        @Override
        public List<String> getCompletableAnswers() {
          return DefaultConfigSetup.this.suggestionIPs;
        }
      }
    ));

    animation.addEntry(new QuestionListEntry<>(
      "memory",
      LanguageManager.getMessage("cloudnet-init-setup-memory"),
      new QuestionAnswerTypeInt() {
        @Override
        public boolean isValidInput(@NotNull String input) {
          return super.isValidInput(input) && Integer.parseInt(input) >= 0;
        }

        @Override
        public String getRecommendation() {
          long systemMaxMemory = (CPUUsageResolver.getSystemMemory() / 1048576);
          return String.valueOf((int) (systemMaxMemory - Math.min(systemMaxMemory, 2048)));
        }

        @Override
        public List<String> getCompletableAnswers() {
          long systemMaxMemory = (CPUUsageResolver.getSystemMemory() / 1048576);
          return Arrays.stream(
            new int[]{2048, 4096, 8192, 16384, 32768, 65536, (int) (systemMaxMemory - Math.min(systemMaxMemory, 2048))})
            .filter(value -> value < systemMaxMemory && value > 0)
            .mapToObj(String::valueOf)
            .sorted(Collections.reverseOrder())
            .collect(Collectors.toList());
        }
      }
    ));
  }

  @Override
  public void execute(ConsoleQuestionListAnimation animation) {
    if (animation.hasResult("internalHost")) {
      HostAndPort internalAddress = (HostAndPort) animation.getResult("internalHost");

      CloudNet.getInstance().getConfig().setIdentity(new NetworkClusterNode(
        animation.hasResult("nodeId") ? (String) animation.getResult("nodeId") : "Node-1",
        new HostAndPort[]{
          new HostAndPort(this.whiteListAndFormatHost(internalAddress.getHost()), internalAddress.getPort())
        }
      ));
    }

    if (animation.hasResult("hostAddress")) {
      String hostAddress = ((HostAndPort) animation.getResult("hostAddress")).getHost();
      String formattedHostAddress = this.whiteListAndFormatHost(hostAddress);

      CloudNet.getInstance().getConfig().setHostAddress(formattedHostAddress);
      CloudNet.getInstance().getConfig().setConnectHostAddress(formattedHostAddress);
    }

    if (animation.hasResult("webHost")) {
      HostAndPort webAddress = (HostAndPort) animation.getResult("webHost");

      CloudNet.getInstance().getConfig().setHttpListeners(new ArrayList<>(Collections.singletonList(
        new HostAndPort(this.whiteListAndFormatHost(webAddress.getHost()), webAddress.getPort())
      )));
    }

    CloudNet.getInstance().getConfig().getIpWhitelist().addAll(this.whitelistDefaultIPs);

    if (animation.hasResult("memory")) {
      CloudNet.getInstance().getConfig().setMaxMemory((int) animation.getResult("memory"));
    }
  }

  @Override
  public boolean shouldAsk(boolean configFileAvailable) {
    return !configFileAvailable;
  }

  private String whiteListAndFormatHost(String host) {
    if (!this.whitelistDefaultIPs.contains(host)) {
      CloudNet.getInstance().getConfig().getIpWhitelist().add(host);
    }

    boolean ipv6 = false;
    try {
      ipv6 = InetAddress.getByName(host) instanceof Inet6Address;
    } catch (UnknownHostException exception) {
      exception.printStackTrace();
    }

    return ipv6 ? String.format("[%s]", host) : host;
  }

}
