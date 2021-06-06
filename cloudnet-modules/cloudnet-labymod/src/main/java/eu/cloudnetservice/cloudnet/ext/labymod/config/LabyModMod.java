package eu.cloudnetservice.cloudnet.ext.labymod.config;

public class LabyModMod {

  private final String hash;
  private final String name;

  public LabyModMod(String hash, String name) {
    this.hash = hash;
    this.name = name;
  }

  public String getHash() {
    return this.hash;
  }

  public String getName() {
    return this.name;
  }
}
