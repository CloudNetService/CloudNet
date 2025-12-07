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

package eu.cloudnetservice.modules.replacer;

import eu.cloudnetservice.modules.replacer.model.config.Replacer;
import eu.cloudnetservice.modules.replacer.type.ReplaceType;
import eu.cloudnetservice.modules.replacer.type.SearchType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;

final class ReplacerDefaults {

  private static final String DEFAULT_REPLACEMENTS_DIR = "replacements";
  private static final String EXAMPLE_RULES = """
    {
      "rules": [
        {
          "id": "lobby-shared",
          "enabled": true,
          "targets": [
            {
              "task": "Lobby"
            }
          ],
          "files": [
            "config/**/*.yml",
            "plugins/**/config.yml"
          ],
          "placeholders": [
            {
              "token": "%motd%",
              "searchType": "ALL",
              "replaceType": "FIRST",
              "values": [
                "Welcome to %taskName%"
              ]
            },
            {
              "token": "%endpoint%",
              "replaceType": "FIRST",
              "values": [
                "http://%serviceHost%:%servicePort%"
              ]
            }
          ]
        },
        {
          "id": "paper-forwarding-secret",
          "enabled": true,
          "targets": [
            {
              "task": "Paper"
            }
          ],
          "files": [
            "config/paper-global.yml",
            "paper.yml"
          ],
          "placeholders": [
            {
              "token": "%forwardingSecret%",
              "replaceType": "FIRST",
              "values": [
                "REPLACE_ME_WITH_SECRET"
              ]
            }
          ]
        },
        {
          "id": "velocity-forwarding-secret",
          "enabled": true,
          "targets": [
            {
              "task": "Velocity"
            }
          ],
          "files": [
            "forwarding.secret"
          ],
          "placeholders": [
            {
              "token": "%forwardingSecret%",
              "replaceType": "FIRST",
              "values": [
                "REPLACE_ME_WITH_SECRET"
              ]
            }
          ]
        },
        {
          "id": "multi-target-shared",
          "enabled": true,
          "targets": [
            {
              "task": "Lobby"
            },
            {
              "service": "Minigame-1",
              "environment": "MINECRAFT_SERVER"
            },
            {
              "group": "SharedGroup"
            }
          ],
          "files": [
            "config/shared.yml"
          ],
          "placeholders": [
            {
              "token": "%sharedSecret%",
              "replaceType": "FIRST",
              "values": [
                "shared-%nodeId%"
              ]
            }
          ]
        }
      ]
    }
    """;

  private static final String PROXY_RULES = """
    {
      "rules": [
        {
          "id": "velocity-endpoints",
          "targets": [
            { "task": "Velocity" }
          ],
          "files": [
            "velocity.toml",
            "config/velocity.toml"
          ],
          "placeholders": [
            {
              "token": "%servicePort%",
              "searchType": "FIRST",
              "replaceType": "FIRST",
              "values": [
                "25577"
              ]
            },
            {
              "token": "%motd%",
              "replaceType": "FIRST",
              "values": [
                "Velocity %serviceName%"
              ]
            }
          ]
        }
      ]
    }
    """;

  private ReplacerDefaults() {
  }

  public static Replacer defaultConfiguration() {
    return new Replacer(
      true,
      new Replacer.DefaultSection(SearchType.ALL, ReplaceType.FIRST),
      new Replacer.PathSection(List.of(
        "**/*.yml",
        "**/*.yaml",
        "**/*.json",
        "**/*.conf",
        "**/*.txt",
        "**/*.cnl")),
      new Replacer.LimitSection(524_288));
  }

  public static Path replacementsDirectory(Path dataDirectory) {
    return dataDirectory.resolve(DEFAULT_REPLACEMENTS_DIR);
  }

  public static void ensureExampleFiles(Path dir, Logger logger) {
    if (!Files.isDirectory(dir)) {
      return;
    }

    try (var files = Files.list(dir)) {
      if (files.anyMatch(path -> path.toString().endsWith(".json"))) {
        return;
      }
    } catch (Exception exception) {
      logger.debug("Unable to inspect replacements directory {}", dir, exception);
      return;
    }

    writeExample(dir.resolve("example.json"), EXAMPLE_RULES, logger);
    writeExample(dir.resolve("proxy-1.json"), PROXY_RULES, logger);
  }

  private static void writeExample(Path target, String content, Logger logger) {
    try {
      Files.writeString(target, content, StandardCharsets.UTF_8);
    } catch (Exception exception) {
      logger.warn("Unable to write example replacement file {}", target, exception);
    }
  }
}
