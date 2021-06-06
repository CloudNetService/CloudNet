package de.dytanic.cloudnet.launcher.version.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public class Dependency {

  private final String repository, group, name, version;

  private String classifier;

  public Dependency(String repository, String group, String name, String version) {
    this.repository = repository;
    this.group = group;
    this.name = name;
    this.version = version;
  }

  public Dependency(String repository, String group, String name, String version, String classifier) {
    this.repository = repository;
    this.group = group;
    this.name = name;
    this.version = version;
    this.classifier = classifier;
  }

  public Path toPath() {
    String fileName = String
      .format("%s-%s%s.jar", this.name, this.version, (this.classifier != null ? "-" + this.classifier : ""));

    return Paths.get(this.group.replace(".", "/"), this.name, this.version, fileName);
  }

  public String getRepository() {
    return this.repository;
  }

  public String getGroup() {
    return this.group;
  }

  public String getName() {
    return this.name;
  }

  public String getVersion() {
    return this.version;
  }

  public String getClassifier() {
    return this.classifier;
  }

}
