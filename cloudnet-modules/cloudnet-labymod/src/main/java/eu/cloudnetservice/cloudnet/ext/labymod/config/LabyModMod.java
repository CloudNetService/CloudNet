package eu.cloudnetservice.cloudnet.ext.labymod.config;

public class LabyModMod {
    private String hash;
    private String name;

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
