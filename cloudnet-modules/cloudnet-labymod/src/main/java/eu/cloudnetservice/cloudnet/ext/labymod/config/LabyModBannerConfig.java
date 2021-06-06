package eu.cloudnetservice.cloudnet.ext.labymod.config;

public class LabyModBannerConfig {

  private boolean enabled;

  private String bannerUrl;

  public LabyModBannerConfig(boolean enabled, String bannerUrl) {
    this.enabled = enabled;
    this.bannerUrl = bannerUrl;
  }

  public boolean isEnabled() {
    return this.enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getBannerUrl() {
    return this.bannerUrl;
  }

  public void setBannerUrl(String bannerUrl) {
    this.bannerUrl = bannerUrl;
  }
}
